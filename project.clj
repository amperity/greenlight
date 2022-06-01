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
  [[org.clojure/clojure "1.11.1"]
   [org.clojure/tools.cli "1.0.206"]
   [org.clojure/data.xml "0.0.8"]
   [amperity/envoy "1.0.1" :exclusions [org.clojure/tools.logging]]
   [com.stuartsierra/component "1.1.0"]
   [cloverage "1.2.4" :exclusions [org.clojure/data.xml]]]

  :codox
  {:metadata {:doc/format :markdown}
   :source-uri "https://github.com/amperity/greenlight/blob/master/{filepath}#L{line}"
   :output-path "target/doc/codox"}

  :plugins
  [[lein-codox "0.10.8" :exclusions [org.clojure/clojure]]
   [lein-cloverage "1.2.4"]
   [lein-ancient "1.0.0-RC3"]]

  :profiles
  {:repl
   {:source-paths ["dev"]
    :dependencies [[org.clojure/tools.namespace "1.3.0"]]}

   :test
   {:dependencies [[commons-logging "1.2"]]
    :jvm-opts ["-Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.NoOpLog"]}

   :coverage
   {:dependencies [[commons-logging "1.2"]]
    :jvm-opts ["-Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.SimpleLog"
               "-Dorg.apache.commons.logging.simplelog.defaultlog=trace"]}})
