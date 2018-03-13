(ns greenlight.runner
  "Entry point for running a suite of tests and generating reports from the
  results."
  (:require
    [clojure.spec.alpha :as s]
    [clojure.string :as str]
    [clojure.tools.cli :as cli]
    [com.stuartsierra.component :as component]
    [greenlight.step :as step]
    [greenlight.test :as test]))


(def cli-options
  [["-o" "--output FILE" "Path to output test results to."]
   [nil  "--[no-]color" "Whether to use color in console reports"
    :default true]
   [nil  "--report-html FILE" "Report test results as HTML to the given path."]
   [nil  "--report-junit FILE" "Report test results as Junit XML to the given path."]
   ["-h" "--help"]])


(defn print-test-info
  "Print out information about the suite of tests."
  [tests options]
  (printf "Found %d tests:\n" (count tests))
  (doseq [test-case tests]
    (printf "  %s (%d steps)\n"
            (::test/title test-case)
            (count (::test/steps test-case)))))


(defn- handle-test-report
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
                (::step/type step)
                (::step/name step)))

    :step-end
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
        ; TODO: aggregate assertion reports
        (when-let [message (::test/message result)]
          (println message)))

    (println "Unknown report event type:" (pr-str event))))


; TODO: handle (multi-proc) parallelization
; TODO: Should we support multi-thread parallelization? Probably not, but worth thinking about.
(defn run-tests!
  "Run a collection of tests."
  [new-system tests options]
  (prn options)
  (println "Starting test system...")
  (let [system (component/start (new-system))]
    (try
      (binding [test/*report* (partial handle-test-report options)]
        (println "Running" (count tests) "tests...")
        (let [results (mapv (partial test/run-test! system) tests)]
          ; TODO: report results better
          (newline)
          (clojure.pprint/pprint results)
          (when-let [path (:output options)]
            (spit path (pr-str results)))))
      (finally
        (component/stop system)))))


(defn clean-tests!
  "Clean up a previous test run."
  [new-system options result-files]
  (prn options)
  (prn result-files)
  ; TODO: load and clean results
  (throw (RuntimeException. "NYI")))


(defn generate-report
  "Generate a report output from some previously-recorded test files."
  [options result-files]
  (prn options)
  (prn result-files)
  ; TODO: load and report results
  (throw (RuntimeException. "NYI")))


(defn main
  "Main entry point for the greenlight test runner. Note that this may exit the
  JVM, so it is not suitable for interactive usage."
  [new-system tests args]
  (let [{:keys [options arguments summary errors]} (cli/parse-opts args cli-options)
        command (first arguments)]
    (cond
      errors
        (binding [*out* *err*]
          (doseq [err errors]
            (println errors))
          (System/exit 1))

      (or (:help options) (nil? command) (= "help" command))
        (do (println "Usage: [opts] <command> [args...]")
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
            (flush)
            (System/exit 0))

      :else
        (case command
          "info" (print-test-info tests options)
          "test" (run-tests! new-system tests options)
          "clean" (clean-tests! new-system options (rest arguments))
          "report" (generate-report options (rest arguments))
          (binding [*out* *err*]
            (println "The argument" (pr-str command) "is not a supported command")
            (System/exit 2))))
    (flush)
    (shutdown-agents)
    (System/exit 0)))
