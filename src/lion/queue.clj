(ns lion.queue
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :as async]
            [langohr.core]
            [langohr.channel]
            [langohr.queue]
            [langohr.consumers]
            [langohr.basic]
            [cheshire.core :as cheshire]))

(def ^:const DEFAULT_QOS 8)

;;;;;;;;;;;;;;;
;; Private
;;;;;;;;;;;;;;;
(defn- connect-to-queue
  [config]
  (langohr.core/connect))

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

(defn- mq-handler-wrapper
  [handler]
  (fn
    [ch {:keys [content-type delivery-tag type] :as meta} ^bytes payload]
    (let [payload-str (String. payload "UTF-8")
          data (cheshire/parse-string payload-str)]
      (handler delivery-tag data))))

(defn- subscribe-to-queue
  [channel config auto-ack? handler]
  ; Subscribe
  (langohr.consumers/subscribe
    channel
    (:input-queue-name config)
    (mq-handler-wrapper handler)
    {:auto-ack auto-ack?}))

(defn- setup-output-queue
  [channel config]
  (declare-queue
    channel
    (:output-queue-name config)
    (:output-queue-config config)))

(defn- setup-input-queue
  [channel config]
  ; Setup queue
  (declare-queue
    channel
    (:input-queue-name config)
    (:input-queue-config config))
  ; Setup QOS
  (langohr.basic/qos
    channel
    (:input-qos-prefetch config DEFAULT_QOS)))


(defn- publish-to-queue
  [channel config msg]
  ; TODO
  (langohr.basic/publish
    channel
    ""
    (:output-queue-name config)
    (cheshire/generate-string msg)))

(defn- subscribe-for-incoming-chan
  [channel config incoming-chan auto-ack?]
  (subscribe-to-queue
    channel
    config
    auto-ack?
    (fn message-handler [delivery-tag msg]
      (println "Received msg ... putting to incoming-chan")
      (async/>!! incoming-chan [delivery-tag msg]))))

(defn- ack-delivery-tags
  [channel ack-chan]
  (async/go-loop []
    (let [delivery-tag (async/<!! ack-chan)]
      (langohr.basic/ack channel delivery-tag)
      (recur))))


(defn- listen
  [conn config incoming-chan outgoing-chan ack-chan stop-chan]

  ; redirect to incoming-chan
  (let [channel (create-channel conn)
        auto-ack? (nil? ack-chan)]
    (setup-input-queue channel config)

    (subscribe-for-incoming-chan
      channel
      config
      incoming-chan
      auto-ack?)

    (when-not auto-ack?
      (ack-delivery-tags channel ack-chan)))

  ; publish messages
  (let [channel (create-channel conn)]
    (setup-output-queue channel config)

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
  (async/>!! (:stop-chan component) :stop))

;;;;;;;;;;;;;;;
;; Component
;;;;;;;;;;;;;;;
(defrecord Queue
  [config incoming-chan outgoing-chan ack-chan]
  component/Lifecycle

  (start [this]
    (let [conn (connect-to-queue config)
          stop-chan (async/chan)]
      (listen conn
              config
              incoming-chan
              outgoing-chan
              ack-chan
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
           :ack-chan nil
           :stop-chan nil)))
