(ns greenlight.test
  "A _test_ is a collection of steps which are run in sequence to exercise a
  specific usage scenario."
  (:require
    [clojure.spec.alpha :as s]
    [clojure.tools.logging :as log]
    [greenlight.step :as step]))


;; ## Test Configuration

; TODO: group or some notion of collections of related tests?

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

;; The test map defines metadata about the test and its contained steps.
(s/def ::config  ; TODO: different name?
  (s/keys :req [::title
                ::steps]
          :opt [::description
                ::links]))



;; ## Test Execution

; TODO: between steps, write out current state to a local file?

(defn run-steps!
  "Executes a test sequence by running the given steps in order until one fails
  or throws an exception. Returns a tuple with the vector of steps run and the
  final context map."
  [system steps]
  (loop [history []
         ctx {}
         steps steps]
    (if-let [step (first steps)]
      ; Run next step to advance the test.
      (let [[step' ctx'] (step/advance! system step ctx)
            history' (conj history step')]
        ; Continue while steps pass.
        (if (= :pass (::step/outcome step'))
          (recur history' ctx' (next steps))
          [history' ctx']))
      ; No more steps.
      [history ctx])))


(defn run-cleanup!
  "Clean up after a test run by cleaning up all the reported resources in
  reverse order."
  [system history]
  (doseq [step (reverse history)]
    (when-let [cleanups (seq (::step/cleanup step))]
      (doseq [[resource-type parameters] (reverse cleanups)]
        (try
          (step/clean! system resource-type parameters)
          (catch Exception ex
            (log/warn ex "Failed to clean up" resource-type
                      "resource" (pr-str parameters))))))))
