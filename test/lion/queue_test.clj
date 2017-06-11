(ns lion.queue-test
  (:require [clojure.test :refer :all]
            [lion.queue :refer :all]
            [clojure.core.async :as async]
            [com.stuartsierra.component :as component]))

(defn rand-str [len]
  (apply str (take len (repeatedly #(char (+ (rand 26) 65))))))

(defn start-system
  [config]
  (component/start (map->Queue config)))

(deftest queue-test
  (testing "testing queue input/output"
    (let [queue-name (rand-str 6)
          config {:input-queue-name queue-name
                  :input-queue-config {:exclusive true :auto-delete true}
                  :output-queue-name queue-name
                  :output-queue-config {:exclusive true :auto-delete true}}
          incoming-chan (async/chan)
          outgoing-chan (async/chan)
          _ (start-system {:incoming-chan incoming-chan
                           :outgoing-chan outgoing-chan
                           :config config})
          result (future (async/<!! incoming-chan))]
      (async/put! incoming-chan 2)
      (is (= @result 2)))))
