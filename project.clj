(defproject clojure-opennlp "0.1.8-SNAPSHOT"
  :description "Natural Language Processing with Clojure, library for opennlp.
                http://github.com/dakrone/clojure-opennlp"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.apache.opennlp/opennlp-tools "1.5.1-incubating"]]
  :dev-dependencies [[slamhound "1.1.1"]
                     [lein-marginalia "0.6.0"]
                     [lein-multi "1.0.0"]]
  :multi-deps {"1.2.1" [[org.clojure/clojure "1.2.1"]
                        [org.apache.opennlp/opennlp-tools "1.5.1-incubating"]]
               "1.4.0" [[org.clojure/clojure "1.4.0-alpha1"]
                        [org.apache.opennlp/opennlp-tools "1.5.1-incubating"]]}
  :jvm-opts ["-Xmx1024m"])

