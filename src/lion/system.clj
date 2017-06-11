(ns lion.system
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :as async]
            [lion.worker :refer [map->Worker]]
            [lion.queue :refer [map->Queue]]
            ; [lion.facebook :refer [map->Facebook facebook-puller]]
            ; [lion.twitter :refer [map->Twitter twitter-puller twitter-streamer]]
            ))

; Dummy functions
(defn facebook-puller
  [component value]
  )

(defn twitter-puller
  [component value]
  )

(defn twitter-streamer
  [component value]
  (let [restart-chan (:restart-chan component)]
    ; Check if you receive any messages here from restart chan
    ; and stop streamer
    )
  )

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
    (async/chan 1)

    :twitter-streamer-output-chan
    (async/chan 8)

    ; This chan was added to allow us to
    ; restart twitter streamer
    ; ------------------------------
    :twitter-streamer-restart-chan
    (async/chan)

    ; ------------------------------
    :facebook-puller-queue
    (component/using
      (map->Queue (:twitter-puller-queue config))
      {:incoming-chan :facebook-puller-output-chan
       :outgoing-chan :facebook-puller-input-chan})


    ; ------------------------------
    :twitter-puller-queue
    (component/using
      (map->Queue (:twitter-puller-queue config))
      {:incoming-chan :twitter-puller-output-chan
       :outgoing-chan :twitter-streamer-input-chan})

    ; ------------------------------
    :twitter-streamer-queue
    (component/using
      (map->Queue (:twitter-streamer-queue config))
      {:incoming-chan :twitter-streamer-output-chan
       :outgoing-chan :twitter-streamer-input-chan})

    ;------------------------------
    :twitter-streamer-restart-queue
    (component/using
      (map->Queue (:twitter-streamer-restart-queue config))
      {:incoming-chan :twitter-streamer-restart-chan
       :outgoing-chan (async/chan)})

    ; ------------------------------
    :twitter-client
    (assoc :twitter-client true)
    ;(map->Twitter (:twitter-client config))

    ; ------------------------------
    :facebook-client
    (assoc :facebook-client true)
    ;(map->Facebook (:facebook-client config))

    ; Facebook puller
    ; ------------------------------
    :worker-facebook-puller
    (component/using
      (map->Worker {:work-fn facebook-puller})
      {:input-chan :facebook-puller-input-chan
       :output-chan :facebook-puller-output-chan
       :client :facebook-client})

    ; Twitter puller
    ; ------------------------------
    :worker-twitter-puller
    (component/using
      (map->Worker {:work-fn twitter-puller})
      {:input-chan :twitter-puller-input-chan
       :output-chan :twitter-puller-output-chan
       :client :twitter-client})

    ; Twitter streamer
    ; ------------------------------
    :worker-twitter-streamer
    (component/using
      (map->Worker {:work-fn twitter-streamer})
      {:input-chan :twitter-streamer-input-chan
       :output-chan :twitter-streamer-output-chan
       :restart-chan :twitter-streamer-restart-chan
       :client :twitter-client})
    ))
