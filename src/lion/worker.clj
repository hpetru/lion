(ns lion.worker
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :as async]))

;;;;;;;;;;;;;;;
;; Private
;;;;;;;;;;;;;;;
(defn- perform-work
  [component payload work-fn callback-fn]
  (println "Starting working ...")
  (work-fn component payload callback-fn)
  (println "Work done ..."))

(defn- ack-work
  [delivery-tag ack-chan]
  (when ack-chan
    (async/>! ack-chan delivery-tag)))


(defn- listen
  [component work-fn input-chan result-chan ack-chan stop-chan]
  (async/go-loop []
    (async/alt!
      input-chan
      ([[delivery-tag payload]]
       (perform-work component
                     payload
                     work-fn
                     #(async/>!! result-chan %))

       (ack-work delivery-tag
                 ack-chan)
       (recur))

      stop-chan
      ([_]
       :no-op))))

(defn- trigger-stop
  [component]
  (async/put! (:stop-chan component) :stop))

;;;;;;;;;;;;;;;
;; Component
;;;;;;;;;;;;;;;
(defrecord Worker
  [queue work-fn]
  component/Lifecycle

  (start [this]
    (let [stop-chan (async/chan 1)
          input-chan (:incoming-chan queue)
          output-chan (:outgoing-chan queue)
          ack-chan (:ack-chan queue)]
      (listen this
              work-fn
              input-chan
              output-chan
              ack-chan
              stop-chan)
      (assoc this
             :stop-chan stop-chan)))

  (stop [this]
    (trigger-stop this)
    (assoc this
           :stop-chan nil
           :work-fn nil)))
