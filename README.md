# clj-vircurex

Vircurex (https://vircurex.com) trading API library for Clojure.

WARNING: This is incomplete.

## Installation

Download from https://github.com/jphackworth/clj-vircurex.

This assumes an environment with Clojure 1.5.1 and Leiningen (https://github.com/technomancy/leiningen) installed.

## Usage

### Configuration

See the example configuration file in doc/clj-vircurex.toml.example

Create a file in $HOME/.clj-vircurex.toml using the above as a template. Fill out the keys
to match your settings on Vircurex.

    $ git clone https://github.com/jphackworth/clj-vircurex.git
    $ cd clj-vircurex
    $ lein repl

## Implemented

### Get market data

This does a live fetch of market data from Vircurex. It does not require authentication. 

    (get-market-data)

The limit of fetch frequency is 5 seconds. While testing, to minimise being throttled or blocked, save the market data to a variable and test with that:

    (def mkt (get-market-data))



### Get Order

Full read-orders version:

    (read-orders 0) ; 0 = unreleased
    (read-orders 1) ; 1 = released

Simplified:

    (unreleased-orders)
    (released-orders)

These may change to unreleased/released in future.

### Get Balances

To get your complete list of balances

    (get-balances)

To get a balance for specific currency

    (get-balances "nmc") ; this may change to using keywords instead of strings... ie :nmc vs "nmc"

### Create Order

Full create-order version:

    (create-order "BUY" "ltc" 1.0005 0.0008) ; order type, currency, amount, unit price
    (create-order "SELL" "ltc" 1.0005 10.001)

Simplified buy/sell:

    (buy :ltc 1.005 0.0008) ; keyword-ized currency (requires :nmc, :ltc, etc), amount, unit price
    (sell :ltc 1.005 10.001)

### Delete Order

Full delete-order version:

    (delete-order 12345) ; orderid  
    (delete-order 12346) ; orderid 

NOTE: There appears to be a bug/mistake in the API documentation. create_order gives you an orderid but no order type. delete-order requires both orderid and order type. It turns out that delete order doesn't care what the order type supplied is, so long as it's provided. So what I've done is supply "test" as the order type. The API happily deletes the order regardless.

To Vircurex developers, please update API to only require orderid. It looks like you only care about it, so maybe you've just left in otype for backwards compatibility?

Simplified delete example:

    (def my-order (buy :ltc 1.005 0.0008)) ; saves order info to "my-order"
    (delete my-order)

### Bugs

Lots of API functionality isn't implemented.

## License

Copyright © 2013 John P. Hackworth

Distributed under the MIT License
