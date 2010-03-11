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

(deftest tokenizer-test
         (is (= (tokenize "First sentence.")
                ["First" "sentence" "."]))
         (is (= (tokenize "Mr. Smith gave a car to his son on Friday.")
                ["Mr." "Smith" "gave" "a" "car" "to" "his" "son" "on" "Friday" "."])))

(deftest pos-tag-test
         (is (= (pos-tag (tokenize "Mr. Smith gave a car to his son on Friday."))
                '(["Mr." "NNP"] ["Smith" "NNP"] ["gave" "VBD"] ["a" "DT"] ["car" "NN"] ["to" "TO"] ["his" "PRP$"] ["son" "NN"] ["on" "IN"] ["Friday" "NNP"] ["." "."]))))

