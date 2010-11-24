(ns opennlp.test.train
  (:use [clojure.test])
  (:require [opennlp.nlp :as nlp]
            [opennlp.tools.train :as train]))

(deftest sentence-training-test
  (let [sent-model (train/train-sentence-detector "training/sentdetect.train")
        get-sentences (nlp/make-sentence-detector sent-model)]
    (is (= (get-sentences "Being at the polls was just like being at church. I didn't smell a drop of liquor, and we didn't have a bit of trouble.")
           ["Being at the polls was just like being at church."
            "I didn't smell a drop of liquor, and we didn't have a bit of trouble."]))))

(deftest tokenizer-training-test
  (let [token-model (train/train-tokenizer "training/tokenizer.train")
        tokenizer (nlp/make-tokenizer token-model)]
    (is (= (tokenizer "Being at the polls was just like being at church.")
           ["Being" "at" "the" "polls"
            "was" "just" "like" "being"
            "at" "church" "."]))))
