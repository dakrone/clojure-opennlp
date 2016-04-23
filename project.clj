(defproject clojure-opennlp "0.3.4-SNAPSHOT"
  :description "Natural Language Processing with Clojure, library for opennlp."
  :url "http://github.com/dakrone/clojure-opennlp"
  :min-lein-version "2.0.0"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.apache.opennlp/opennlp-tools "1.5.3"]
                 [instaparse "1.4.1"]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.6.0"]]
                   :plugins [[lein-marginalia "0.8.0"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}}
  :aliases {"all" ["with-profile" "dev,1.5:dev:dev,1.7:dev,1.8"]}
  :jvm-opts ["-Xmx2048m"])
