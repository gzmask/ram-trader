(ns ram-trader.core
  (:require [ram-trader.ram :refer [buy-ram-limit-order+poll]]))

(defn -main [& args] 
  (println "starting...")
  (buy-ram-limit-order+poll 30 0.470 :kingslanding :kingslanding))

