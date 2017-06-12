(ns lion.facebook
  (:require [com.stuartsierra.component :as component]))

;;;;;;;;;;;;;;;
;; Private
;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;
;; Component
;;;;;;;;;;;;;;;
(defrecord Facebook
  [config]
  component/Lifecycle

  (start [this]
    (let [oauth-creds 1]
      (assoc this
             :oauth-creds oauth-creds)))

  (stop [this]
    (assoc this
           :oauth-creds nil)))


;;;;;;;;;;;;;;;
;; Public
;;;;;;;;;;;;;;;

; TODO:
(defn facebook-puller-worker
  [component value callback]
  )
