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
    ; Channels for facebook puller
    ; ------------------------------
    :facebook-puller-input-chan
    (async/chan 8)

    :facebook-puller-output-chan
    (async/chan 8)
    ; ------------------------------

    ; Channels for twitter puller
    ; ------------------------------
    :twitter-puller-input-chan
    (async/chan 8)

    :twitter-puller-output-chan
    (async/chan 8)
    ; ------------------------------

    ; Channels for twitter streamer
    ; ------------------------------
    :twitter-streamer-input-chan
    (async/chan)

    :twitter-streamer-output-chan
    (async/chan 8)

    :twitter-streamer-ack-chan
    (async/chan 8)

    ; This chan was added to allow us to
    ; restart twitter streamer
    ; ------------------------------
    :twitter-streamer-restart-chan
    (async/chan)

    ; ------------------------------
    ;:facebook-puller-queue
    ;(component/using
    ;  (map->Queue {:config (:facebook-puller-queue config)})
    ;  {:incoming-chan :facebook-puller-input-chan
    ;   :outgoing-chan :facebook-puller-output-chan})


    ; ------------------------------
    ;:twitter-puller-queue
    ;(component/using
    ;  (map->Queue {:config (:twitter-puller-queue config)})
    ;  {:incoming-chan :twitter-puller-input-chan
    ;   :outgoing-chan :twitter-puller-output-chan})

    ; ------------------------------
    :twitter-streamer-queue
    (component/using
      (map->Queue {:config (:twitter-streamer-queue config)})
      {:incoming-chan :twitter-streamer-input-chan
       :outgoing-chan :twitter-streamer-output-chan
       :ack-chan :twitter-streamer-ack-chan})

    ;------------------------------
    :twitter-streamer-restart-queue
    (component/using
      (map->Queue {:config (:twitter-streamer-restart-queue config)
                   :outgoing-chan (async/chan)
                   :ack-chan (async/chan)})
      {:incoming-chan :twitter-streamer-restart-chan})

    ; ------------------------------
    :twitter-client
    (map->Twitter {:config (:twitter-client config)})

    ; ------------------------------
    :facebook-client
    (map->Facebook {:config (:facebook-client config)})

    ; Facebook puller
    ; ------------------------------
    ;:worker-facebook-puller
    ;(component/using
    ;  (map->Worker {:work-fn facebook-puller-worker})
    ;  {:input-chan :facebook-puller-input-chan
    ;   :output-chan :facebook-puller-output-chan
    ;   :client :facebook-client})

    ;; Twitter puller
    ;; ------------------------------
    ;:worker-twitter-puller
    ;(component/using
    ;  (map->Worker {:work-fn twitter-puller-worker})
    ;  {:input-chan :twitter-puller-input-chan
    ;   :output-chan :twitter-puller-output-chan
    ;   :client :twitter-client})

    ; Twitter streamer
    ; ------------------------------
    :worker-twitter-streamer
    (component/using
      (map->Worker {:work-fn twitter-streamer-worker})
      {:input-chan :twitter-streamer-input-chan
       :output-chan :twitter-streamer-output-chan
       :restart-chan :twitter-streamer-restart-chan
       :ack-chan :twitter-streamer-ack-chan
       :client :twitter-client})
    ))
