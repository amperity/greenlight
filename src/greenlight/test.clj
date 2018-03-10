(ns greenlight.test
  "A _test_ is a collection of steps which are run in sequence to exercise a
  specific usage scenario."
  (:require
    [clojure.spec.alpha :as s]
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

; - check that all steps' component dependencies are satisfied?
; - run each step
; - between steps, write out current state to a local file?
; - if any step fails, skip remaining steps
; - run accumulated clean-up steps
;   - how are these accrued? inject function for 'reporting' them?
