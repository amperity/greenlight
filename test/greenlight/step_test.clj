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
                                    #::step {:test (constantly {})}
                                    {})]
    (is (= :pass outcome))))
