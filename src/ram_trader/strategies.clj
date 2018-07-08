(ns ram-trader.strategies
  (:require [ram-trader.trending :as trend]
            [cheshire.core :as json]
            [ram-trader.cleos :refer [cleos login] :as cleos]
            [ram-trader.ram :as ram]))

(def ^:const TRADE-INTERVAL 5000) ;;milliseconds

(defn buy-limit-trend-up
  [from to amount]
  (when (< (trend/get-delta) 0)
    (ram/buy-ram from to amount)))

(defn buy-ram-limit-order
  "From an account to another acount, buy some EOS amount of RAM if price is lower than given.
   Returns nil when price no gud.
   --Gilfoyle"
  [amount price from to]
  (let [query-result  (json/parse-string
                       (cleos :get :table :eosio :eosio :rammarket)
                       keyword)
        current-price (ram/->EOS-price query-result)]
    (println "Current RAM price: " current-price " EOS")
    (when (< current-price price)
      (buy-limit-trend-up from to amount))))

(defn buy-ram-limit-order+ram-fee
  "Also consider ram-fee"
  [amount price from to]
  (buy-ram-limit-order amount
                       (- price ram/RAM-FEE-RATE)
                       from to))

(defn buy-ram-limit-order-when-trend-up+poll
  "polling buy order until successful"
  [amount price from to]
  (future
    (loop [result (buy-ram-limit-order amount price from to)]
      (Thread/sleep TRADE-INTERVAL)
      (println "trying to buy" amount "EOS of RAM at" price)
      (if (nil? result)
        (recur (buy-ram-limit-order amount price from to))
        result))))

(defn sell-limit-trend-down
  [account amount]
  (when (< 0 (trend/get-delta))
    (ram/sell-ram account amount)))

(defn sell-ram-limit-order
  "For an account, sell some bytes of RAM if price is pricer than given.
   Returns nil when price no gud.
   --Gilfoyle"
  [amount price account]
  (let [query-result  (json/parse-string
                       (cleos :get :table :eosio :eosio :rammarket)
                       keyword)
        current-price (ram/->EOS-price query-result)]
    (println "Current RAM price:" current-price "EOS")
    (when (> current-price price)
      (sell-limit-trend-down account amount))))

(defn sell-ram-limit-order+ram-fee
  "Also consider ram-fee"
  [amount price account]
  (sell-ram-limit-order amount
                        (+ price ram/RAM-FEE-RATE)
                        account))

(defn sell-ram-limit-order-when-trend-down+poll
  "polling sell order until successful"
  [amount price account]
  (future
    (loop [result (sell-ram-limit-order amount price account)]
      (Thread/sleep TRADE-INTERVAL)
      (println "trying to sell" amount "bytes of RAM at" price)
      (if (nil? result)
        (recur (sell-ram-limit-order amount price account))
        result))))

(comment

  (def buy-limit-order
    (buy-ram-limit-order-when-trend-up+poll 30 0.430 :kingslanding :kingslanding))
  (future-cancel buy-limit-order)

  (def sell-limit-order
    (sell-ram-limit-order-when-trend-down+poll 61440 0.5 :kingslanding))
  (future-cancel sell-limit-order))
