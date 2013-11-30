(ns clj-vircurex.core
  (:gen-class)
  (:require [clojure.data.json :as json])
  (:require [clj-http.client :as client])
  (:use [pandect.core])
  (:use [clj-time.local])
  (:require [clj-time.core :as j])
  (:use [clj-time.format])
  (:use [clj-toml.core])
  (:use [clojure.java.io])
  )

(def built-in-formatter (formatters :date-hour-minute-second))
(def api-url-base "https://vircurex.com/api")

(defn load-config
  "Load user config from $HOME/.clj-vircurex.toml" []
  (def config-path (format "%s/.clj-vircurex.toml" (System/getProperty "user.home")))
  (if-not (.exists (as-file config-path)) 
    ((System/exit 1))) 

  (parse-string (slurp config-path)))

(def ^:dynamic *config* (load-config))
(def ^:dynamic *username* (*config* "username"))
(def ^:dynamic *api-keys* (*config* "keys"))

(defn get-timestamp
  "Returns timestamp for constructing API requests" []
  (unparse built-in-formatter (j/now)))

(defn get-transaction-id
  "Calculates token to use with an API call." [timestamp]
  (def transaction_id_secret (format "%s-%.10f" timestamp (rand)))
  (sha256 transaction_id_secret)
  )

(defn get-token
  "Calculates the token for the requested api method."
  [api-call t txid & args]
  (def api-key (*api-keys* api-call))
 
  (case api-call
    "get_balances" (sha256 (format "%s;%s;%s;%s;get_balances" api-key *username* t txid))
    "get_balance" (sha256 (format "%s;%s;%s;%s;get_balance;%s" api-key *username* t txid (nth args 0)))
    "read_orders" (sha256 (format "%s;%s;%s;%s;read_orders" api-key *username* t txid))
  ))


(defn api-get
  "Make a get request to the server"
  [url]
  (json/read-str ((client/get url) :body) :key-fn keyword)
  )

(defn get-balances
  "Get your balances via Vircurex API." []
  (def t (get-timestamp))
  (def txid (get-transaction-id t))
  (def url (format "%s/get_balances.json?account=%s&id=%s&token=%s&timestamp=%s"
      api-url-base *username* txid (get-token "get_balances" t txid) t
      ))
  (api-get url))

; get-balance:
; YourSecurityWord;YourUserName;Timestamp;ID;get_balance;CurrencyName
(defn get-balance
  "Get your Vircurex balance via API."
  [currency]
  (def t (get-timestamp))
  (def txid (get-transaction-id t))
  ;(def token (sha256 (format "%s;%s;%s;%s;get_balance;%s" x-api-key x-username t txid currency)))
  (def url (format "%s/get_balance.json?account=%s&id=%s&token=%s&timestamp=%s&currency=%s"
      api-url-base *username* txid (get-token "get_balance" t txid currency) t currency
      ))
  (api-get url)

  ;(def balance_json ((client/get url) :body) )
  ;(def balance (json/read-str balance_json :key-fn keyword))
  ;(println (format "%s balance: %d" currency (balance :balance)))
  ;(balance :balance)
  )
;  YourSecurityWord;YourUserName;Timestamp;ID;read_order;orderid
(defn read-orders
  "Returns order information for all users' saved or released orders"
  [otype]
  (def t (get-timestamp))
  (def txid (get-transaction-id t))
  (def url (format "%s/read_orders.json?account=%s&id=%s&token=%s&timestamp=%s&otype=%s"
      api-url-base *username* txid (get-token "read_orders" t txid) t otype
      ))
  (api-get url))

(defn get-market-data
  "Returns market data from Vircurex in PersistentMap format"
  [& args]
  (def market-data-url "https://vircurex.com/api/get_info_for_currency.json")

  (def market_json ((client/get market-data-url) :body) )
  (json/read-str market_json :key-fn keyword))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Retrieving Market Data")
  (def market-data (get-market-data))
  (println "Last LTC trade was:",(((market-data :LTC) :BTC) :last_trade),"BTC")
  (println "Time now is:",(local-now))
  )

