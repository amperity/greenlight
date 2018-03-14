(ns greenlight.report
  "Code for generating human-readable reports from test results. Each reporter
  takes a collection of test results as input and should produce some output,
  depending on the report type."
  (:require
    [clojure.spec.alpha :as s]
    [clojure.string :as str]
    [greenlight.step :as step]
    [greenlight.test :as test]))


; TODO: add color, generally improve how these reports look.
(defn handle-test-event
  "Print out a test report event."
  [options event]
  (case (:type event)
    :test-start
      (let [test-case (:test event)]
        (printf "\nRunning test %s (%s:%d)\n"
                (::test/title test-case)
                (::test/ns test-case "???")
                (::test/line test-case -1))
        (when-let [desc (::test/description test-case)]
          (println desc)))

    :step-start
      (let [step (:step event)]
        (printf "    > %s %s\n"
                (::step/name step)
                (::step/title step)))

    :step-end
      ; TODO: re-publish test reports?
      (let [result (:step event)]
        (printf "    [%s] %s (%.3f seconds)\n"
                (name (::step/outcome result "???"))
                (::step/message result)
                (::step/elapsed result)))

    :cleanup-resource
      (let [{:keys [resource-type parameters]} event]
        (printf "    Cleaning %s resource %s\n"
                resource-type
                (pr-str parameters)))

    :cleanup-error
      (let [{:keys [error resource-type parameters]} event]
        (printf "    Failed to cleanup %s resource %s: %s\n"
                resource-type
                (pr-str parameters)
                error))

    :test-end
      (let [result (:test event)]
        (printf "[%s] %s (%.3f seconds)\n"
                (str/upper-case (name (::test/outcome result "???")))
                (::test/title result)
                (test/elapsed result))
        (when-let [message (::test/message result)]
          (println message)))

    (println "Unknown report event type:" (pr-str event))))


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
