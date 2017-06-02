(ns lion.worker
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :as async]))

;;;;;;;;;;;;;;;
;; Private
;;;;;;;;;;;;;;;
(defn- listen
  [component input-chan result-chan stop-chan work-fn]
  (async/go-loop []
    (async/alt!
      input-chan
      ([result]
       (async/put! result-chan (work-fn component result))
       (recur))

      stop-chan
      ([_]
       :no-op))))

(defn- trigger-stop
  [stop-chan]
  (async/put! stop-chan :stop))

;;;;;;;;;;;;;;;
;; Component
;;;;;;;;;;;;;;;
(defrecord Worker
  [input-chan output-chan stop-chan work-fn]
  component/Lifecycle

  (start [this]
    (listen this input-chan output-chan stop-chan work-fn)
    this)

  (stop [this]
    (trigger-stop stop-chan)
    (assoc this
           :input-chan nil
           :output-chan nil
           :stop-chan nil
           :work-fn nil)))

;;;;;;;;;;;;;;;
;; Public
;;;;;;;;;;;;;;;
(defn new-instance
  [{:keys [input-chan output-chan work-fn]}]
  (map->Worker
    {:input-chan input-chan
     :output-chan output-chan
     :stop-chan (async/chan 1)
     :work-fn work-fn}))
