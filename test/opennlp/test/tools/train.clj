(ns opennlp.test.tools.train
  "Tests training models for the OpenNLP tools"
  (:use [clojure.test])
  (:require [opennlp.nlp :as nlp]
            [opennlp.treebank :as tb]
            [opennlp.tools.train :as train])
  (:import [java.io File FileOutputStream]))


(deftest sentence-training-test
  (let [sent-model (train/train-sentence-detector "training/sentdetect.train")
        get-sentences (nlp/make-sentence-detector sent-model)]
    (is
     (= (get-sentences (str "Being at the polls was just like being at "
                            "church. I didn't smell a drop of liquor, and "
                            "we didn't have a bit of trouble."))
        ["Being at the polls was just like being at church."
         (str "I didn't smell a drop of liquor, and we didn't"
              " have a bit of trouble.")]))))

(deftest tokenizer-training-test
  (let [token-model (train/train-tokenizer "training/tokenizer.train")
        tokenizer (nlp/make-tokenizer token-model)]
    (is (= (tokenizer "Being at the polls was just like being at church.")
           ["Being" "at" "the" "polls"
            "was" "just" "like" "being"
            "at" "church" "."]))))

(deftest postagger-training-test
  (let [pos-model (train/train-pos-tagger "training/postagger.train")
        pos-tagger (nlp/make-pos-tagger pos-model)]
    (is (= (pos-tagger ["Being" "at" "the" "polls" "was" "just" "like"
                        "being" "at" "church."])
           '(["Being" "VBD"] ["at" "IN"] ["the" "NN"] ["polls" ","]
               ["was" "VBD"] ["just" "RB"] ["like" "IN"] ["being" "NN"]
                 ["at" "IN"] ["church." "NN"])))))

(deftest chunker-training-test
  (let [chunk-model (train/train-treebank-chunker "training/chunker.train")
        chunker (tb/make-treebank-chunker chunk-model)
        pos-tag (nlp/make-pos-tagger "models/en-pos-maxent.bin")
        tokenize (nlp/make-tokenizer "models/en-token.bin")]
    (is (= (chunker
            (pos-tag
             (tokenize (str "He reckons the current account deficit "
                            "will narrow to only #1.8 billion in September."))))
           '({:phrase ["He"], :tag "NP"}
             {:phrase ["reckons"], :tag "VP"}
             {:phrase ["the" "current" "account" "deficit"], :tag "NP"}
             {:phrase ["will" "narrow"], :tag "VP"}
             {:phrase ["to"], :tag "PP"}
             {:phrase ["only" "#" "1.8" "billion"], :tag "NP"}
             {:phrase ["in"], :tag "PP"}
             {:phrase ["September"], :tag "NP"})))))

(deftest name-finder-training-test
  (let [namefind-model (train/train-name-finder "training/named_org.train")
        name-finder (nlp/make-name-finder namefind-model)]
    (is (= (name-finder ["The" "Giants" "win" "the" "World Series" "."])
           '("Giants" "World Series")))))

(deftest treebank-parser-training-test
  (let [tb-parser-model (train/train-treebank-parser "training/parser.train"
                                                     "training/head_rules")
        parser (tb/make-treebank-parser tb-parser-model)]
    (is (= (parser ["This is a sentence ."])
           [(str "(INC (NP (DT This)) (NP (DT is)) (NP (DT a))"
                 " (DT sentence) (. .))")]))))

(deftest write-out-training-model-test
  (let [token-model (train/train-tokenizer "training/tokenizer.train")
        tmp-file (java.io.File/createTempFile "testmodel" ".train")
        out-stream (FileOutputStream. tmp-file)]
    (.deleteOnExit tmp-file)
    (train/write-model token-model out-stream)
    (is (=  (.exists tmp-file) true))
    (let [tokenizer (nlp/make-tokenizer (.getAbsolutePath tmp-file))]
      (is (= (tokenizer "Being at the polls was just like being at church.")
             ["Being" "at" "the" "polls"
              "was" "just" "like" "being"
              "at" "church" "."])))))

(deftest categorization-training-test
  (let [cat-model (train/train-document-categorization "training/doccat.train")
        get-category (nlp/make-document-categorizer cat-model)]
    (let [test-cat (get-category
                    "The third verse of the song was quite upbeat.")]
      (is (= (:best-category test-cat) "Happy"))
      (is (= (count (:probabilities (meta test-cat))) 2)))

    (let [test-cat (get-category
                    "There was a sense of foreboding at the outset.")]
      (is (= (:best-category test-cat) "Unhappy"))
      (is (= (count (:probabilities (meta test-cat))) 2)))
    (is (= (:best-category
            (get-category "The sun was shining, smiles everywhere."))
           "Happy"))
    (is (= (:best-category
            (get-category (str "The confused prisoner could not figure "
                               "out which way to go.")))
           "Unhappy"))
    (is (= (:best-category
            (get-category (str "The frowning man chastized his son for "
                               "not divulging the truth.")))
           "Unhappy"))))
