(ns opennlp.test.tools.lazy
  (:use [clojure.test]
        [opennlp.tools.lazy]
        [opennlp.nlp :only [make-sentence-detector make-tokenizer
                            make-pos-tagger]]
        [opennlp.treebank :only [make-treebank-chunker]]))

(def get-sentences (make-sentence-detector "models/en-sent.bin"))
(def tokenize (make-tokenizer "models/en-token.bin"))
(def pos-tag (make-pos-tagger "models/en-pos-maxent.bin"))
(def chunker (make-treebank-chunker "models/en-chunker.bin"))

(deftest laziness-test
  (let [s (get-sentences "First sentence. Second sentence?")]
    (is (= (type (lazy-tokenize s tokenize))
           clojure.lang.LazySeq))
    (is (= (first (lazy-tokenize s tokenize))
           ["First" "sentence" "."]))
    (is (= (type (lazy-tag s tokenize pos-tag))
           clojure.lang.LazySeq))
    (is (= (first (lazy-tag s tokenize pos-tag))
           '(["First" "RB"] ["sentence" "NN"] ["." "."]))))
  (testing "should lazily read sentences from a file"
    (with-open [rdr (clojure.java.io/reader "test/sentence-file")]
      (let [sentences (sentence-seq rdr get-sentences)]
        (is (= (type sentences) clojure.lang.Cons))
        (is (= sentences
               ["This is a sentence." "Another sentence."
                "My name is awesome." "Another line."]))))))

(deftest lazy-chunker-test
  (let [s ["First sentence." "Second sentence?"]]
    (is (= (type (lazy-chunk s tokenize pos-tag chunker))
           clojure.lang.LazySeq))
    (is (= (first (lazy-chunk s tokenize pos-tag chunker))
           '({:phrase ["First"], :tag "ADVP"}
             {:phrase ["sentence"], :tag "NP"})))))

