(ns greenlight.report.junit-test
  "Tests for JUnit report generation."
  (:require
    [clojure.test :refer [deftest is testing]]
    [greenlight.report.junit :as junit])
  (:import
    java.time.Instant))


(def ^:private greenlight-edn
  [{:greenlight.test/ns 'foo-test-ns
    :greenlight.test/title "foo-test-title"
    :greenlight.test/outcome :pass
    :greenlight.test/started-at (Instant/parse "2007-12-03T10:15:30.00Z")
    :greenlight.test/ended-at (Instant/parse "2007-12-03T10:15:31.00Z")
    :greenlight.test/steps
    [{:greenlight.step/title "foo-test-step-title"
      :greenlight.step/outcome :pass
      :greenlight.step/elapsed 1.0
      :greenlight.step/message "foo-test-step message"
      :greenlight.step/name 'foo-test-step}]}
   {:greenlight.test/ns 'foo-fail-ns
    :greenlight.test/title "foo-fail-title"
    :greenlight.test/outcome :fail
    :greenlight.test/started-at (Instant/parse "2007-12-03T10:15:30.00Z")
    :greenlight.test/ended-at (Instant/parse "2007-12-03T10:15:31.00Z")
    :greenlight.test/steps
    [{:greenlight.step/title "foo-fail-step-title"
      :greenlight.step/outcome :fail
      :greenlight.step/elapsed 1.0
      :greenlight.step/message "foo-fail-step message"
      :greenlight.step/name 'foo-fail-step}]}
   {:greenlight.test/ns 'foo-error-ns
    :greenlight.test/title "foo-error-title"
    :greenlight.test/outcome :error
    :greenlight.test/started-at (Instant/parse "2007-12-03T10:15:30.00Z")
    :greenlight.test/ended-at (Instant/parse "2007-12-03T10:15:31.00Z")
    :greenlight.test/steps
    [{:greenlight.step/title "foo-error-step-title"
      :greenlight.step/outcome :error
      :greenlight.step/elapsed 1.0
      :greenlight.step/message "foo-error-step message"
      :greenlight.step/name 'foo-error-step}]}])
(def ^:private junit-edn
  [:testsuites
   [:testsuite
    {:name "foo-test-title"
     :timestamp "2007-12-03T10:15:30Z"
     :tests 1
     :time "1.000"
     :errors 0
     :failures 0
     :skipped 0}
    [:testcase
     {:name "foo-test-step-title"
      :classname 'foo-test-ns
      :time "1.000"}]]
   [:testsuite
    {:name "foo-fail-title"
     :timestamp "2007-12-03T10:15:30Z"
     :tests 1
     :time "1.000"
     :errors 0
     :failures 1
     :skipped 0}
    [:testcase
     {:name "foo-fail-step-title"
      :classname 'foo-fail-ns
      :time "1.000"}
     [:failure {:message "foo-fail-step message"}]]]
   [:testsuite
    {:name "foo-error-title"
     :timestamp "2007-12-03T10:15:30Z"
     :tests 1
     :time "1.000"
     :errors 1
     :failures 0
     :skipped 0}
    [:testcase
     {:name "foo-error-step-title"
      :classname 'foo-error-ns
      :time "1.000"}
     [:error {:message "foo-error-step message"}]]]])


(deftest happy-case
  (testing "junit results transformation"
    (is (= junit-edn (#'junit/results->testsuites greenlight-edn)))))
