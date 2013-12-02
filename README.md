# clj-vircurex

Unofficial [Vircurex](https://vircurex.com) [trading API library](https://vircurex.com/welcome/api) for Clojure.

WARNING: This is alpha software. Do not use in production.

## Installation

    $ git clone https://github.com/jphackworth/clj-vircurex


This assumes an environment with Clojure 1.5.1 and [Leiningen](https://github.com/technomancy/leiningen) installed.

## Configuration

### Set your API keys

- Login to your Vircurex account
- Click "Settings"
- Click "API"
- Select the checkboxes for API features you want to enable
- For each enabled feature, enter a strong password (20+ characters) in the input field
- Click save at the bottom of the page to apply

More info can be found here: [Vircurex Trading API Documentation](https://vircurex.com/welcome/api)

### Setup Configuration File

See the example configuration file in [doc/clj-vircurex.toml.example](https://github.com/jphackworth/clj-vircurex/blob/master/doc/clj-vircurex.toml.example)
     
    $ cd clj-vircurex 
    $ cp doc/clj-vircurex.toml.example $HOME/.clj-vircurex.toml
    $ chmod 600 $HOME/.clj-vircurex.toml
    $ vi $HOME/.clj-vircurex.toml 

Create a file in $HOME/.clj-vircurex.toml using the above as a template. Fill out the keys
to match your settings on Vircurex.

## Usage

### Using Interactively in REPL

    $ cd clj-vircurex
    $ lein repl
    nREPL server started on port 52856 on host 127.0.0.1
    REPL-y 0.3.0
    Clojure 1.5.1
        Docs: (doc function-name-here)
              (find-doc "part-of-name-here")
      Source: (source function-name-here)
     Javadoc: (javadoc java-object-or-class-here)
        Exit: Control+D or (exit) or (quit)
     Results: Stored in vars *1, *2, *3, an exception in *e

    user=> (use 'clj-vircurex.core)
    nil
    user=> (read-config) ; loads up config from $HOME/.clj-vircurex.toml 
    #'clj-vircurex.core/*config*
    user=>     

### Using in a Clojure Application

This library is available on Clojars: [https://clojars.org/clj-vircurex](https://clojars.org/clj-vircurex)

If you're using Leiningen:

- Add [clj-vircurex "0.0.1"] to project.clj as a dependency
- In your source code, (:use [clj-vircurex.core])

## API

### Design Discussion

Most authenticated api calls have two ways of using them.

The "full" version which more closely matches the [trading API documentation](https://vircurex.com/welcome/api), and a "simplified" version which usually reduces verbosity, intended for interactive use at repl console. 

Generally speaking, the full version function name will have a -order/-orders. For instance: 

    (create-order :buy :ltc 1.005 0.008) ; versus 
    (buy :ltc 1.005 0.008)

    (delete-order 12345) ; versus

    (def myorder (buy :ltc 1.005 0.0008))
    (delete (myorder)) 

### Library Functions

* [Get Market Data](https://github.com/jphackworth/clj-vircurex#get-market-data)
* [Read Orders](https://github.com/jphackworth/clj-vircurex#read-orders)
* [Get Balances](https://github.com/jphackworth/clj-vircurex#get-balances)
* [Create Order](https://github.com/jphackworth/clj-vircurex#create-order)
* [Delete Order](https://github.com/jphackworth/clj-vircurex#delete-order)
* [Release Order](https://github.com/jphackworth/clj-vircurex#release-order)      

### Get market data

This does a live fetch of market data from Vircurex. It does not require authentication. 

    (get-market-data)

The limit of fetch frequency is 5 seconds. While testing, to minimise being throttled or blocked, save the market data to a variable and test with that:

    (def mkt (get-market-data))

### Read Orders

NOTE: The format of data has changed between 0.0.1 and 0.0.2

Full version:

    (read-orders 0) ; 0 = unreleased
    (read-orders 1) ; 1 = released

Simplified:

    (unreleased) ; unreleased orders
    (released) ; released orders

These may change to unreleased/released in future.

### Get Balances

To get your complete list of balances

    (get-balances)

To get a balance for specific currency

    (get-balances :nmc) 

### Create Order

Full version:

    (create-order :buy :ltc 1.0005 0.0008) ; order type, currency, amount, unit price
    (create-order :sell :ltc 1.0005 10.001)

Simplified buy/sell:

    (buy :ltc 1.005 0.0008) ; keyword-ized currency (requires :nmc, :ltc, etc), amount, unit price
    (sell :ltc 1.005 10.001)

### Delete Order

Full version:

    (delete-order 12345) ; orderid  
    (delete-order 12346) ; orderid 

Simplified delete example:

    (def my-order (buy :ltc 1.005 0.0008)) ; saves order info to "my-order"
    (delete my-order)

#### Issues with Delete Order API and workaround

There appears to be a bug/mistake in the API documentation. create_order gives you an orderid but no order type. delete-order requires both orderid and order type. It turns out that delete order doesn't care what the order type supplied is, so long as it's provided. So what I've done is supply "test" as the order type. The API happily deletes the order regardless.

To Vircurex developers, please update API to only require orderid. 

### Release Order

Full version:

    (release-order 12345)

Simplified

    (def my-order (buy :ltc 1.005 0.0008))
    (release my-order)

### Bugs

No testing implemented. You should not use this in production.

## License

Copyright © 2013 John P. Hackworth

Distributed under the Mozilla Public License Version 2.0
