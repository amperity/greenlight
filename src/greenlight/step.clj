(ns greenlight.step
  "A _step_ is a reusable chunk of test logic."
  (:require
    [clojure.spec.alpha :as s]
    [clojure.string :as str]
    [clojure.test :as ctest]))


;; ## Step Configuration

;; Step name symbol.
(s/def ::name symbol?)

;; Human friendly title string for the step.
(s/def ::title string?)

;; Used for context lookups. Can be a keyword for direct access,
;; a collection of values for `get-in`, or a function of the context.
(s/def ::context-key
  (s/or :kw keyword?
        :kws (s/coll-of any? :min-count 1 :kind sequential?)
        :fn fn?))

;; System component keyword
(s/def ::component
  keyword?)

;; Map of inputs for test function. Value can be
;; a value, component or context key.
(s/def ::inputs
  (s/map-of keyword?
            (s/or :context-key (s/keys :req [::context-key])
                  :component (s/keys :req [::component])
                  :value any?)))

;; Output result to store in step context. Can be a keyword,
;; a collection of values as keys, or a function (ctx, return-value) -> ctx'
(s/def ::output
  (s/or :kw keyword?
        :kws (s/coll-of any? :min-count 1 :kind sequential?)
        :fn fn?))

;; The timeout defines the maximum amount of time that the step will be allowed
;; to run, in seconds. Steps which exceed this will fail the test.
(s/def ::timeout pos-int?)

;; Function which will be invoked with the step configuration, selected
;; components, and current test context in order to execute the test logic.
(s/def ::test fn?)

;; The configuration map ultimately drives the execution of each step. This map
;; is built when tests are initialized and immutable afterwards.
(s/def ::config
  (s/keys :req [::name
                ::title
                ::test]
          :opts [::inputs
                 ::timeout]))


; TODO: defstep macro or step config constructor?



;; ## Step Results

;; Ultimately, a completed step may have one of four outcome states.
;; - `:pass` if the step succeeded and the system behaved as expected.
;; - `:fail` if the system did not behave as expected.
;; - `:error` if the actual step execution failed with an unhandled exception.
;; - `:timeout` if the step ran longer than the allowed duration.
(s/def ::outcome #{:pass :fail :error :timeout})

;; A message to the user about why the step has its current state. May include
;; remediation steps or areas to look at fixing.
(s/def ::message string?)

;; Sequence of cleanup actions to take.
(s/def ::cleanup (s/coll-of any? :kind vector?))

;; Duration in seconds that the step ran for.
(s/def ::elapsed float?)

;; Collection of reported clojure.test assertions.
(s/def ::reports (s/coll-of map? :kind vector?))

; TODO: capture stdout/stderr/logs?

;; Aggregate result fields.
(s/def ::results
  (s/keys :req [::outcome
                ::elapsed]
          :opt [::message
                ::cleanup
                ::reports]))


(defn lookup
  "Look up a context value in the step context. A context key can be either
  a keyword, a collection of values for `get-in`, or a function of the context."
  [context-key]
  {::context-key context-key})


(defn component
  "Look up a system component by key. `component-key` should be a keyword."
  [component-key]
  {::component component-key})


;; ## Resource Cleanup

(def ^:dynamic *pending-cleanups*
  nil)


(defn register-cleanup!
  "Registers a cleanup job with the `*pending-cleanups*` atom, if bound."
  [resource-type parameters]
  (when-not (thread-bound? #'*pending-cleanups*)
    (throw (IllegalStateException.
             "register-cleanup! called without *pending-cleanups* bound!")))
  (swap! *pending-cleanups* conj [resource-type parameters])
  nil)


(defmulti clean!
  "Multimethod to clean up a created resource after a test finishes. Given the
  entire system to choose dependencies from."
  (fn dispatch
    [system resource-type parameters]
    resource-type))


(defmethod clean! :default
  [system resource-type parameters]
  (throw (RuntimeException.
           (format "Don't know how to clean up resource"
                   resource-type
                   (pr-str parameters)))))



;; ## Execution Facilities


(defn- resolve-context!
  [step ctx k context-key]
  (let [[t key] context-key
        result (case t
                 :kw (get ctx key ::missing)
                 :kws (get-in ctx key ::missing)
                 :fn (key ctx))]
    (if (not= ::missing result)
      result
      (throw
        (ex-info
          (format "Step %s depends on %s context key %s which is not available in the context: %s"
                  (::name step) k context-key (str/join " " (keys ctx)))
          {:name (::name step)
           :key k
           :context-key context-key})))))


(defn- resolve-component!
  [step system k component-key]
  (let [result (get system component-key ::missing)]
    (if (not= ::missing result)
      result
      (throw
        (ex-info
          (format "Step %s depends on %s component %s which is not available in the system: %s"
                  (::name step) k component-key (str/join " " (keys system)))
          {:name (::name step)
           :key k
           :component-key component-key})))))


(defn- collect-inputs
  [system ctx step]
  (reduce-kv
    (fn [m k [t v]]
      (assoc
        m k
        (case t
          :value v
          :component (resolve-component! step system k (::component v))
          :context-key (resolve-context! step ctx k (::context-key v)))))
    {}
    (s/conform ::inputs (::inputs step {}))))


(defn- save-output
  [step ctx step-result]
  (if-let [output-key (::output step)]
    (let [[t x] (s/conform ::output output-key)]
      (case t
        :kw (assoc ctx output-key step-result)
        :kws (assoc-in ctx output-key step-result)
        :fn (output-key ctx step-result)))
    ctx))


(defn advance!
  "Advance the test by performing the next step. Returns a tuple of the
  enriched step map and updated context."
  [system step ctx]
  (let [start (System/nanoTime)
        elapsed (delay (/ (- (System/nanoTime) start) 1e9))
        reports (atom [])
        output-step #(assoc step
                            ::outcome %1
                            ::message %2
                            ::cleanup @*pending-cleanups*
                            ::elapsed @elapsed
                            ::reports @reports)]
    (binding [ctest/report (partial swap! reports conj)
              *pending-cleanups* (atom [])]
      (try
        (let [test-fn (::test step)
              timeout (::timeout step 60)
              inputs (collect-inputs system ctx step)
              step-future (future (test-fn inputs))
              output (deref step-future (* 1000 timeout) ::timeout)]
          (if (= output ::timeout)
            (do
              (future-cancel step-future)
              [(output-step
                 :timeout
                 (format "Step timed out after %d seconds" timeout))
               ctx])
            (let [report-types (group-by :type @reports)
                  passed? (and (empty? (:fail report-types))
                               (empty? (:error report-types)))]
              [(output-step
                 (if passed? :pass :fail)
                 (->> report-types
                      (map #(format "%d %s"
                                    (count (val %))
                                    (name (key %))))
                      (str/join ", ")
                      (format "%d assertions (%s)"
                              (count @reports))))
               (save-output step ctx output)])))
        (catch Exception ex
          [(output-step
             :error
             (format "Unhandled %s: %s"
                     (.getSimpleName (class ex))
                     (.getMessage ex)))
           ctx])))))
