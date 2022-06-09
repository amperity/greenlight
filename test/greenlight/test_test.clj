(ns greenlight.test-test
  (:require
    [clojure.java.io :as io]
    [clojure.test :refer :all]
    [com.stuartsierra.component :as component]
    [greenlight.step :as step]
    [greenlight.test :as test]
    [greenlight.test-suite.blue :as blue]
    [greenlight.test-suite.yellow :as yellow]))


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
    (is (= 5 (count (::test/steps test-result)))))

  (let [system      (component/system-map :greenlight.test-test/component 5)
        sample-test (blue/sample-test)
        test-result (test/run-test! system sample-test)]
    (is (= :fail (::test/outcome test-result)))
    (is (= ["Sample Step"
            "Sample Step"
            "greenlight.test-suite.blue/sample-step-without-optionals"
            "Another Step"]
           (butlast
             (mapv ::step/title (::test/steps test-result)))))
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


(deftest optional-attr-map-test
  (let [system (component/system-map :greenlight.test-test/component 6)
        attr-test (blue/optional-attr-map)
        test-result (test/run-test! system attr-test)]
    (is (= (::test/description test-result) "foobar"))
    (is (= (::test/context test-result) {:foo :bar}))
    (is (= :pass (::test/outcome test-result)))
    (is (= ["step-1"]
           (mapv ::step/title (::test/steps test-result))))
    (is (= 1 (count (::test/steps test-result))))))

(deftest fixtures-test
  (let [system (component/system-map :greenlight.test-test/component 6)
        attr-test (blue/with-fixtures)
        test-result (test/run-test! system attr-test)]
    (is (= (::test/description test-result) "foobar"))
    (is (= (::test/context test-result) {:foo :bar}))
    (is (= :pass (::test/outcome test-result)))
    (is (= 4 @blue/counter))
    (is (= ["step-1" "step-2"]
           (mapv ::step/title (::test/steps test-result))))
    (is (= 2 (count (::test/steps test-result))))))

(defmacro with-io
  [input & forms]
  `(binding [*in* (io/reader (java.io.StringReader. ~input))
             *out* (io/writer (java.io.StringWriter.))]
     ~@forms))


(deftest prompt-on-failure
  (let [system (component/system-map :greenlight.test-test/component 6)
        test (yellow/fail-until-3rd-try-test)
        pass-result (with-io "y\ny\n" (test/run-test! system {:on-fail :prompt} test))
        fail-result (with-io "y\nn\n" (test/run-test! system {:on-fail :prompt} test))]
    (is (= :pass (::test/outcome pass-result)))
    (is (= :fail (::test/outcome fail-result)))))


(deftest prompt-on-error
  (let [system (component/system-map :greenlight.test-test/component 6)
        test (yellow/error-until-3rd-try-test)
        pass-result (with-io "y\ny\n" (test/run-test! system {:on-fail :prompt} test))
        error-result (with-io "y\nn\n" (test/run-test! system {:on-fail :prompt} test))]
    (is (= :pass (::test/outcome pass-result)))
    (is (= :error (::test/outcome error-result)))))


(deftest error-report
  (let [system (component/system-map :greenlight.test-test/component 6)
        test (yellow/error-until-3rd-try-test)
        error-result (test/run-test! system {} test)
        step (-> error-result ::test/steps last)
        reports (::step/reports step)
        report (first reports)]
    (is (= 1 (count reports)))
    (is (= :error (:type report)))
    (is (instance? Exception (:actual report)))
    (is (= (::step/message step) (:message report)))))
