(ns greenlight.test-test
  (:require
    [clojure.test :refer :all]
    [com.stuartsierra.component :as component]
    [greenlight.step :as step :refer [defstep]]
    [greenlight.test :as test]
    [greenlight.test-suite.blue :as blue]))

(deftest sample-test
  (let [system (component/system-map ::component 6)
        test-result (test/run-test! system (blue/sample-test))]
    (is (= ["Sample Step"
            "Sample Step"
            "Another Step"
            "d: 8, e: 10, f: 12"]
           (mapv ::step/title (::test/steps test-result))))
    (is (= :pass (::test/outcome test-result)))
    (is (= 4 (count (::test/steps test-result))))))
