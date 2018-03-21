(ns greenlight.report.junit
  "Utilties for generating JUnit output files based on test results."
  (:require
    [clojure.data.xml :as xml]
    [clojure.set :as set])
  (:import
    (java.time
      Duration)))


(defn- step->testcase
  [ns {:greenlight.step/keys [title outcome elapsed message name]}]
  (cond-> [:testcase (merge
                       {:name title
                        :classname ns}
                       (when elapsed
                         {:time (format "%.3f" elapsed)}))]
    (not outcome)
      (conj [:skipped])
    (= :error outcome) ;; TODO: timeout?
      (conj [:error {:message message}])
    (= :failure outcome)
      (conj [:failure {:message message}])))


(defn- test->testsuite
  [{:greenlight.test/keys [title steps outcome started-at ended-at ns]}]
  (into
    [:testsuite
     (let [outcomes (frequencies (map :greenlight.step/outcome steps))
           duration (.toNanos (Duration/between started-at ended-at))]
       {:name title
        :timestamp (str started-at)
        :tests (count steps)
        :time (format "%.3f" (/ duration 1e9))
        :errors (:error outcomes 0) ;; TODO: timeout?
        :failures (:fail outcomes 0)
        :skipped (get outcomes nil 0)})]
    (map (partial step->testcase ns))
    steps))


(defn- results->testsuites
  [results]
  (into
    [:testsuites]
    (map test->testsuite)
    results))


(defn report
  "Render a set of test results to an XML string."
  [results options]
  (xml/emit-str (xml/sexp-as-element (results->testsuites results))))
