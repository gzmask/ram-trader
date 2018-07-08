(ns ram-trader.core
  (:require [ram-trader.ram :refer [buy-ram-limit-order+poll
                                    sell-ram-limit-order+poll]]))

(defn -main [& args]
  (println "starting...")
  (sell-ram-limit-order+poll 61440 0.5 :kingslanding)
  (buy-ram-limit-order+poll 30 0.4 :kingslanding :kingslanding))

