(ns lion.worker-test
  (:require [clojure.test :refer :all]
            [lion.queue :refer :all]
            [clojure.core.async :as async]
            [com.stuartsierra.component :as component]))

(defn start-system
  [config]
  (component/start (map->Queue config)))

(deftest worker-test
  (testing "testing queue input/output"
    (let [config {}
          input-chan (async/chan)
          output-chan (async/chan)
          _ (start-system {:incoming-chan incoming-chan
                           :outgoing-chan output-chan
                           :config config})
          result (future (async/<!! outgoing-chan))]
      (async/put! input-chan 1)
      (is (= @result 2)))))
