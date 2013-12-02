; Copyright (C) 2013 John. P Hackworth
;
; This Source Code Form is subject to the terms of the Mozilla Public
; License, v. 2.0. If a copy of the MPL was not distributed with this
; file, You can obtain one at http://mozilla.org/MPL/2.0/.


(ns clj-vircurex.core
 
  (:require [clojure.data.json :as json])
  (:require [clj-http.client :as client])
  (:use [pandect.core])
  (:use [clj-time.local])
  (:require [clj-time.core :as j])
  (:use [clj-time.format])
  (:use [clj-toml.core])
  (:use [clojure.java.io]))

(def built-in-formatter (formatters :date-hour-minute-second))

; config related functions

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
    *config*))

(defn username
  "Returns the Vircurex account username" []
  (*config* "username"))

(defn api-keys
  "Returns the API keys defined in $HOME/.clj-vircurex.toml" []
  (*config* "keys"))

; api call helpers

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

(defn api-get
  "Make a get request to the server"
  [url]
  (json/read-str ((client/get url) :body)))

; api calls

(defn get-balances []
  (api-get (url-for "get_balances")))

(defn get-balance [currency]
  (api-get (url-for "get_balance" (name :currency))))

(defn read-orders 
  "(read-orders <0|1>)" [otype & args]
  (def orders (api-get (url-for "read_orders" otype)))
  (case true
    true  (vals (select-keys orders (for [[k v] orders :when (re-find #"^order-.+$" k)] k)))
    "default" orders))

(defn read-order 
  "Read order based on supplied order id and type" [orderid]
  (api-get (url-for "read_order" orderid "test")))

(defn delete-order 
  "(delete-order <orderid>" [orderid]
    (api-get (url-for "delete_order" orderid "test")))

(defn release-order
  "Release order for execution." [orderid]
  (api-get (url-for "release_order" orderid))


  )

(defn create-order 
  "(create-order <:buy|:sell> <:currency> <amount> <unitprice>)" [otype currency & args]
  (def amount (nth args 0))
  (def unitprice (float (nth args 1)))

  (printf "Creating %s order: %.8f %s at %.8f BTC\n" (clojure.string/lower-case (name otype)) amount (clojure.string/upper-case (name currency)) unitprice)
  (api-get (url-for "create_order" (clojure.string/upper-case (name otype)) amount (name currency) unitprice)))

; simplified calls

(defn buy 
  "(buy <:currency> <amount> <unitprice>" [currency amount unitprice]
    (def response (create-order :buy currency amount unitprice))
    (keys response) 
    (case (response "status")
      0 (printf "Order ID: %s\n" (response "orderid"))
      (printf "Error: %s\n", (response "statustxt")))
    response)

(defn sell
  "(sell <:currency> <amount> <unitprice>" [currency amount unitprice]
    (def response (create-order :sell currency amount unitprice)) 
    (case (response "status")
      0 (printf "Order ID: %s\n" (response "orderid"))
      (printf "Error: %s\n" (response "statustxt")))
    response)

(defn released
  "Read released orders" []
  (read-orders 1))

(defn unreleased
  "Read unreleased orders" []
  (read-orders 0))

(defn delete
  "Deletes order based on the orderid in supplied map" [order]
  (delete-order (order "orderid")))

(defn release 
  "Releases the order based on the orderid in supplied map" [order]
  (release-order (order "orderid")))

(defn get-market-data
  "Returns market data from Vircurex in PersistentMap format"
  [& args]
  (json/read-str ((client/get "https://vircurex.com/api/get_info_for_currency.json") :body)))
  ;(json/read-str market-json))

