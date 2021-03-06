(ns lion.worker-test
  (:require [clojure.test :refer :all]
            [lion.worker :refer :all]
            [clojure.core.async :as async]
            [com.stuartsierra.component :as component]))

(defn dumb-worker-fn
  [component value callback]
  (callback (+ value 1)))

(defn start-system
  [config]
  (component/start (map->Worker config)))

(deftest worker-test
  (testing "testing worker input/output"
    (let [input-chan (async/chan)
          output-chan (async/chan)
          _ (start-system {:queue {:incoming-chan input-chan
                                   :outgoing-chan output-chan}
                            :work-fn dumb-worker-fn})
          result (future (async/<!! output-chan))]
      (async/>!! input-chan ["abc" 1])
      (is (= @result 2)))))
