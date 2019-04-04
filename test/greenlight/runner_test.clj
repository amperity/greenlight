(ns greenlight.runner-test
  (:require
    [clojure.test :refer :all]
    [greenlight.runner :as runner]
    [greenlight.test-suite.blue :as blue]
    [greenlight.test-suite.red :as red]))


(deftest filter-test-suite
  (let [tests [(blue/sample-test) (red/sample-test)]
        all-tests (runner/filter-test-suite tests [])
        blue-test-only (runner/filter-test-suite tests [":only" "greenlight.test-suite.blue"])
        red-test-only (runner/filter-test-suite tests [":only" "greenlight.test-suite.red"])
        one-blue-test-only (runner/filter-test-suite tests [":only" "greenlight.test-suite.blue/sample-test"])]
    (are [e a] (= e a)
      ['greenlight.test-suite.blue 'greenlight.test-suite.red] (mapv :greenlight.test/ns all-tests)
      ['greenlight.test-suite.blue] (mapv :greenlight.test/ns blue-test-only)
      ['greenlight.test-suite.red] (mapv :greenlight.test/ns red-test-only)
      ['greenlight.test-suite.blue/sample-test]
      (mapv #(symbol (str (name (:greenlight.test/ns %))
                          "/"
                          (:greenlight.test/title %)))
            one-blue-test-only))))