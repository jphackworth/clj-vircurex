(defproject clj-vircurex "0.0.1-SNAPSHOT"
  :description "Vircurex Trading API library"
  :url "https://github.com/jphackworth/clj-vircurex"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.5.1"]
   [clj-json "0.5.3"]
    [org.clojure/data.json "0.2.3"]
    [clj-http "0.7.7"]
    [clj-time "0.6.0"]
    [pandect "0.3.0"]
    [clj-toml "0.3.1"]
    [clojurewerkz/quartzite "1.1.0"]
    [table "0.4.0"]]
  :main ^:skip-aot clj-vircurex.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
