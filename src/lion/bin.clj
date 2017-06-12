(ns lion.bin
  (:require [com.stuartsierra.component :as component]
            [lion.system :as sys]
            [clojure.edn :as edn])
  (:gen-class))

(def system nil)

(defn read-config
  [config-path]
  (let [config (-> config-path slurp (edn/read-string))]
    (println "System config:")
    (println config)
    config))

(defn start
  [config-path]
  (let [config (read-config config-path)]
    (println "Starting system")
    (alter-var-root #'system
                    (fn [_] (component/start (sys/system config))))))

(defn -main
  [& args]
  (start "config.edn"))
