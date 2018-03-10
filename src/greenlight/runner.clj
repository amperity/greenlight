(ns greenlight.runner
  "Entry point for running a suite of tests and generating reports from the
  results."
  (:require
    [clojure.spec.alpha :as s]
    [clojure.string :as str]))


; TODO: handle (multi-proc) parallelization
; Should we support multi-thread parallelization? Probably not, but worth
; thinking about.


; Supported modes:
; - print test info
; - run tests
; - clean up from a previous test run
; - generate a report from collected results


(defn run-tests!
  "Main entry-point for running a collection of tests."
  [new-system tests options]
  ; build system from env
  ; start system
  ; run each test
  ; save results
  ,,,)


; TODO: tools.cli parser version?
