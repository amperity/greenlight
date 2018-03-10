(ns greenlight.step
  "A _step_ is a reusable chunk of test logic."
  (:require
    [clojure.spec.alpha :as s]
    [clojure.string :as str]
    [clojure.tools.logging :as log]))


;; ## Step Configuration

;; Step type - used to dispatch the actual implementation.
(s/def ::type keyword?)

;; Human friendly name for the step. This must be unique within a test.
(s/def ::name string?)

;; Component dependencies required by the step. This should
;; be a map of local keys to component ids.
(s/def ::components
  (s/map-of keyword? keyword?))

; TODO: timeout as first-class concept?

;; The configuration map ultimately drives the execution of each step. This map
;; is built when tests are initialized and immutable afterwards.
(s/def ::config
  (s/keys :req [::type
                ::name]
          :opts [::components]))



;; ## Step Results

;; Ultimately, a completed step may have one of three outcome states.
;; - `:pass` if the step succeeded and the system behaved as expected.
;; - `:fail` if the system did not behave as expected.
;; - `:error` if the actual step execution failed with an unhandled exception.
(s/def ::outcome #{:pass :fail :error})

;; A message to the user about why the step has its current state. May include
;; remediation steps or areas to look at fixing.
(s/def ::message string?)

;; Duration in seconds that the step ran for.
(s/def ::elapsed float?)

;; Sequence of cleanup actions to take.
(s/def ::cleanup (s/coll-of any? :kind vector?))

; TODO: capture clojure.test assertions
; TODO: capture stdout/stderr/logs?



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
  (log/warn "Don't know how to clean up resource" resource-type (pr-str parameters)))



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


(defmulti execute!
  "Multimethod to execute a test step. This method should return an updated
  context map to pass to the next step."
  (fn dispatch
    [config components ctx]
    (::type config)))


(defmethod execute! :default
  [config components ctx]
  (throw (RuntimeException.
           (format "No method defined for step type %s"
                   (::type config)))))


(defn advance!
  "Advance the test by performing the next step. Returns a tuple of the
  enriched step map and updated context."
  [system step ctx]
  (let [start (System/nanoTime)
        elapsed (delay (/ (- (System/nanoTime) start) 1e9))]
    ; TODO: bind clojure.test reporter
    (binding [*pending-cleanups* (atom [])]
      (try
        (let [components (collect-components system step)
              ctx' (execute! step components ctx)]
          [(assoc step
                  ::outcome :pass ; TODO check clojure.test results
                  ::message "All assertions passed"
                  ::cleanup @*pending-cleanups*
                  ::elapsed @elapsed)
           ctx'])
        (catch Exception ex
          [(assoc step
                  ::outcome :error
                  ::message (format "Unhandled %s: %s"
                                    (.getSimpleName (class ex))
                                    (.getMessage ex))
                  ::cleanup @*pending-cleanups*
                  ::elapsed @elapsed)
           ctx])))))
