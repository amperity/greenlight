(ns greenlight.step-test
  (:require
    [greenlight.step :as step]
    [clojure.test :refer :all]))


(deftest test-invalid-context
  (let [[{::step/keys [outcome message]} _] (step/advance!
                                              {}
                                              #::step{:test (constantly true)}
                                              {})]
    (is (= :error outcome))
    (is (= "Unhandled ExceptionInfo: Returned context from step is not a map. Did you forget to return it?"
           message))))


(deftest test-valid-context
  (let [[{::step/keys [outcome]}] (step/advance!
                                    {}
                                    #::step{:test (constantly {})}
                                    {})]
    (is (= :pass outcome))))


(deftest lookup-inputs
  (let [ctx {:foo/id 1}
        system {:foo/component ::foo}
        step #::step{:name 'test-step
                     :title "Test Step"
                     :inputs {:foo :foo/id
                              :client :foo/component}
                     :test (fn [{:keys [foo bar client]} ctx]
                             (is (= 1 foo))
                             (is (= 2 bar))
                             (is (= (:foo/component system) client))
                             ctx)}
        [step' ctx'] (step/advance! system step ctx)]
    (is (= ctx ctx'))
    (is (= :pass (::step/outcome step'))
        (with-out-str (clojure.pprint/pprint step')))))
