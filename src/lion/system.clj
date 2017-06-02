(ns lion.system
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :as async]
            [lion.worker :refer [map->Worker]]))

(defn system
  [config]
  (component/system-map
    :input-chan (async/chan 8)
    :result-chan (async/chan 8)

    :worker-1 (component/using (map->Worker
                                 {:work-fn (fn [x] (println x) x)})
                               {:input-chan :input-chan
                                :output-chan :result-chan})))
