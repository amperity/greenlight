(ns greenlight.test-test
  (:require
    [clojure.test :refer :all]
    [com.stuartsierra.component :as component]
    [greenlight.step :as step]
    [greenlight.test :as test]
    [greenlight.test-suite.blue :as blue]))

(deftest sample-test
  (let [system (component/system-map :greenlight.test-test/component 6)
        sample-test (blue/sample-test)
        test-result (test/run-test! system sample-test)]
    (is (= :pass (::test/outcome test-result)))
    (is (= ["Sample Step"
            "Sample Step"
            "greenlight.test-suite.blue/sample-step-without-optionals"
            "Another Step"
            "d: 8, e: 10, f: 12"]
           (mapv ::step/title (::test/steps test-result))))
    (is (= 5 (count (::test/steps test-result))))))

(deftest optional-docstring-test
  (let [system (component/system-map :greenlight.test-test/component 6)
        docstring-test (blue/optional-docstring-test)
        test-result (test/run-test! system docstring-test)]
    (is (= :pass (::test/outcome test-result)))
    (is (= ["step-1"
            "step-2"]
           (mapv ::step/title (::test/steps test-result))))
    (is (= 2 (count (::test/steps test-result))))))
