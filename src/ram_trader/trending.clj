(ns ram-trader.trending
  (:require [cheshire.core :as json]
            [ram-trader.cleos :as cleos :refer [cleos! cleos]]))

;;TODO using get actions data for ram account to analyse trends

(comment
  (def test-data
    (json/parse-string
     (cleos :get :actions "-j" :eosio.ram "-1" "-22")
     keyword))
  (count (:actions test-data))
  (first (:actions test-data))
  )

