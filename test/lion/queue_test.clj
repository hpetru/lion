(ns lion.queue-test
  (:require [clojure.test :refer :all]
            [lion.queue :refer :all]
            [clojure.core.async :as async]
            [com.stuartsierra.component :as component]))

(defn start-system
  [config]
  (component/start (map->Queue config)))

(deftest queue-test
  (testing "testing queue input/output"
    (let [config {:input-queue-name "macaronic"
                  :output-queue-name "macaronic"}
          incoming-chan (async/chan)
          outgoing-chan (async/chan)
          _ (start-system {:incoming-chan incoming-chan
                           :outgoing-chan outgoing-chan
                           :config config})
          result (future (async/<!! incoming-chan))]
      (async/put! incoming-chan 2)
      (is (= @result 2)))))
