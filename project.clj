(defproject clojure-opennlp "0.2.1"
  :description "Natural Language Processing with Clojure, library for opennlp."
  :url "http://github.com/dakrone/clojure-opennlp"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.apache.opennlp/opennlp-tools "1.5.2-incubating"]]
  :profiles {:1.3 {:dependencies [[org.clojure/clojure "1.3.0"]]}
             :1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}}
  :aliases {"all" ["with-profile" "dev,1.3:dev,1.4:dev"]}
  :jvm-opts ["-Xmx2048m"])

