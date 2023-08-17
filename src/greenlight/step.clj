(ns greenlight.step
  "A _step_ is a reusable chunk of test logic."
  (:require
    [clojure.spec.alpha :as s]
    [clojure.string :as str]
    [clojure.test :as ctest]
    [greenlight.assert :as assert])
  (:import
    java.util.concurrent.ExecutionException))


;; ## Step Configuration

;; Step name symbol.
(s/def ::name symbol?)


;; Human friendly title string for the step.
;; Can be supplied as a string or a function of the test context
;; that returns a string.
(s/def ::title
  (s/or :str string?
        :fn fn?))


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


(defn qualify-config-keys
  "Takes a map, and prefixes all unnamespaced keywords with greenlight.step."
  [step-config]
  (->> step-config
       (map (fn [[k v]]
              [(if (and (keyword? k) (not (namespace k)))
                 (keyword "greenlight.step" (name k))
                 k)
               v]))
       (into {})))


(defmacro defstep
  "Takes a default step configuration for a reusable step, and defines
  a constructor that allows optional overrides to the inputs and step
  parameters.

  Usage:

  (defstep create-foo
    \"Creates a new foo\"
    :title \"Create a Foo object\"
    :test (fn ...)
    :inputs {:foo/name \"Test Foo\"}
    :output :foo/id)

  (create-foo)
  (create-foo {})
  (create-foo
    {:foo/name \"Custom Foo\"})
  (create-foo
    {:foo/name \"Foo 1\"}
    :output :foo.1/id)"
  [step-name & step-decl]
  (let [docstring (when (string? (first step-decl)) (first step-decl))
        fn-declaration (if docstring
                         `(defn ~step-name ~docstring)
                         `(defn ~step-name))
        default-config (-> (if docstring (rest step-decl) step-decl)
                           (->> (apply hash-map))
                           qualify-config-keys
                           (update ::title #(or % (str *ns* \/ step-name))))]
    `(~@fn-declaration
      ([]
       (~step-name {}))
      ([~'inputs & {:as ~'config}]
       (merge
         {::name '~step-name
          ::inputs (merge
                     ~(::inputs default-config)
                     ~'inputs)}
         ~(dissoc default-config ::inputs)
         (qualify-config-keys ~'config))))))


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


;; TODO: capture stdout/stderr/logs?

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
    [_system resource-type _parameters]
    resource-type))


(defmethod clean! :default
  [_system resource-type parameters]
  (throw (RuntimeException.
           (format "Don't know how to clean up resource type %s with parameters %s"
                   resource-type
                   (pr-str parameters)))))


;; ## Execution Facilities


(defn- resolve-context!
  "Resolves a context value for a given step based on the `context-key`.
  Throws if the key is not present in the ctx map."
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
                  (::name step) k key (str/join " " (keys ctx)))
          {:name (::name step)
           :key k
           :context-key context-key})))))


(defn- resolve-component!
  "Resolves a system component for a given step based on the `component-key`.
  Throws if the key is not present in the system."
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
  "Collect inputs for a step's test function. Resolves all context and component values.
  Throws if a context or component key is not resolvable."
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
  "Store the output of a step back into the context if a step has an output registered.
  Otherwise, returns the context unmodified."
  [step ctx step-result]
  (if-let [output-key (::output step)]
    (let [[t x] (s/conform ::output output-key)]
      (case t
        :kw (assoc ctx output-key step-result)
        :kws (assoc-in ctx output-key step-result)
        :fn (output-key ctx step-result)))
    ctx))


(defn initialize
  "Resolves contextual properties of a step prior to execution."
  [step ctx]
  (cond-> step
    (fn? (::title step)) (update ::title #(% ctx))))


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
      (let [test-fn (::test step)
            timeout (::timeout step 60)
            inputs (collect-inputs system ctx step)
            step-future (future (test-fn inputs))
            result (try
                     (deref step-future (* 1000 timeout) ::timeout)
                     ;; If deref throws an ExecutionException, it means user
                     ;; code in the step threw an exception and greenlight should
                     ;; report the exception in user code, which is the cause.
                     (catch ExecutionException ex
                       (ex-cause ex))
                     ;; Otherwise, exceptions can be reported as-is.
                     (catch Exception ex
                       ex))]
        (cond
          (= result ::timeout)
          (do
            (future-cancel step-future)
            [(output-step
               :timeout
               (format "Step timed out after %d seconds" timeout))
             ctx])

          (instance? Throwable result)
          (let [ex ^Throwable result
                message (str "Unhandled "
                             (.getName (class ex))
                             ": "
                             (ex-message ex)
                             (when-let [data (ex-data ex)]
                               (str " " (pr-str data))))]
            (ctest/do-report {:type :error
                              :message message
                              :expected nil
                              :actual ex})
            [(output-step :error message) ctx])

          :else
          (let [report-types (group-by assert/report->outcome @reports)
                passed? (and (empty? (::assert/fail report-types))
                             (empty? (::assert/error report-types)))]
            [(output-step
               (if passed? :pass :fail)
               (->> report-types
                    (map #(format "%d %s"
                                  (count (val %))
                                  (name (key %))))
                    (str/join ", ")
                    (format "%d assertions (%s)"
                            (count @reports))))
             (save-output step ctx result)]))))))
