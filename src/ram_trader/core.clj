(ns ram-trader.core
  (:require [ram-trader.strategies :refer [buy-ram-limit-order-when-trend-up+poll
                                           sell-ram-limit-order-when-trend-down+poll]])
  (:gen-class))

(defn -main [& args]
  (println "starting...")
  @(sell-ram-limit-order-when-trend-down+poll 847872 0.5 :kingslanding)
  ;@(buy-ram-limit-order-when-trend-up+poll 200 0.385 :kingslanding :kingslanding)
  )

