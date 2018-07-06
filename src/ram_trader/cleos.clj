;; pre-requirements:
;; cleos installed locally
;; keosd installed locally
;; wallets located at ${HOME}/eosio-wallet}
(ns ram-trader.cleos
  (:require [clojure.java.shell :refer [sh] :as jshell]
            [clojure.pprint :as pp]
            [clojure.core.async :as async]))

(def ^:const keosd-http-server-address "http://127.0.0.1:8900")
(def ^:const nodeos-http-server-address "http://api.eosnewyork.io")
(def keosd (atom nil))

(defn- start-local-wallet!
  "returns a long running future. use (future-cancel future) to stop the wallet"
  []
  (reset! keosd
          (future
            (sh "keosd" "--wallet-dir" "data-dir"
                (str "--http-server-address=" keosd-http-server-address)))))

(defn cleos [& args]
  (let [cmd (concat ["cleos"
                     "--wallet-url" keosd-http-server-address
                     "-u" nodeos-http-server-address]
                    (map name args))
        res (apply sh cmd)]
    (if (= 0 (:exit res))
      (println (:out res))
      (println (:err res)))))

(defn login
  [password]
  (start-local-wallet!)
  (cleos "wallet" "open" "-n" "gzmask")
  (cleos "wallet" "unlock" "-n" "gzmask" "--password" password))

(defn logout-and-quit []
  (cleos "wallet" "lock" "-n" "gzmask")
  (cleos "wallet" "stop")
  future-cancel @keosd)

(comment
  (login "password")

  (cleos :get :info)
  (cleos :wallet :list)
  (cleos :get :account :hezdombqgege)
  (cleos :get :account :kingslanding)
  (cleos :system :listproducers)
  (map (partial cleos :system :voteproducer :approve :kingslanding)
       ["1eostheworld"   "cryptolions1"   "cypherglasss"
        "eos42freedom"   "eosamsterdam"   "eosauthority"
        "eoscafeblock"   "eoscanadacom"   "eoscannonchn"
        "eosdacserver"   "eosdublinwow"   "eoseouldotio"
        "eoshenzhenio"   "eosiomeetone"   "eoslaomaocom"
        "eosliquideos"   "eosnationftw"   "eosnewyorkio"
        "eosnodeonebp"   "eosphereiobp"   "eosriobrazil"
        "eosswedenorg"   "eostribeprod"   "eosvancouver"
        "jedaaaaaaaaa"   "libertyblock"   "sheos21sheos"
        "teamgreymass"   "tokenika4eos"])
  (logout-and-quit))
