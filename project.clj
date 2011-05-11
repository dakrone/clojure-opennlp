(defproject clojure-opennlp "0.1.7-SNAPSHOT"
  :description "Natural Language Processing with Clojure, library for opennlp.
                http://github.com/dakrone/clojure-opennlp"
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [org.apache.opennlp/opennlp-tools "1.5.1-incubating"]]
  :dev-dependencies [[marginalia "0.5.1"]
                     [slamhound "1.1.1"]]
  :jvm-opts ["-Xmx1024m"])

