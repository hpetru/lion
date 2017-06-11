(ns lion.queue
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :as async]
            [langohr.core]
            [langohr.channel]
            [langohr.queue]
            [langohr.consumers]
            [langohr.basic]
            [cheshire.core :as cheshire]))

;;;;;;;;;;;;;;;
;; Private
;;;;;;;;;;;;;;;
; TODO:
(defn- connect-to-queue
  [config]
  (langohr.core/connect))

; TODO:
(defn- close-connection
  [conn]
  (langohr.core/close conn))

(defn- create-channel
  [conn]
  (langohr.channel/open conn))

(defn- declare-queue
  [channel queue-name queue-config]
  (langohr.queue/declare
    channel
    queue-name
    queue-config))

(defn- handler-wrapper
  [handler]
  (fn
    [ch {:keys [content-type delivery-tag type] :as meta} ^bytes payload]
    (let [payload-str (String. payload "UTF-8")
          json (cheshire/parse-string payload-str)]
      (handler json))))

; TODO:
(defn- subscribe-to-queue
  [channel config handler]
  (declare-queue
    channel
    (:input-queue-name config)
    (:input-queue-config config))
  (langohr.consumers/subscribe
    channel
    (:input-queue-name config)
    (handler-wrapper handler)))

(defn- publish-to-queue
  [channel config msg]
  (declare-queue
    channel
    (:output-queue-name config)
    (:output-queue-config config))

  (langohr.basic/publish
    channel
    ""
    (:output-queue-name config)
    (cheshire/generate-string msg)))

(defn- listen
  [conn config incoming-chan outgoing-chan stop-chan]
  (subscribe-to-queue
    (create-channel conn)
    config
    (fn message-handler [msg]
      (async/put! incoming-chan msg)))
  (let [channel (create-channel conn)]
    (async/go-loop []
      (async/alt!

        outgoing-chan
        ([msg]
         (publish-to-queue channel config msg))

        stop-chan
        ([_]
         :no-op)))))

(defn- trigger-stop
  [component]
  (async/put! (:stop-chan component) :stop))

;;;;;;;;;;;;;;;
;; Component
;;;;;;;;;;;;;;;
(defrecord Queue
  [config incoming-chan outgoing-chan]
  component/Lifecycle

  (start [this]
    (let [conn (connect-to-queue config)
          stop-chan (async/chan 1)]
      (listen conn
              config
              incoming-chan
              outgoing-chan
              stop-chan)
      (assoc this
             :stop-chan stop-chan
             :conn conn)))

  (stop [this]
    (trigger-stop this)
    (close-connection (:conn this))
    (assoc this
           :conn nil
           :config nil
           :incoming-chan nil
           :outgoing-chan nil
           :stop-chan nil)))
