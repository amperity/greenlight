(ns greenlight.runner-test
  (:require
    [clojure.test :refer :all]
    [greenlight.test-suite.red :as red]
    [greenlight.test-suite.blue :as blue]
    [greenlight.runner :as runner]))

(deftest filter-test-suite
  (let [tests [(red/sample-test) (red/sample-test)]]
    (is (= 2 (count (runner/filter-test-suite tests []))))
    (is (= 1 (count (runner/filter-test-suite tests [":only" "greenlight.test-suite.blue"]))))
    (is (= 1 (count (runner/filter-test-suite tests [":only" "greenlight.test-suite.red"]))))))