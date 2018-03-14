(ns greenlight.step
  "A _step_ is a reusable chunk of test logic."
  (:require
    [clojure.spec.alpha :as s]
    [clojure.string :as str]
    [clojure.test :as ctest]))


;; ## Step Configuration

;; Step type keyword. TODO: still useful?
(s/def ::type keyword?)

;; Human friendly name for the step. This must be unique within a test.
(s/def ::name string?)

;; Component dependencies required by the step. This should
;; be a map of local keys to component ids.
(s/def ::components
  (s/map-of keyword? keyword?))

;; The timeout defines the maximum amount of time that the step will be allowed
;; to run, in seconds. Steps which exceed this will fail the test.
(s/def ::timeout pos-int?)

;; Function which will be invoked with the step configuration, selected
;; components, and current test context in order to execute the test logic.
(s/def ::test fn?)

;; The configuration map ultimately drives the execution of each step. This map
;; is built when tests are initialized and immutable afterwards.
(s/def ::config
  (s/keys :req [::type
                ::name
                ::test]
          :opts [::components
                 ::timeout]))


; TODO: defstep macro or step config constructor?



;; ## Step Results

;; Ultimately, a completed step may have one of four outcome states.
;; - `:pass` if the step succeeded and the system behaved as expected.
;; - `:fail` if the system did not behave as expected.
;; - `:error` if the actual step execution failed with an unhandled exception.
;; - `:timeout` if the step ran longer than the allowed duration.
(s/def ::outcome #{:pass :fail :error :timeout})

;; A message to the user about why the step has its current state. May include
;; remediation steps or areas to look at fixing.
(s/def ::message string?)

;; Sequence of cleanup actions to take.
(s/def ::cleanup (s/coll-of any? :kind vector?))

;; Duration in seconds that the step ran for.
(s/def ::elapsed float?)

;; Collection of reported clojure.test assertions.
(s/def ::reports (s/coll-of map? :kind vector?))

; TODO: capture stdout/stderr/logs?

;; Aggregate result fields.
(s/def ::results
  (s/keys :req [::outcome
                ::elapsed]
          :opt [::message
                ::cleanup
                ::reports]))



;; ## Resource Cleanup

(def ^:dynamic *pending-cleanups*
  nil)


(defn register-cleanup!
  "Registers a cleanup job with the `*pending-cleanups*` atom, if bound."
  [resource-type parameters]
  (when-not (thread-bound? #'*pending-cleanups*)
    (throw (IllegalStateException.
             "register-cleanup! called without *pending-cleanups* bound!")))
  (swap! *pending-cleanups* conj [resource-type parameters])
  nil)


(defmulti clean!
  "Multimethod to clean up a created resource after a test finishes. Given the
  entire system to choose dependencies from."
  (fn dispatch
    [system resource-type parameters]
    resource-type))


(defmethod clean! :default
  [system resource-type parameters]
  (throw (RuntimeException.
           (format "Don't know how to clean up resource"
                   resource-type
                   (pr-str parameters)))))



;; ## Execution Facilities

(defn collect-components
  "Gather a map of the component dependencies specified by the test step.
  Returns a map from the component keys to their resolved values, or throws an
  exception if not all components are available."
  [system step]
  (reduce-kv
    (fn [m k v]
      (if-let [c (get system v)]
        (assoc m k c)
        (throw (ex-info
                 (format "Step %s depends on %s component %s which is not available in the test system: %s"
                         (::name step) k v (str/join " " (keys system)))
                 {:step (::name step)
                  :key k
                  :component v}))))
    {}
    (::components step)))


(defn advance!
  "Advance the test by performing the next step. Returns a tuple of the
  enriched step map and updated context."
  [system step ctx]
  (let [start (System/nanoTime)
        elapsed (delay (/ (- (System/nanoTime) start) 1e9))
        reports (atom [])
        output-step #(assoc step
                            ::outcome %1
                            ::message %2
                            ::cleanup @*pending-cleanups*
                            ::elapsed @elapsed
                            ::reports @reports)]
    (binding [ctest/report (partial swap! reports conj)
              *pending-cleanups* (atom [])]
      (try
        (let [test-fn (::test step)
              timeout (::timeout step 60)
              components (collect-components system step)
              step-future (future (test-fn step components ctx))
              ctx' (deref step-future (* 1000 timeout) ::timeout)]
          (if (= ctx' ::timeout)
            (do
              (future-cancel step-future)
              [(output-step
                 :timeout
                 (format "Step timed out after %d seconds" timeout))
               ctx])
            (let [report-types (group-by :type @reports)
                  passed? (and (empty? (:fail report-types))
                               (empty? (:error report-types)))]
              [(output-step
                 (if passed? :pass :fail)
                 (->> report-types
                      (map #(format "%d %s"
                                    (count (val %))
                                    (name (key %))))
                      (str/join ", ")
                      (format "%d assertions (%s)"
                              (count @reports))))
               ctx'])))
        (catch Exception ex
          [(output-step
             :error
             (format "Unhandled %s: %s"
                     (.getSimpleName (class ex))
                     (.getMessage ex)))
           ctx])))))
