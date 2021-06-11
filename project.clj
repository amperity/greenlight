(defproject amperity/greenlight "0.6.2-SNAPSHOT"
  :description "Clojure integration testing framework."
  :url "https://github.com/amperity/greenlight"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}

  :deploy-branches ["master"]
  :aliases
  {"coverage" ["with-profile" "+test,+coverage" "cloverage"]}

  :pedantic? :abort

  :dependencies
  [[org.clojure/clojure "1.10.1"]
   [org.clojure/tools.cli "1.0.194"]
   [org.clojure/data.xml "0.0.8"]
   [amperity/envoy "0.3.3"]
   [com.stuartsierra/component "1.0.0"]]

  :codox
  {:metadata {:doc/format :markdown}
   :source-uri "https://github.com/amperity/greenlight/blob/master/{filepath}#L{line}"
   :output-path "target/doc/codox"}

  :plugins
  [[lein-codox "0.10.7" :exclusions [org.clojure/clojure]]
   [lein-cloverage "1.1.2"]]

  :profiles
  {:repl
   {:source-paths ["dev"]
    :dependencies [[org.clojure/tools.namespace "1.0.0"]]}

   :test
   {:dependencies [[commons-logging "1.2"]]
    :jvm-opts ["-Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.NoOpLog"]}

   :coverage
   {:dependencies [[commons-logging "1.2"]]
    :jvm-opts ["-Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.SimpleLog"
               "-Dorg.apache.commons.logging.simplelog.defaultlog=trace"]}})
