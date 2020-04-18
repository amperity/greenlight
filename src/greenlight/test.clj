(ns greenlight.test
  "A _test_ is a collection of steps which are run in sequence to exercise a
  specific usage scenario."
  (:require
    [clojure.spec.alpha :as s]
    [greenlight.step :as step])
  (:import
    java.time.Instant
    java.time.temporal.ChronoUnit))


;; ## Test Configuration

;; Namespace where the test is defined.
(s/def ::ns symbol?)

;; Source line where the test is defined.
(s/def ::line integer?)

;; Title of the test run.
(s/def ::title string?)

;; Human-friendly description of the scenario the test covers.
(s/def ::description string?)

;; Test execution group tag. Tests within the same group
;; are executed in serial.
(s/def ::group keyword?)

;; Sequence of steps to take for this test.
(s/def ::steps
  (s/coll-of ::step/config
             :kind vector?
             :min-count 1))

;; Initial and final context map for the test.
(s/def ::context map?)

;; The test case map defines metadata about the test and its steps.
(s/def ::case
  (s/keys :req [::title
                ::steps]
          :opt [::ns
                ::line
                ::description
                ::context]))

;; Collection of test cases.
(s/def ::suite
  (s/coll-of ::case
             :kind? vector?
             :min-count 1))


(defn- contains-ns?
  "Returns true if at least one key is in the namespace provided."
  [m ns]
  (some #(= ns (namespace (key %))) m))


(defn- attr-map?
  "Returns true if the map contains at least one key in the
  greenlight.test namespace."
  [o]
  (and (map? o) (contains-ns? o "greenlight.test")))


(defmacro deftest
  "Defines a new integration test. In the first position, the value can
  either be an optional docstring or an optional test configuration
  map. An integration test is a collection of individual steps or an
  arbitrarily nested sequential collection of steps."
  [test-sym & body]
  (let [docstring (when (string? (first body))
                    (first body))
        body (if (string? (first body))
               (rest body)
               body)
        attr-map (when (attr-map? (first body))
                   (first body))
        steps (if (attr-map? (first body))
                (rest body)
                body)
        base (cond-> {}
               docstring (assoc ::description docstring)
               attr-map (merge attr-map))]
    `(defn ~(vary-meta test-sym assoc ::test true)
       []
       (assoc ~base
              ::title ~(str test-sym)
              ::ns '~(symbol (str *ns*))
              ::line ~(:line (meta &form))
              ::steps (vec (flatten (list ~@steps)))))))


;; ## Test Results

;; Final outcome of the test case.
(s/def ::outcome ::step/outcome)

;; When the test run started.
(s/def ::started-at inst?)

;; When the test run ended.
(s/def ::ended-at inst?)


(defn elapsed
  "Calculates the elapsed time a test took. Returns the duration in fractional
  seconds, or 0.0 if started-at or ended-at is missing."
  [result]
  (let [started-at (::started-at result)
        ended-at (::ended-at result)]
    (if (and started-at ended-at)
      (/ (.between ChronoUnit/MILLIS started-at ended-at) 1e3)
      0.0)))


;; ## Test Execution

(defn ^:dynamic *report*
  "Dynamic reporting function which is called at various points in the test
  execution. The event data should be a map containing at least a `:type` key."
  [event]
  ; Default no-op action.
  nil)


; TODO: between steps, write out current state to a local file?
(defn- run-steps!
  "Executes a sequence of test steps by running them in order until one fails.
  Returns a tuple with the enriched vector of steps run and the final context
  map."
  [system ctx steps]
  (loop [history []
         ctx ctx
         steps steps]
    (if-let [step (first steps)]
      ; Run next step to advance the test.
      (let [step (step/initialize step ctx)
            _ (*report* {:type :step-start
                         :step step})
            [step' ctx'] (step/advance! system step ctx)
            history' (conj history step')]
        (*report* {:type :step-end
                   :step step'})
        ; Continue while steps pass.
        (if (= :pass (::step/outcome step'))
          (recur history' ctx' (next steps))
          [(vec (concat history' (rest steps))) ctx']))
      ; No more steps.
      [history ctx])))


(defn- run-cleanup!
  "Clean up after a test run by cleaning up all the reported resources in
  reverse order."
  [system history]
  (doseq [step (reverse history)]
    (when-let [cleanups (seq (::step/cleanup step))]
      (doseq [[resource-type parameters] (reverse cleanups)]
        (try
          (*report* {:type :cleanup-resource
                     :resource-type resource-type
                     :parameters parameters})
          (step/clean! system resource-type parameters)
          (catch Exception ex
            (*report* {:type :cleanup-error
                       :resource-type resource-type
                       :parameters parameters
                       :error ex})))))))


(defn run-test!
  "Execute a test. Returns the updated test map."
  [system test-case]
  (*report* {:type :test-start
             :test test-case})
  (let [started-at (Instant/now)
        ctx (::context test-case {})
        [history ctx] (run-steps! system ctx (::steps test-case))
        _ (run-cleanup! system history)
        ended-at (Instant/now)
        test-case (assoc test-case
                         ::steps history
                         ::context ctx
                         ::outcome (last (keep ::step/outcome history))
                         ::started-at started-at
                         ::ended-at ended-at)]
    (*report* {:type :test-end
               :test test-case})
    test-case))
