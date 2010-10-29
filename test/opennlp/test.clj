(ns opennlp.test
  (:use [opennlp.nlp])
  (:use [opennlp.tools.lazy])
  (:use [clojure.test])
  (:import [java.io File FileNotFoundException]))

(def get-sentences (make-sentence-detector "models/en-sent.bin"))
(def tokenize (make-tokenizer "models/en-token.bin"))
(def pos-tag (make-pos-tagger "models/en-pos-maxent.bin"))
(def name-find (make-name-finder "models/namefind/person.bin.gz"))
#_(def chunker (make-treebank-chunker "models/EnglishChunk.bin.gz"))


(deftest sentence-split-test
         (is (= (get-sentences "First sentence. Second sentence? Here is another one. And so on and so forth - you get the idea...")
                ["First sentence." "Second sentence?" "Here is another one." "And so on and so forth - you get the idea..."]))
         (is (= (get-sentences "'Hmmm.... now what?' Mr. Green said to H.A.L.")
                ["'Hmmm.... now what?'" "Mr. Green said to H.A.L."])))


(deftest tokenizer-test
    (is (= (tokenize "First sentence.")
           ["First" "sentence" "."]))
    (is (= (tokenize "Mr. Smith gave a car to his son on Friday.")
           ["Mr." "Smith" "gave" "a" "car" "to" "his" "son" "on" "Friday" "."])))

(deftest pos-tag-test
    (is (= (pos-tag (tokenize "Mr. Smith gave a car to his son on Friday."))
           '(["Mr." "NNP"] ["Smith" "NNP"] ["gave" "VBD"] ["a" "DT"] ["car" "NN"] ["to" "TO"] ["his" "PRP$"] ["son" "NN"] ["on" "IN"] ["Friday" "NNP"] ["." "."]))))

(comment
  (deftest name-finder-test

(deftest name-finder-test
    (is (= (name-find (tokenize "My name is Lee, not John"))
           '("Lee" "John"))))


(comment
  
  (deftest chunker-test
    (is (= (chunker (pos-tag (tokenize "The override system is meant to deactivate the accelerator when the brake pedal is pressed.")))
           '({:phrase ["The" "override" "system"] :tag "NP"}
             {:phrase ["is" "meant" "to" "deactivate"] :tag "VP"}
             {:phrase ["the" "accelerator"] :tag "NP"}
             {:phrase ["when"] :tag "ADVP"}
             {:phrase ["the" "brake" "pedal"] :tag "NP"}
             {:phrase ["is" "pressed"] :tag "VP"}))))

  (try
    (do
      (def parser (make-treebank-parser "parser-models/build.bin.gz" "parser-models/check.bin.gz" "parser-models/tag.bin.gz" "parser-models/chunk.bin.gz" "parser-models/head_rules"))
      (deftest parser-test
        (is (= (parser ["This is a sentence ."])
               ["(TOP (S (NP (DT This)) (VP (VBZ is) (NP (DT a) (NN sentence))) (. .)))"]))))
    (catch FileNotFoundException e
      (println "Unable to execute treebank-parser tests. Download the model files to $PROJECT_ROOT/parser-models.")))

  (deftest no-model-file-test
    (is (thrown? FileNotFoundException (make-sentence-detector "nonexistantfile")))
    (is (thrown? FileNotFoundException (make-tokenizer "nonexistantfile")))
    (is (thrown? FileNotFoundException (make-pos-tagger "nonexistantfile")))
    (is (thrown? FileNotFoundException (make-name-finder "nonexistantfile" "anotherfilethatdoesnotexist")))
    (is (thrown? FileNotFoundException (make-treebank-chunker "nonexistantfile")))
    (is (thrown? FileNotFoundException (make-treebank-parser "nonexistantfile" "asdf" "fdsa" "qwer" "rewq"))))

  (deftest laziness-test
    (let [s (get-sentences "First sentence. Second sentence?")]
      (is (= (type (lazy-tokenize s tokenize))
             clojure.lang.LazySeq))
      (is (= (first (lazy-tokenize s tokenize))
             ["First" "sentence" "."]))
      (is (= (type (lazy-tag s tokenize pos-tag))
             clojure.lang.LazySeq))
      (is (= (first (lazy-tag s tokenize pos-tag))
             '(["First" "RB"] ["sentence" "NN"] ["." "."])))
      (is (= (type (lazy-chunk s tokenize pos-tag chunker))
             clojure.lang.LazySeq))
      (is (= (first (lazy-chunk s tokenize pos-tag chunker))
             '({:phrase ["First"], :tag "ADVP"} {:phrase ["sentence"], :tag "NP"})))))

  )


