(defproject clojure-opennlp "0.2.1-SNAPSHOT"
  :description "Natural Language Processing with Clojure, library for opennlp."
  :url "http://github.com/dakrone/clojure-opennlp"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.apache.opennlp/opennlp-tools "1.5.2-incubating"]]
  :profiles {:1.2 {:dependencies [[org.clojure/clojure "1.2.1"]]}
             :1.3 {:dependencies [[org.clojure/clojure "1.3.0"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.0-alpha6"]]}}
  :aliases {"all" ["with-profile" "dev,1.2:dev,1.3:dev:dev,1.5"]}
  :jvm-opts ["-Xmx2048m"])

