(defproject lion "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.async "0.3.442"]
                 [com.stuartsierra/component "0.3.2"]
                 [com.novemberain/langohr "3.6.1"]
                 [cheshire "5.7.1"]
                 [twitter-api "1.8.0"]

                 [org.clojure/tools.namespace "0.2.11"]]
  :main ^:skip-aot lion.bin
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
