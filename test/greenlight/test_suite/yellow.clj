(ns greenlight.test-suite.yellow
  (:require
    [clojure.test :refer [is]]
    [com.stuartsierra.component :as component]
    [greenlight.step :as step :refer [defstep]]
    [greenlight.test :as test :refer [deftest]]))


(def counter (atom 0))


(defstep reset-counter
  "Reset the counter."
  :title "Reset counter"
  :test (fn [_]
          (reset! counter 0)))


(defstep fail-until-3rd-try
  "A step that fails until the 3rd try."
  :title "Fail Until 3rd Try"
  :test (fn [_]
          (is (= 3 (swap! counter inc)))))


(defstep error-until-3rd-try
  "A step that errors until the 3rd try."
  :title "Error Until 3rd Try"
  :test (fn [_]
          (when (< (swap! counter inc) 3)
            (throw (RuntimeException. "boo")))))


(deftest fail-until-3rd-try-test
  "A test that fails until the 3rd try"
  (reset-counter)
  (fail-until-3rd-try))


(deftest error-until-3rd-try-test
  "A test that errors until the 3rd try"
  (reset-counter)
  (error-until-3rd-try))
