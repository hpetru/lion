(ns lion.worker
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :as async]))

;;;;;;;;;;;;;;;
;; Private
;;;;;;;;;;;;;;;
(defn- listen
  [component input-chan result-chan ack-chan stop-chan work-fn]
  (async/go-loop []
    (async/alt!
      input-chan
      ([[delivery-tag result]]

       (println "Received work ... calling handler")
       (work-fn
         component
         result
         (fn callback-fn [value] (async/>! result-chan value)))

       (println "Work done ... sending delivery-tag to ack")
       (async/>! ack-chan delivery-tag)
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
  [input-chan output-chan stop-chan ack-chan work-fn]
  component/Lifecycle

  (start [this]
    (let [stop-chan (async/chan 1)]
      (listen this
              input-chan
              output-chan
              ack-chan
              stop-chan
              work-fn)
      (assoc this
             :stop-chan stop-chan)))

  (stop [this]
    (trigger-stop this)
    (assoc this
           :input-chan nil
           :output-chan nil
           :ack-chan nil
           :stop-chan nil
           :work-fn nil)))
