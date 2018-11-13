(ns greenlight.test-suite.blue
  (:require
    [clojure.test :refer [is]]
    [com.stuartsierra.component :as component]
    [greenlight.step :as step :refer [defstep]]
    [greenlight.test :as test :refer [deftest]]))


(defstep sample-step
  "A sample greenlight test step in the blue test suite"
  :title "Sample Step"
  :inputs {:foo 1
           :bar 2
           :baz -1}
  ::step/output [::foo ::bar ::baz]
  :test (fn [{:keys [foo bar baz]}]
          (is (= 1 foo))
          (is (= 2 bar))
          (is (= 3 baz))
          4))


(deftest sample-test
  "A sample greenlight test in the blue test suite"
  (sample-step
    {:baz 3})
  (sample-step
    {:baz 3}
    :output [::foo ::bar ::baz2])
  #::step{:name 'another-step
          :title "Another Step"
          :output (fn [ctx outputs]
                    (merge outputs ctx))
          :inputs {:a (step/lookup [::foo ::bar ::baz])
                   :a' (step/lookup [::foo ::bar ::baz2])
                   :b 5
                   :c (step/component :greenlight.test-test/component)
                   :double-a (step/lookup (fn [ctx]
                                            (* 2 (get-in ctx [::foo ::bar ::baz]))))}
          :test (fn [{:keys [a a' b c double-a]}]
                  (is (= 4 a a'))
                  (is (= 5 b))
                  (is (= 6 c))
                  (is (= (* 2 a) double-a))
                  {:d (* 2 a)
                   :e (* 2 b)
                   :f (* 2 c)})}
  [#::step{:name 'third-step
           :title (fn [{:keys [d e f]}]
                    (format "d: %s, e: %s, f: %s" d e f))
           :inputs {:x (step/lookup :d)
                    :y (step/lookup :e)
                    :z (step/lookup :f)}
           :test (fn [{:keys [x y z]}]
                   (is (= 8 x))
                   (is (= 10 y))
                   (is (= 12 z)))}])

