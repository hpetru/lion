(ns lion.twitter
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :as async]
            [twitter.oauth]
            [twitter.callbacks]
            [twitter.callbacks.handlers]
            [twitter.api.streaming]))

;;;;;;;;;;;;;;;
;; Private
;;;;;;;;;;;;;;;
(defn- twitter-streaming-start
  [users client]
  (twitter.api.streaming/statuses-filter
    :params {:track "dd"}
    :oauth-creds (:oauth-creds client)
    ; TODO: real callbacks here
    :callbacks
    (twitter.callbacks/get-default-callbacks :async :streaming)))

(defn- twitter-streaming-stop
  [streamer]
  ((:cancel (meta streamer))))


;;;;;;;;;;;;;;;
;; Component
;;;;;;;;;;;;;;;
(defrecord Twitter
  [config]
  component/Lifecycle

  (start [this]
    (let [oauth-creds (twitter.oauth/make-oauth-creds
                        (:app-consumer-key config)
                        (:app-consumer-secret config)
                        (:user-access-token config)
                        (:user-access-token-secret config))]
      (assoc this
             :oauth-creds oauth-creds)))

  (stop [this]
    (assoc this
           :oauth-creds nil)))


;;;;;;;;;;;;;;;
;; Public
;;;;;;;;;;;;;;;

(defn twitter-streamer-worker
  [component value result-chan]
  (let [restart-queue (:restart-queue component)
        restart-chan (:incoming-chan restart-queue)

        client (:client component)
        users value
        streamer (delay (twitter-streaming-start users client))]
    (println "Starting twitter streaming")
    @streamer
    ; Block for a value in restart-chan
    ; and after stop streaming
    (async/<!! restart-chan)
    (println "Stopping twitter streaming ...")
    (twitter-streaming-stop @streamer)))
