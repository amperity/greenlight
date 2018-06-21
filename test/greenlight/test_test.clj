(ns greenlight.test-test
  (:require
    [clojure.test :refer :all]
    [com.stuartsierra.component :as component]
    [greenlight.step :as step]
    [greenlight.test :as test]))


(test/deftest sample-greenlight-test
  "A sample greenlight test"
  #::step{:name 'sample-greenlight-test
          :title "Sample greenlight test"
          :inputs {:foo 1
                   :bar 2
                   :baz 3}
          :output ::qux
          :test (fn [{:keys [foo bar baz]}]
                  (is (= 1 foo))
                  (is (= 2 bar))
                  (is (= 3 baz))
                  4)}
  #::step{:name 'another-step
          :title "Another Step"
          :inputs {:x (step/lookup ::qux)
                   :y 5
                   :z (step/component ::component)}
          :test (fn [{:keys [x y z]}]
                  (is (= 4 x))
                  (is (= 5 y))
                  (is (= 6 z)))})


(deftest sample-test
  (let [system (component/system-map ::component 6)
        test-result (test/run-test! system (sample-greenlight-test))]
    (is (= :pass (::test/outcome test-result)))))
