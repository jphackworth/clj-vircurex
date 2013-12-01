(ns clj-vircurex.core
  (:gen-class)
  (:require [clojure.data.json :as json])
  (:require [clj-http.client :as client])
  (:require [clojurewerkz.quartzite.scheduler :as qs]
    [clojurewerkz.quartzite.triggers :as t])
  (:use [pandect.core])
  (:use [clj-time.local])
  (:require [clj-time.core :as j])
  (:use [clj-time.format])
  (:use [clj-toml.core])
  (:use [clojure.java.io])
  (:use [table.core :only [table]])
  )

(def built-in-formatter (formatters :date-hour-minute-second))
(def api-url-base "https://vircurex.com/api")



(defn read-config
  "Load user config from $HOME/.clj-vircurex.toml" []
  (def config-path (format "%s/.clj-vircurex.toml" (System/getProperty "user.home")))
  
  (if (not (.exists (as-file config-path)))
    (println "no config exists")
    (def ^:dynamic *config* (parse-string (slurp config-path))))
  )

; In future if we want to persistently store config we can use this to maintain state
(defn config 
  "Returns the config in JSON" [] 
  (if (not (boolean (resolve '*config*) ))
    (def ^:dynamic *config* (read-config))
    *config*


    ))
  ;(read-config))

(defn username
  "Returns the Vircurex account username" []
  (*config* "username"))

(defn api-keys
  "Returns the API keys defined in $HOME/.clj-vircurex.toml" []
  (*config* "keys"))

(defn urls
  "Returns all URL paths for API calls" []
  (*config* "urls"))

  ; (if-not (.exists (as-file config-path)) 
  ;   ((System/exit 1))) 

  ; (parse-string (slurp config-path)))

;(def ^:dynamic *config* (load-config))
;(def ^:dynamic *username* (*config* "username"))
;(def ^:dynamic *api-keys* (*config* "keys"))


(defn coerce-unformattable-types [args]
  (map (fn [x]
         (cond (instance? clojure.lang.BigInt x) (biginteger x)
               (instance? clojure.lang.Ratio x) (double x)
               :else x))
       args))

(defn format-plus [fmt & args]
  (apply format fmt (coerce-unformattable-types args)))

(defn get-timestamp
  "Returns timestamp for constructing API requests" []
  (unparse built-in-formatter (j/now)))

(defn get-transaction-id
  "Calculates token to use with an API call." [timestamp]
  (def transaction_id_secret (format "%s-%.10f" timestamp (rand)))
  (sha256 transaction_id_secret)
  )

(defn token-for
  "Calculates the token for the requested api method."
  [api-call t txid args]
  (def api-key ((api-keys) api-call))
 
  (case api-call
    "get_balances" (sha256 (format "%s;%s;%s;%s;get_balances" api-key (username) t txid))
    "get_balance" (sha256 (format "%s;%s;%s;%s;get_balance;%s" api-key (username) t txid (nth args 0)))
    "read_orders" (sha256 (format "%s;%s;%s;%s;read_orders" api-key (username) t txid))
    "create_order" (sha256 (format "%s;%s;%s;%s;create_order;%s;%s;%s;%s;btc" api-key (username) t txid (nth args 0) (nth args 1) (nth args 2) (nth args 3)    ))
  ))

(defn api-get
  "Make a get request to the server"
  [url]
  (json/read-str ((client/get url) :body)))

(defn query-for
  "Create query for specified API call" [api-call args]
  (case api-call
    "get_balances" ""
    "get_balance" (format "&currency=%s" (nth args 0))
    "read_orders" (format "&otype=%s" (nth args 0))
    "create_order" (format "&ordertype=%s&amount=%s&currency1=%s&unitprice=%s&currency2=btc"
      (nth args 0) (nth args 1) (nth args 2) (nth args 3)))
  )

(defn url-for
  "Create URL for specified API call" [api-call & args]

  (def t (get-timestamp))
  (def txid (get-transaction-id t))
  (def base ((urls) "base"))
  (def call-path ((urls) api-call))
  (def base-query (format "?account=%s&id=%s&token=%s&timestamp=%s" 
    (username) txid (token-for api-call t txid args) t))
  (def api-query (query-for api-call args))

  (format "%s/%s%s%s" base call-path base-query api-query))

(defn get-balances []
  (api-get (url-for "get_balances")))

(defn get-balance [currency]
  (api-get (url-for "get_balance" currency)))

(defn read-orders [otype & args]
  (def orders (api-get (url-for "read_orders" otype)))
  (case true
    true  (select-keys orders (for [[k v] orders :when (re-find #"^order-.+$" k)] k))
    "default" orders)
  )

(defn create-order [otype currency & args]
  (def amount (nth args 0))
  (def unitprice (float (nth args 1)))

  (printf "Creating %s order for %s of %s at %.8f BTC\n" otype amount currency unitprice)
  (api-get (url-for "create_order" otype amount  currency unitprice))
  )

(defn get-market-data
  "Returns market data from Vircurex in PersistentMap format"
  [& args]
  (def market-data-url "https://vircurex.com/api/get_info_for_currency.json")

  (def market-json ((client/get market-data-url) :body) )
  ;(json/read-str market_json :key-fn keyword))
(json/read-str market-json))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Retrieving Market Data")
  (def market-data (get-market-data))
  (println "Last LTC trade was:",(((market-data :LTC) :BTC) :last_trade),"BTC")
  (println "Time now is:",(local-now))
  )

(defn print-order
  [order]
  (table [["Currency1" "Currency2" "Unit Price" "Order Type" "Quanity" "Open Quantity"]
    [(order "currency1") (order "currency2") (order "unitprice") (order "ordertype") (order "quantity") (order "openquantity")]]
    )

  )

; (defn print-orders 
;   (def order-keys (filter (fn [x] (re-find #"^order-.+$" x)) (keys (read-orders 1))))
;   (table [["#" "Type" "Open Quantity" "Quantity" "Price"]
;     ]))

(read-config)
