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



### 


* Get market data: (get-market-data)
* Get open orders: (read-orders 1)
* Get your balances: (get-balances)
* Get you balance for a given currency: (get-balance "ltc")  
* Create order: (create-order "SELL" "ltc" 10 0.05)

### Usage / Examples

    $ cd clj-vircurex
    $ lein repl
    clj-vircurex.core=> (get-market-data)
    clj-vircurex.core=> ((((get-market-data) :LTC) :BTC) :last_trade) 

### Bugs

Lots of API functionality isn't implemented.

## License

Copyright © 2013 John P. Hackworth

Distributed under the MIT License
