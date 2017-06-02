(ns lion.system
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :as async]
            [lion.worker :refer [map->Worker]]
            [lion.queue :refer [map->Queue]]))

(defn system
  [config]
  (component/system-map
    ; Channels for facebook puller
    :facebook-puller-input-chan (async/chan 8)
    :facebook-puller-output-chan (async/chan 8)

    ; Channels for twitter puller
    :twitter-puller-input-chan (async/chan 8)
    :twitter-puller-output-chan (async/chan 8)

    ; Channels for twitter streamer
    :twitter-streamer-input-chan (async/chan 8)
    :twitter-streamer-output-chan (async/chan 8)

    :facebook-puller-queue (component/using
                            (map->Queue (:twitter-puller-queue config))
                            {:incoming-chan :facebook-puller-output-chan
                             :outgoing-chan :facebook-puller-input-chan})


    :twitter-puller-queue (component/using
                            (map->Queue (:twitter-puller-queue config))
                            {:incoming-chan :twitter-puller-output-chan
                             :outgoing-chan :twitter-streamer-input-chan})

    :twitter-streamer-queue (component/using
                              (map->Queue (:twitter-puller-queue config))
                              {:incoming-chan :twitter-streamer-output-chan
                               :outgoing-chan :twitter-streamer-input-chan})

    ; TODO: Write an real :work-fn
    ; Facebook puller
    :worker-facebook-puller (component/using
                              (map->Worker
                                {:work-fn (fn [_ x] (println x) x)})
                              {:input-chan :facebook-puller-input-chan
                                :output-chan :facebook-puller-output-chan})

    ; TODO: Write an real :work-fn
    ; Twitter puller
    :worker-twitter-puller (component/using
                             (map->Worker
                                {:work-fn (fn [_ x] (println x) x)})
                             {:input-chan :twitter-puller-input-chan
                              :output-chan :twitter-puller-output-chan})

    ; TODO: Write an real :work-fn
    ; Twitter streamer
    :worker-twitter-streamer (component/using
                               (map->Worker
                                 {:work-fn (fn [_ x] (println x) x)})
                               {:input-chan :twitter-streamer-input-chan
                                :output-chan :twitter-streamer-output-chan})))
