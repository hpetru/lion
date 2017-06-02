(ns lion.bin
  (:require [com.stuartsierra.component :as component]
            [lion.system :as sys])
  (:gen-class))

(def system nil)

(defn start
  []
  (let [config {}]
    (alter-var-root #'system
                    (fn [_] (component/start (sys/system config))))))

(defn -main
  [& args]
  (start))
