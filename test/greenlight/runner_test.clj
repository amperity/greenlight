(ns greenlight.runner-test
  (:require
    [clojure.test :refer :all]
    [greenlight.test-suite.red :as red]
    [greenlight.test-suite.blue :as blue]
    [greenlight.runner :as runner]))

(deftest filter-test-suite
  (let [tests [(blue/sample-test) (red/sample-test)]
        all-tests (runner/filter-test-suite tests [])
        blue-test-only (runner/filter-test-suite tests [":only" "greenlight.test-suite.blue"])
        red-test-only (runner/filter-test-suite tests [":only" "greenlight.test-suite.red"])]
    (are [e a] (= e a)
      ['greenlight.test-suite.blue 'greenlight.test-suite.red] (mapv :greenlight.test/ns all-tests)
      ['greenlight.test-suite.blue] (mapv :greenlight.test/ns blue-test-only)
      ['greenlight.test-suite.red] (mapv :greenlight.test/ns red-test-only))))