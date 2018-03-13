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

;; Collection of detail links to attach to the test.
(s/def ::links
  (s/coll-of (s/tuple string? uri?)))

;; Sequence of steps to take for this test.
(s/def ::steps
  (s/coll-of ::step/config
             :kind vector?
             :min-count 1))

;; Initial and final context map for the test.
(s/def ::context map?)

;; The test map defines metadata about the test and its contained steps.
(s/def ::config  ; TODO: different name?
  (s/keys :req [::title
                ::steps]
          :opt [::ns
                ::line
                ::description
                ::links
                ::context]))


(defmacro deftest
  "Define a new integration test. The test attributes may be a simple string
  description or a map of configuration to merge into the test. Defines a
  function which will construct the test config map."
  [test-sym attrs & steps]
  ; TODO: attach metadata marking this as a test so its discoverable?
  (let [base (if (string? attrs)
               {::description attrs}
               attrs)]
    `(defn ~test-sym
       []
       (assoc ~base
              ::title ~(str test-sym)
              ::ns '~(symbol (str *ns*))
              ::line ~(:line (meta &form))
              ::steps (vector ~@steps)))))



;; ## Test Results

;; Final outcome of the test case.
(s/def ::outcome ::step/outcome)

;; When the test started.
(s/def ::started-at inst?)

;; When the test started.
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
      (let [_ (*report* {:type :step-start
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
        ; TODO: track metrics?
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
