(ns opennlp.test.nlp
  (:use [opennlp.nlp]
        [clojure.test])
  (:import [java.io File FileNotFoundException]))

(def get-sentences (make-sentence-detector "models/en-sent.bin"))
(def tokenize (make-tokenizer "models/en-token.bin"))
(def pos-tag (make-pos-tagger "models/en-pos-maxent.bin"))
(def name-find (make-name-finder "models/namefind/en-ner-person.bin"))
(def detokenize (make-detokenizer "models/english-detokenizer.xml"))

(deftest sentence-split-test
  (is (= (get-sentences (str "First sentence. Second sentence? Here is another"
                             " one. And so on and so forth - you get the"
                             " idea..."))
         ["First sentence." "Second sentence?" "Here is another one."
          "And so on and so forth - you get the idea..."]))
  (is (= (get-sentences "'Hmmm.... now what?' Mr. Green said to H.A.L.")
         ["'Hmmm.... now what?'" "Mr. Green said to H.A.L."])))


(deftest tokenizer-test
  (is (= (tokenize "First sentence.")
         ["First" "sentence" "."]))
  (is (= (tokenize "Mr. Smith gave a car to his son on Friday.")
         ["Mr." "Smith" "gave" "a" "car" "to" "his" "son" "on" "Friday" "."])))

(deftest pos-tag-test
  (is (= (pos-tag (tokenize ""))
         '()))
  (is (= (pos-tag (tokenize "Mr. Smith gave a car to his son on Friday."))
         '(["Mr." "NNP"] ["Smith" "NNP"] ["gave" "VBD"] ["a" "DT"] ["car" "NN"]
             ["to" "TO"] ["his" "PRP$"] ["son" "NN"] ["on" "IN"]
               ["Friday" "NNP"] ["." "."]))))

(deftest name-finder-test
  (is (= (name-find (tokenize "My name is Lee, not John"))
         '("Lee" "John")))
  (is (= (name-find ["adsf"])
         '()))
  (is (= (name-find (tokenize "My name is James Brown"))
         '("James Brown"))
      "should find names with two words"))

(deftest detokenizer-test
  (is (= (detokenize (tokenize "I don't think he would've."))
         "I don't think he would've."))
  (is (= (detokenize (tokenize "This isn't the right thing."))
         "This isn't the right thing."))
  (is (= (detokenize (tokenize "Where'd you go?"))
         "Where'd you go?"))
  (is (= (detokenize (tokenize "I'll get that tomorrow."))
         "I'll get that tomorrow."))
  (is (= (detokenize (tokenize "She's the best."))
         "She's the best."))
  (is (= (detokenize (tokenize "I'm not sure."))
         "I'm not sure."))
  (is (= (detokenize (tokenize "Mary likes cows (Mary is a cow)."))
         "Mary likes cows (Mary is a cow)."))
  (is (= (detokenize (tokenize "Mary exclaimed \"I am a cow!\""))
         "Mary exclaimed \"I am a cow!\""))
  (is (= (detokenize ["I" "know" "what" "\"" "it" "\"" "means" "well" "enough"
                      "," "when" "I" "find" "a" "thing" "," "said" "the" "Duck"
                      ":" "its" "generally" "a" "frog" "or" "a" "worm" "."])
         (str "I know what \"it\" means well enough, when"
              " I find a thing, said the Duck: its"
              " generally a frog or a worm."))))

(deftest precondition-test
  (is (thrown? java.lang.AssertionError (get-sentences 1)))
  (is (thrown? java.lang.AssertionError (tokenize 1)))
  (is (thrown? java.lang.AssertionError (pos-tag "foooo")))
  (is (thrown? java.lang.AssertionError (name-find "asdf"))))

(deftest no-model-file-test
  (is (thrown? FileNotFoundException
               (make-sentence-detector "nonexistantfile")))
  (is (thrown? FileNotFoundException (make-tokenizer "nonexistantfile")))
  (is (thrown? FileNotFoundException (make-pos-tagger "nonexistantfile")))
  (is (thrown? FileNotFoundException (make-name-finder "nonexistantfile"))))

(deftest parse-categories-test
  (let [outcomes-string "CAT1[0.123] CAT2[0.234] CAT3[0.345] CAT4[0.456]"
        outcomes [0.123456 0.234567 0.345678 0.456789]]
    (is (= (count (parse-categories outcomes-string outcomes)) 4))
    (is (= (get (parse-categories outcomes-string outcomes) "CAT1")) 0.123456)
    (is (= (get (parse-categories outcomes-string outcomes) "CAT2")) 0.234567)
    (is (= (get (parse-categories outcomes-string outcomes) "CAT3")) 0.345678)
    (is (= (get (parse-categories outcomes-string outcomes) "CAT4")) 0.456789)))
