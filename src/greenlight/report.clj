(ns greenlight.report
  "Code for generating human-readable reports from test results. Each reporter
  takes a collection of test results as input and should produce some output,
  depending on the report type."
  (:require
    [clojure.spec.alpha :as s]
    [clojure.string :as str]))


; TODO: report should be able to aggregate results
; TODO: support multiple reporters:
;   - console (ANSI)
;   - junit XML?
;   - HTML
