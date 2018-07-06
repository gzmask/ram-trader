;; The calculation is referring to
;; https://eosio.stackexchange.com/questions/847/how-to-get-current-last-ram-price?noredirect=1&lq=1
(ns ram-trader.ram
  (:require [cheshire.core :as json]
            [ram-trader.cleos :refer [cleos] :as cleos]))

(def ^:const ram-fee-rate 0.005)

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
  (let [ram-fee (* amount ram-fee-rate)]
    (if (> ram-fee 0.0001)
      ram-fee
      0.0001)))

(defn buy-ram-when-cheaper-than!
  "From an account to another acount, buy some EOS amount of RAM if price is lower than given.
   --Gilfoyle"
  [amount price from to]
  (let [query-result  (json/parse-string
                       (cleos :get :table :eosio :eosio :rammarket)
                       keyword)
        current-price (->EOS-price query-result)]
    (when (< current-price price)
      (cleos :system :buyram from to (str amount " EOS")))))

(defn buy-ram-when-cheaper-than!!
  "Also consider ram-fee"
  [amount price from to]
  (buy-ram-when-cheaper-than! amount
    (- price (->ram-fee amount))
    from to))

(defn sell-ram-when-pricer-than!
  "For an account, sell some bytes of RAM if price is pricer than given.
   --Gilfoyle"
  [amount price account]
  (let [query-result  (json/parse-string
                       (cleos :get :table :eosio :eosio :rammarket)
                       keyword)
        current-price (->EOS-price query-result)]
    (when (> current-price price)
      (cleos :system :sellram account (str amount)))))

(defn sell-ram-when-pricer-than!!
  "Also consider ram-fee"
  [amount price account]
  (sell-ram-when-pricer-than! amount
    (+ price (->ram-fee amount))
    account))

(comment
  (def test-data
    (json/parse-string
     (cleos :get :table :eosio :eosio :rammarket)
     keyword))

  (->EOS-price test-data) ;0.45891871728488004
  (->utilization test-data) ;0.80756074

  (buy-ram-when-cheaper-than!! 0.01 0.458 :kingslanding :kingslanding)
  (sell-ram-when-pricer-than!! 1 0.458 :kingslanding))
