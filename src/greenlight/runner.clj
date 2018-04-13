(ns greenlight.runner
  "Entry point for running a suite of tests and generating reports from the
  results."
  (:require
    [clojure.spec.alpha :as s]
    [clojure.string :as str]
    [clojure.tools.cli :as cli]
    [com.stuartsierra.component :as component]
    [greenlight.report :as report]
    [greenlight.step :as step]
    [greenlight.test :as test]))


(def cli-options
  [["-o" "--output FILE" "Path to output test results to."]
   [nil  "--no-color" "Disable the use of color in console output."]
   [nil  "--html-report FILE" "Report test results as HTML to the given path."]
   [nil  "--junit-report FILE" "Report test results as Junit XML to the given path."]
   ["-h" "--help"]])



;; ## Runner Commands

(defn print-test-info
  "Print out information about the suite of tests."
  [tests options]
  (printf "Found %d tests:\n" (count tests))
  (doseq [test-case tests]
    (printf "  %s (%d steps)\n"
            (::test/title test-case)
            (count (::test/steps test-case))))
  true)


(defn- report-results
  "Handle result reporting in a general fashion."
  [results options]
  (report/print-console-results
    results
    {:print-color (not (:no-color options))})
  (newline)
  (when-let [junit-path (:junit-report options)]
    (println "Generating JUnit XML report" junit-path)
    (report/write-junit-results junit-path results nil))
  (when-let [html-path (:html-report options)]
    (println "Generating HTML report" html-path)
    (report/write-html-results html-path results nil)))


(defn find-tests
  "Finds tests, optionally limited to namespaces matching a provided regular
  expression."
  ([] (find-tests #".*"))
  ([re]
   (let [keep-test? (fn [test]
                      (re-matches re (name (::test/ns test))))
         test-vars (fn [ns]
                     (->> ns ns-interns vals (filter (comp ::test/test meta))))]
     (->> (all-ns)
          (mapcat test-vars)
          (map (fn [v] (v)))
          (filter keep-test?)))))


(defn run-tests!
  "Run a collection of tests."
  [new-system tests options]
  (when-not (s/valid? ::test/suite tests)
    (throw (IllegalArgumentException.
             (str "Invalid test suite configuration: "
                  (s/explain-str ::test/suite tests)))))
  (println "Starting test system...")
  (let [system (component/start (new-system))]
    (try
      (binding [test/*report* (partial report/handle-test-event
                                       {:print-color (not (:no-color options))})]
        (println "Running" (count tests) "tests...")
        (let [results (mapv (partial test/run-test! system) tests)]
          ; TODO: check result spec?
          (newline)
          (report-results results options)
          (when-let [result-path (:output options)]
            (println "Saving test results to" result-path)
            ; FIXME: this results in unreadable data because it often includes
            ; exceptions in the assertion reports.
            (spit result-path (prn-str results)))
          ; Successful if every test passed.
          (every? (comp #{:pass} ::test/outcome) results)))
      (finally
        (component/stop system)))))


(defn run-all-tests!
  "Runs all tests in all namespaces."
  [new-system options]
  (run-tests! new-system (find-tests) options))


(defn clean-results!
  "Clean up a previous test run."
  [new-system options result-files]
  (prn options)
  (prn result-files)
  ; TODO: load and clean results (#4)
  (throw (RuntimeException. "NYI")))


(defn generate-report
  "Generate a report output from some previously-recorded test files."
  [options result-files]
  (prn options)
  (prn result-files)
  ; TODO: load and report results (#5)
  (throw (RuntimeException. "NYI")))



;; ## Entry Point

(defn ^:dynamic *exit*
  "Dynamic helper which will be called by `main` when it wants to end the
  runner's control flow."
  ([code]
   (flush)
   (shutdown-agents)
   (System/exit code))
  ([code message]
   (binding [*out* *err*]
     (println message)
     (flush))
   (*exit* code)))


(defn main
  "Main entry point for the greenlight test runner. Note that this may exit the
  JVM, so it is not suitable for interactive usage."
  [new-system tests args]
  (let [{:keys [options arguments summary errors]} (cli/parse-opts args cli-options)
        command (first arguments)]
    (cond
      errors
        (*exit* 1 (str/join "\n" errors))

      (or (:help options) (nil? command) (= "help" command))
        (do (println "Usage: [opts] <command> [args...]")
            (newline)
            (println "Commands:")
            (println "  info")
            (println "      Print out test information.")
            (println "  test")
            (println "      Run tests.")
            (println "  clean <result.edn> [...]")
            (println "      Clean up resources from previous test runs.")
            (println "  report <result.edn> [...]")
            (println "      Generate reports from a set of test results.")
            (newline)
            (println summary)
            (*exit* (if (nil? command) 1 0)))

      :else
        (->
          (case command
            "info" (print-test-info tests options)
            "test" (run-tests! new-system tests options)
            "clean" (clean-results! new-system options (rest arguments))
            "report" (generate-report options (rest arguments))
            (*exit* 1 (str "The argument" (pr-str command) "is not a supported command")))
          (as-> result
            (*exit* (if result 0 1)))))))
