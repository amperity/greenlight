(ns greenlight.runner-test
  (:require
    [clojure.test :refer :all]
    [greenlight.runner :as runner]
    [greenlight.test :as test]
    [greenlight.test-suite.blue :as blue]
    [greenlight.test-suite.red :as red]))


(defn test-name
  "Retrieve test name only from a Greenlight test"
  [test]
  (symbol (str (name (:greenlight.test/ns test)) "/" (:greenlight.test/title test))))


(deftest filter-test-suite
  (let [tests [(blue/sample-test) (red/sample-test)]
        all-tests (runner/filter-test-suite tests [])
        all-tests-re (runner/filter-test-suite tests [":only" "greenlight.test-suite.*"])
        blue-test-only (runner/filter-test-suite tests [":only" "greenlight.test-suite.blue"])
        blue-test-only-re (runner/filter-test-suite tests [":only" ".*blue"])
        red-test-only (runner/filter-test-suite tests [":only" "greenlight.test-suite.red"])
        one-blue-test-only (runner/filter-test-suite tests [":only" "greenlight.test-suite.blue/sample-test"])
        one-blue-test-only-re (runner/filter-test-suite tests [":only" ".*blue/sample-.*"])]
    (are [e a] (= e a)
      ['greenlight.test-suite.blue
       'greenlight.test-suite.red] (mapv :greenlight.test/ns all-tests)
      ['greenlight.test-suite.blue
       'greenlight.test-suite.red] (mapv :greenlight.test/ns all-tests-re)
      ['greenlight.test-suite.blue] (mapv :greenlight.test/ns blue-test-only)
      ['greenlight.test-suite.blue] (mapv :greenlight.test/ns blue-test-only-re)
      ['greenlight.test-suite.red] (mapv :greenlight.test/ns red-test-only)
      ['greenlight.test-suite.blue/sample-test] (mapv test-name one-blue-test-only)
      ['greenlight.test-suite.blue/sample-test] (mapv test-name one-blue-test-only-re))))


(test/deftest ^:a test-a "a")
(test/deftest ^:b test-b "b")
(test/deftest ^:c test-c
  {::test/group :group/c
   ::test/description "c"})


(deftest finding-tests
  (let [test-vars [#'test-a #'test-b #'test-c]]
    (are [matcher tests] (= tests (#'runner/filter-tests matcher test-vars))
      ;; Return all with no matcher
      nil [(test-a) (test-b) (test-c)]

      ;; Match on metadata
      :a [(test-a)]
      :b [(test-b)]

      ;; Regular expression on test name
      #"test-a" [(test-a)]
      #"test-." [(test-a) (test-b) (test-c)]

      ;; By test properties
      {::test/ns 'greenlight.runner-test}
      [(test-a) (test-b) (test-c)]

      {::test/ns 'greenlight.runner-test, ::test/title "test-a"}
      [(test-a)]

      {::test/group :group/c}
      [(test-c)]

      {::test/group :group/c ::test/ns 'foo.bar}
      [])))
