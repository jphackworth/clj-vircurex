; Copyright (C) 2013 John. P Hackworth
;
; This Source Code Form is subject to the terms of the Mozilla Public
; License, v. 2.0. If a copy of the MPL was not distributed with this
; file, You can obtain one at http://mozilla.org/MPL/2.0/.


(ns clj-vircurex.core

  (:require [clojure.data.json :as json]
    [org.httpkit.client :as http]
    [clj-time.core :as j])   
  (:use [pandect.core]
    [clj-time.local]
    [clj-time.format]
    [clojure.java.io]
    [clj-toml.core]
    [clojure.string :only (upper-case)]
    ))

;)


;(def built-in-formatter (j/formatters :date-hour-minute-second))

; config related functions

(defn config
  "Load user config from $HOME/.clj-vircurex.toml" []
  (def config-path (format "%s/.clj-vircurex.toml" (System/getProperty "user.home")))
  
  (if (not (.exists (as-file config-path)))
    (println "no config exists")
    (parse-string (slurp config-path))))

; In future if we want to persistently store config we can use this to maintain state
; (defn config2 
;   "Returns the config in JSON" [] 
;   (if (not (boolean (resolve '*config*) ))
;     (def ^:dynamic *config* (read-config))
;     *config*))

(defn config3 [] @(config))

(defn username
  "Returns the Vircurex account username" []
  ((config) "username"))

(defn api-keys
  "Returns the API keys defined in $HOME/.clj-vircurex.toml" []
  ((config) "keys"))

(defn upper-keyword [k] (-> k name upper-case keyword) )

; api call helpers

(defn get-timestamp
  "Returns timestamp for constructing API requests" []
  (unparse (formatters :date-hour-minute-second) (j/now)))

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
    "get_balances" (sha256 (format "%s;%s;%s;%s;get_balances" 
      api-key (username) t txid))
    "get_balance" (sha256 (format "%s;%s;%s;%s;get_balance;%s" 
      api-key (username) t txid (nth args 0)))
    "read_orders" (sha256 (format "%s;%s;%s;%s;read_orders" 
      api-key (username) t txid))
    "read_order" (sha256 (format "%s;%s;%s;%s;read_order;%s" 
      api-key (username) t txid (nth args 0)))
    "read_orderexecutions" (sha256 (format "%s;%s;%s;%s;read_orderexecutions:%s" 
      api-key (username) t txid (nth args 0)))
    "create_order" (sha256 (format "%s;%s;%s;%s;create_order;%s;%s;%s;%s;btc" 
      api-key (username) t txid (nth args 0) (nth args 1) (nth args 2) (nth args 3)))
    "delete_order" (sha256 (format "%s;%s;%s;%s;delete_order;%s;%s"
      api-key (username) t txid (nth args 0) (nth args 1)))
    "release_order" (sha256 (format "%s;%s;%s;%s;release_order;%s"
      api-key (username) t txid (nth args 0)))

    ))

(defn query-for
  "Create query for specified API call" [api-call args]
  (case api-call
    "get_balances" ""
    "get_balance" (format "&currency=%s" (nth args 0))
    "read_orders" (format "&otype=%s" (nth args 0))
    "read_order" (format "&orderid=%d&otype=test" (nth args 0))
    "read_orderexecutions" (format "&orderid=%s" (nth args 0))
    "create_order" (format "&ordertype=%s&amount=%s&currency1=%s&unitprice=%s&currency2=btc"
      (nth args 0) (nth args 1) (nth args 2) (nth args 3))
    "delete_order" (format "&orderid=%s&otype=%s" (nth args 0) (nth args 1))
    "release_order" (format "&orderid=%s" (nth args 0)))
  )

(defn url-for
  "Create URL for specified API call" [api-call & args]
  (def t (get-timestamp))
  (def txid (get-transaction-id t))
  (def base ((config) "url"))
  (def base-query (format "?account=%s&id=%s&token=%s&timestamp=%s" 
    (username) txid (token-for api-call t txid args) t))
  (def api-query (query-for api-call args))

  (format "%s/%s.json%s%s" base api-call base-query api-query))

; (defn api-get2
;   "Make a get request to the server"
;   [url]

;   (json/read-str ((client/get url) :body)))

(defn api-get2 
  [url]
    (def options {:method :get 
      :content-type "application/json"
      :user-agent "clj-vircurex 0.0.3"
      :insecure? false
      :keepalive 30000 })

    (http/get url options))

(defn api-get 
  [& {:keys [url json]
    :or {json true}}]
    (def options {:method :get 
      :content-type "application/json"
      :user-agent "clj-vircurex 0.0.3"
      :insecure? false })

    (def response @(http/get url options))
    (if json (json/read-str (response :body) :key-fn keyword) (response :body)))

(defn keyword-to-upper [k] (upper-case (name k)))

; api calls

(defn get-balances []
  (api-get :url (url-for "get_balances")))

(defn get-balance [currency]
  (api-get :url (url-for "get_balance" (keyword-to-upper currency))))

(defn orders-to-seq [orders] 
  (vals (select-keys orders (for [[k v] orders :when (re-find #"^order-.+$" (name k))] k))))

(defn read-orders 
  [otype]
  (orders-to-seq (api-get :url (url-for "read_orders" otype))))

(defn read-order 
  [& {:keys [orderid ordertype]
    :or {ordertype "test"}}]
    (api-get :url (url-for "read_order" orderid ordertype)))

; Bugged?

(defn read-orderexecutions 
  [& {:keys [orderid ordertype]}]
  (api-get :url (url-for "read_orderexecutions" orderid ordertype)))

(defn delete-order 
  [& {:keys [orderid ordertype]}]
    (api-get :url (url-for "delete_order" orderid ordertype)))

(defn release-order
  "Release order for execution." 
   [& {:keys [orderid]}]
  (api-get :url (url-for "release_order" orderid)))

(defn create-order 
  [otype currency & args]
  (def amount (nth args 0))
  (def unitprice (float (nth args 1)))
  (api-get :url (url-for "create_order" (keyword-to-upper otype) amount (name currency) unitprice)))

; simplified calls

(defn upper-test [x] (upper-case x))

(defn buy 
  [currency amount unitprice]
    (create-order :buy currency amount unitprice))
    

(defn sell
  "(sell <:currency> <amount> <unitprice>" [currency amount unitprice]
    (def response (create-order :sell currency amount unitprice)) 
    (case (response "status")
      0 (printf "Order ID: %s\n" (response "orderid"))
      (printf "Error: %s\n" (response "statustxt")))
    response)

(defn balance [currency] (((get-balances) :balances) (upper-keyword currency))) 

(defn released [& currency]
  (if (nil? currency)
    (read-orders 1)
    (filter #(= (:currency1 %) (keyword-to-upper (nth currency 0))) (read-orders 1)) 
    ))

(defn unreleased
  "Read unreleased orders" []
  (read-orders 0))

(defn delete
  "Deletes order based on the orderid in supplied map" [order]
  (delete-order :orderid (order :orderid) :ordertype (order :ordertype)))

(defn release 
  "Releases the order based on the orderid in supplied map" [order]
  (release-order :orderid (order :orderid)))


(defn market-data-fetch []
  (def options {:method :get 
    :content-type "application/json"
    :user-agent "clj-vircurex 0.0.3"
    :insecure? false
    :keepalive 30000 })
  (http/get "https://vircurex.com/api/get_info_for_currency.json" options))

(defn market-data [] (json/read-str (@(market-data-fetch) :body) :key-fn keyword))
  ;(json/read-str market-json))

(defn btce-ticker 
  [& {:keys [exchange]
    :or {exchange :btce}}]
  (case exchange
    :btce (def url "https://btc-e.com/api/2/btc_usd/ticker")
    (def url "https://btc-e.com/api/2/btc_usd/ticker"))    
  (def response (@(http/get url {:keepalive 30000}) :body))
  ((json/read-str response :key-fn keyword) :ticker))
  
(defn price [currency] (read-string ((((market-data) (upper-keyword currency)) :BTC) :last_trade) ))

(defn book-value [currency] nil


  )
(defn market-value [currency]
  (* (* (price currency) (read-string ((get-balance currency) :balance))) ((btce-ticker) :last)))

(defn market-value2 [currency]
  (def mkt (market-data-fetch))
  (def balance-raw  (api-get2 (url-for "get_balance" (keyword-to-upper currency))))
  (def btce-ticker-raw (api-get2 "https://btc-e.com/api/2/btc_usd/ticker"))
  
  (def last-price (read-string ((((json/read-str (@mkt :body) :key-fn keyword) (upper-keyword currency)) :BTC) :last_trade)))
  (def total-balance (read-string ((json/read-str (@balance-raw :body) :key-fn keyword) :balance)))
  (def btc-usd (((json/read-str (@btce-ticker-raw :body) :key-fn keyword) :ticker) :last))
   
  (* (* total-balance last-price) btc-usd))