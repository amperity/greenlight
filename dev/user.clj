(ns user
  (:require
    [clojure.repl :refer :all]
    [clojure.spec.alpha :as s]
    [clojure.string :as str]
    [clojure.tools.namespace.repl :refer [refresh]]
    [greenlight.report :as report]
    [greenlight.runner :as runner]
    [greenlight.step :as step]
    [greenlight.test :as test]))
