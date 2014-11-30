(defproject clojure-opennlp "0.3.4-SNAPSHOT"
  :description "Natural Language Processing with Clojure, library for opennlp."
  :url "http://github.com/dakrone/clojure-opennlp"
  :min-lein-version "2.0.0"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.apache.opennlp/opennlp-tools "1.5.3"]
                 [instaparse "1.3.4"]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.6.0"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}}
  :aliases {"all" ["with-profile" "dev,1.5:dev"]}
  :jvm-opts ["-Xmx2048m"])
