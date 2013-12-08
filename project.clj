(defproject clojure-opennlp "0.3.3-SNAPSHOT"
  :description "Natural Language Processing with Clojure, library for opennlp."
  :url "http://github.com/dakrone/clojure-opennlp"
  :min-lein-version "2.0.0"
  :dependencies [[org.apache.opennlp/opennlp-tools "1.5.3"]
                 [instaparse "1.2.12"]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}}
  :aliases {"all" ["with-profile" "dev,1.4:dev"]}
  :jvm-opts ["-Xmx2048m"])
