(ns greenlight.assert)

(defmulti report->outcome :type)

(defmethod report->outcome :pass [_] ::pass)
(defmethod report->outcome :fail [_] ::fail)
(defmethod report->outcome :error [_] ::error)
(defmethod report->outcome :default [_] ::pass)
