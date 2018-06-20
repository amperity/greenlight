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

;; Component dependencies required by the step. This should
;; be a map of local keys to component ids.
(s/def ::components
  (s/map-of keyword? keyword?))

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
          :opts [::components
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


(defn collect-inputs
  [system ctx step]
  (reduce-kv
    (fn [m k v]
      (cond
        (::component v)
          (if-let [c (get system (::component v))]
            (assoc m k c)
            (throw (ex-info
                     (format "Step %s depends on %s component %s which is not available in the test system: %s"
                             (::name step) k (::component v) (str/join " " (keys system)))
                     {:name (::name step)
                      :key k
                      :component v})))
        (::selector v)
          (if-let [c (get ctx (::selector v))]
            (assoc m k c)
            (throw (ex-info
                     (format "Step %s depends on %s context key %s which is not available in the context: %s"
                             (::name step) k (::selector v) (str/join " " (keys ctx)))
                     {:name (::name step)
                      :key k
                      :selector v})))
        :else
          (assoc m k v)))
    {}
    (::inputs step)))


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
              step-future (future (test-fn inputs ctx))
              ctx' (deref step-future (* 1000 timeout) ::timeout)]
          (if (= ctx' ::timeout)
            (do
              (future-cancel step-future)
              [(output-step
                 :timeout
                 (format "Step timed out after %d seconds" timeout))
               ctx])
            (let [report-types (group-by :type @reports)
                  passed? (and (empty? (:fail report-types))
                               (empty? (:error report-types)))]
              (when-not (map? ctx')
                (throw
                  (ex-info "Returned context from step is not a map. Did you forget to return it?" {:ctx ctx'})))
              [(output-step
                 (if passed? :pass :fail)
                 (->> report-types
                      (map #(format "%d %s"
                                    (count (val %))
                                    (name (key %))))
                      (str/join ", ")
                      (format "%d assertions (%s)"
                              (count @reports))))
               ctx'])))
        (catch Exception ex
          [(output-step
             :error
             (format "Unhandled %s: %s"
                     (.getSimpleName (class ex))
                     (.getMessage ex)))
           ctx])))))


(defn lookup
  [k]
  {::selector k})


(defn component
  [k]
  {::component k})
