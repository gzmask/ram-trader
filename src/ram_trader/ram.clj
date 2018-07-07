;; The calculation is referring to
;; https://eosio.stackexchange.com/questions/847/how-to-get-current-last-ram-price?noredirect=1&lq=1
(ns ram-trader.ram
  (:require [cheshire.core :as json]
            [ram-trader.cleos :refer [cleos] :as cleos]))

(def ^:const RAM-FEE-RATE 0.005)
(def ^:const TRADE-INTERVAL 5000) ;;milliseconds

(defn ->utilization
  "Given query result from (cleos :get :table :eosio :eosio :rammarket), returns the usage percentage."
  [query-result]
  (let [supply (-> query-result
                   :rows first :supply
                   (clojure.string/split #" ") first
                   bigdec long)
        base   (-> query-result
                   :rows first :base :balance
                   (clojure.string/split #" ") first
                   bigdec long)]
    (float (/ supply base))))

(defn ->EOS-price
  "
  Price is:
  Connector Balance/(Smart Token’s Outstanding supply × Connector Weight)
  which is:
  quote.balance / (base.balance * (quote.weight * 5))
  "
  [query-result]
  (let [connector-balance  (-> query-result
                               :rows first :quote :balance
                               (clojure.string/split #" ") first
                               Double.)
        outstanding-supply (-> query-result
                               :rows first :base :balance
                               (clojure.string/split #" ") first
                               bigdec)
        connector-weight   (-> query-result
                               :rows first :quote :weight
                               (clojure.string/split #" ") first
                               Double. (* 2))]
    (Double.
     (* 1024
        (/ connector-balance
           (* outstanding-supply connector-weight))))))

(defn ->ram-fee
  "And also does rounding of ram-fee"
  [amount]
  (let [ram-fee (* amount RAM-FEE-RATE)]
    (if (> ram-fee 0.0001)
      ram-fee
      0.0001)))

(defn buy-ram [from to amount]
  (cleos :system :buyram from to (str amount " EOS")))

(defn buy-ram-limit-order
  "From an account to another acount, buy some EOS amount of RAM if price is lower than given.
   Returns nil when price no gud.
   --Gilfoyle"
  [amount price from to]
  (let [query-result  (json/parse-string
                       (cleos :get :table :eosio :eosio :rammarket)
                       keyword)
        current-price (->EOS-price query-result)]
    (println "Current RAM price: " current-price " EOS")
    (when (< current-price price)
      (buy-ram from to amount))))

(defn buy-ram-limit-order+ram-fee
  "Also consider ram-fee"
  [amount price from to]
  (buy-ram-limit-order amount
    (- price (->ram-fee amount))
    from to))

(defn buy-ram-limit-order+poll
  "polling buy order until successful"
  [amount price from to]
  (future
    (loop [result (buy-ram-limit-order+ram-fee amount price from to)]
      (Thread/sleep TRADE-INTERVAL)
      (println "trying to buy" amount "EOS of RAM at" price)
      (if (nil? result)
        (recur (buy-ram-limit-order+ram-fee amount price from to))
        result))))

(defn sell-ram [account amount]
  (cleos :system :sellram account (str amount)))

(defn sell-ram-limit-order
  "For an account, sell some bytes of RAM if price is pricer than given.
   Returns nil when price no gud.
   --Gilfoyle"
  [amount price account]
  (let [query-result  (json/parse-string
                       (cleos :get :table :eosio :eosio :rammarket)
                       keyword)
        current-price (->EOS-price query-result)]
    (println "Current RAM price:" current-price "EOS")
    (when (> current-price price)
      (sell-ram account amount))))

(defn sell-ram-limit-order+ram-fee
  "Also consider ram-fee"
  [amount price account]
  (sell-ram-limit-order amount
    (+ price (->ram-fee amount))
    account))

(defn sell-ram-limit-order+poll
  "polling sell order until successful"
  [amount price account]
  (future
    (loop [result (sell-ram-limit-order+ram-fee amount price account)]
      (Thread/sleep TRADE-INTERVAL)
      (println "trying to sell" amount "bytes of RAM at" price)
      (if (nil? result)
        (recur (sell-ram-limit-order+ram-fee amount price account))
        result))))

(comment
  (def test-data
    (json/parse-string
     (cleos :get :table :eosio :eosio :rammarket)
     keyword))

  (->EOS-price test-data) ;0.45891871728488004
  (->utilization test-data) ;0.80756074

  (def buy-limit-order
    (buy-ram-limit-order+poll 30 0.458 :kingslanding :kingslanding))
  (future-cancel buy-limit-order)

  (def sell-limit-order
    (sell-ram-limit-order+poll 1024 0.558 :kingslanding))
  (future-cancel sell-limit-order)
  )
