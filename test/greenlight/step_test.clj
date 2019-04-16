(ns greenlight.step-test
  (:require
    [clojure.test :refer [is testing deftest]]
    [greenlight.step :as sut]))

(deftest register-cleanup!
  (testing "Default use case"
    (binding [sut/*pending-cleanups* (sut/new-cleanups)]
      (sut/register-cleanup! :hax/dirty {:custom "param"})
      (sut/register-cleanup! :some/other nil)
      (sut/register-cleanup! :hax/dirty {:custom "param"})
      (sut/register-cleanup! :last/one {})
      (is (= [[:hax/dirty {:custom "param"}]
              [:some/other nil]
              [:hax/dirty {:custom "param"}]
              [:last/one {}]]
             @sut/*pending-cleanups*))))
  (testing "Don't add duplicate cleanups"
    (binding [sut/*pending-cleanups* (sut/new-cleanups)]
      (sut/register-cleanup! :hax/dirty {:custom "param"})
      (sut/register-cleanup! :some/other nil)
      (sut/register-cleanup! :hax/dirty {:custom "param"} {:unique? true})
      (sut/register-cleanup! :some/other nil)
      (is (= [[:hax/dirty {:custom "param"}]
              [:some/other nil]
              [:some/other nil]]
             @sut/*pending-cleanups*)))))
