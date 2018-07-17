(ns ram-trader.trending
  (:require [cheshire.core :as json]
            [clj-time.core :as time]
            [clj-time.format :as time.f]
            [ram-trader.cleos :as cleos :refer [cleos! cleos]]))

(def ^:const TRADE-NUM 6) ;;smaller, more aggressive

(defn recent-ram-trade
  "get the n number of most recent ram trades"
  [n]
  (let [raw-result (json/parse-string
                    (cleos :get :actions "-j" :eosio.ram "-1" (str "-" n))
                    keyword)]
    (transduce (comp (partition-by (comp :trx_id :action_trace))
                     (map first)
                     (map (fn select-data [t]
                            {:block_time (:block_time t)
                             :quantity   (-> t :action_trace :act :data :quantity)
                             :memo       (-> t :action_trace :act :data :memo)}))
                     (map (fn [t]
                            (let [block-time (time.f/parse (time.f/formatters :date-hour-minute-second-fraction) (:block_time t))]
                              (cond (= "sell ram" (:memo t))
                                    {:quantity   (Double. (first (clojure.string/split (:quantity t) #" ")))
                                     :block_time block-time}
                                    (= "buy ram" (:memo t))
                                    {:quantity   (- 0 (Double. (first (clojure.string/split (:quantity t) #" "))))
                                     :block_time block-time}
                                    :else (throw (Exception. "Unexpected memo, neither sell or buy ram"))
                                    ))))) conj (:actions raw-result))))

(defn delta-per-second
  "Given ram-trades, returns ram pool delta per second for an interval of time in seconds"
  [ram-trades]
  (assoc
   (reduce
    (fn [result trade]
      {:delta (+ (:delta result) (:quantity trade))})
    {:delta 0}
    ram-trades)
   :interval (time/in-seconds (time/interval (:block_time (first ram-trades))
                                             (:block_time (last ram-trades))))))

(defn get-delta []
  (let [delta-info (delta-per-second (recent-ram-trade TRADE-NUM))]
    (println "ram pool:" delta-info)
    (:delta delta-info)))

(comment
  (recent-ram-trade 22)
  (delta-per-second (recent-ram-trade 22))
  (get-delta)
  )

