(ns lion.system
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :as async]
            [lion.worker :as worker]))

(defn system
  [config]
  (component/system-map
    :input-chan (async/chan 8)
    :result-chan (async/chan 8)

    :worker-1 (component/using (worker/new-instance
                                 {:work-fn (fn [x] (println x) x)})
                               {:input-chan :input-chan
                                :output-chan :result-chan})))
