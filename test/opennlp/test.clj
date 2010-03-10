(ns opennlp.test
  (:use [opennlp.nlp])
  (:use [clojure.test]))

(def get-sentences (make-sentence-detector "models/EnglishSD.bin.gz"))
(def tokenize (make-tokenizer "models/EnglishTok.bin.gz"))
(def pos-tag (make-pos-tagger "models/tag.bin.gz"))

(deftest sentence-split-test
         (is (= (get-sentences "First sentence. Second sentence? Here is another one. And so on and so forth - you get the idea...")
                ["First sentence. " "Second sentence? " "Here is another one. " "And so on and so forth - you get the idea..."]))
         (is (= (get-sentences "'Hmmm.... now what?' Mr. Green said to H.A.L.")
                ["'Hmmm.... now what?' Mr. Green said to H.A.L."])))
