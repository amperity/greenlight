(ns greenlight.report
  "Code for generating human-readable reports from test results. Each reporter
  takes a collection of test results as input and should produce some output,
  depending on the report type."
  (:require
    [clojure.spec.alpha :as s]
    [clojure.string :as str]))


(defn print-console-results
  "Render a set of test results out to the console."
  [results options]
  ; TODO: report results better
  (clojure.pprint/pprint results))


(defn write-junit-results
  "Render a set of test results to a JUnit XML file."
  [report-path results options]
  ; TODO: implement junit reporting (#6)
  (println "WARN: JUnit XML reporting is not available yet"))


(defn write-html-results
  "Render a set of test results to a human-friendly HTML file."
  [report-path results options]
  ; TODO: implement html reporting (#7)
  (println "WARN: HTML reporting is not available yet"))
