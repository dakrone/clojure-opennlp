(ns opennlp.test.treebank-tree
  (:use [opennlp.treebank])
  (:use [clojure.test])
  (:use [opennlp.nlp :only [make-tokenizer make-pos-tagger]])
  (:import [java.io File FileNotFoundException]))

(def tokenize (make-tokenizer "models/en-token.bin"))
(def pos-tag (make-pos-tagger "models/en-pos-maxent.bin"))
(def chunker (make-treebank-chunker "models/en-chunker.bin"))


(try
  (let [parser (make-treebank-parser "parser-model/en-parser-chunking.bin")]
    (deftest parser-test-time-strings
      (is (= (make-tree (first (parser ["\"2:30\" is a time ."])))
           '{:tag TOP,
             :chunk (
                     {:tag S,
                      :chunk (
                              {:tag NP,
                               :chunk (
                                       {:tag NN,
                                        :chunk ("-DQUOTE-2-COLON-30-DQUOTE-")})}
                              {:tag VP,
                               :chunk (
                                       {:tag VBZ,
                                        :chunk ("is")}
                                       {:tag NP,
                                        :chunk (
                                                {:tag DT, :chunk ("a")}
                                                {:tag NN, :chunk ("time")})})}
                              {:tag ., :chunk (".")})})}
                      ))))
  (catch FileNotFoundException e
    (println "Unable to execute treebank-parser tests."
             "Download the model files to $PROJECT_ROOT/parser-model.")))

(try
  (let [parser (make-treebank-parser "parser-model/en-parser-chunking.bin")]
    (deftest make-tree-test-integer-strings
    (is (= (make-tree (first (parser ["2 is an integer ."])))
           '{:tag TOP,
             :chunk (
                     {:tag S,
                      :chunk (
                              {:tag NP,
                               :chunk (
                                       {:tag CD,
                                        :chunk ("2")})}
                              {:tag VP,
                               :chunk (
                                       {:tag VBZ,
                                        :chunk ("is")}
                                       {:tag NP,
                                        :chunk (
                                                {:tag DT, :chunk ("an")}
                                                {:tag NN, :chunk ("integer")})})}
                                                {:tag ., :chunk (".")})})}
           ))))
  (catch FileNotFoundException e
    (println "Unable to execute treebank-parser tests."
             "Download the model files to $PROJECT_ROOT/parser-model.")))


