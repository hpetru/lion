(ns lion.system
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :as async]
            [lion.worker :refer [map->Worker]]
            [lion.queue :refer [map->Queue]]
            [lion.facebook :refer [map->Facebook
                                   facebook-puller-worker]]
            [lion.twitter :refer [map->Twitter
                                  twitter-streamer-worker
                                  twitter-puller-worker]]
            [twitter.oauth]
            [twitter.callbacks]
            [twitter.callbacks.handlers]
            [twitter.api.streaming]
            )
  (:import [twitter.callbacks.protocols AsyncStreamingCallback]))

(defn system
  [config]
  (component/system-map
    ; ------------------------------
    :twitter-client
    (map->Twitter {:config (:twitter-client config)})

    ; ------------------------------
    :twitter-streamer-queue
    (map->Queue {:config (:twitter-streamer-queue config)
                 :incoming-chan (async/chan)
                 :outgoing-chan (async/chan 8)
                 :ack-chan (async/chan)})

    ;------------------------------
    :twitter-streamer-restart-queue
    (map->Queue {:config (:twitter-streamer-restart-queue config)
                 :incoming-chan (async/chan)
                 :outgoing-chan (async/chan)})

    ; Twitter streamer
    ; ------------------------------
    :worker-twitter-streamer
    (component/using
      (map->Worker {:work-fn twitter-streamer-worker})
      {:queue :twitter-streamer-queue
       :restart-queue :twitter-streamer-restart-queue
       :client :twitter-client})
    ))
