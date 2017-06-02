(ns lion.queue
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :as async]))

;;;;;;;;;;;;;;;
;; Private
;;;;;;;;;;;;;;;
; TODO:
(defn- connect-to-queue
  [config]
  )

; TODO:
(defn- close-connection
  [conn]
  )

; TODO:
(defn- subscribe-to-queue
  [conn config handler]
  )

(defn- publish-to-queue
  [conn conn config msg]
  )

(defn- listen
  [conn config incoming-chan outgoing-chan stop-chan]
  (subscribe-to-queue conn config
    (fn message-handler [msg]
      (async/put! incoming-chan msg)))
  (async/go-loop []
    (async/alt!

      outgoing-chan
      ([msg]
       (publish-to-queue conn config msg))

      stop-chan
      ([_]
       :no-op))))

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
