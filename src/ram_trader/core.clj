(ns ram-trader.core
  (:require [ram-trader.strategies :refer [buy-ram-limit-order-when-trend-up+poll
                                           sell-ram-limit-order-when-trend-down+poll]]))

(defn -main [& args]
  (println "starting...")
  (sell-ram-limit-order-when-trend-down+poll 61440 0.5 :kingslanding)
  (buy-ram-limit-order-when-trend-up+poll 30 0.4 :kingslanding :kingslanding))

