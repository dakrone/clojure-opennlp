(ns opennlp.test.treebank
  (:use [opennlp.treebank])
  (:use [clojure.test])
  (:use [opennlp.nlp :only [make-tokenizer make-pos-tagger]])
  (:use [opennlp.tools.lazy :only [lazy-chunk]])
  (:import [java.io File FileNotFoundException]))

(def tokenize (make-tokenizer "models/en-token.bin"))
(def pos-tag (make-pos-tagger "models/en-pos-maxent.bin"))
(def chunker (make-treebank-chunker "models/en-chunker.bin"))

(deftest chunker-test
  (is (= (chunker (pos-tag (tokenize (str "The override system is meant to"
                                          " deactivate the accelerator when"
                                          " the brake pedal is pressed."))))
         '({:phrase ["The" "override" "system"] :tag "NP"}
           {:phrase ["is" "meant" "to" "deactivate"] :tag "VP"}
           {:phrase ["the" "accelerator"] :tag "NP"}
           {:phrase ["when"] :tag "ADVP"}
           {:phrase ["the" "brake" "pedal"] :tag "NP"}
           {:phrase ["is" "pressed"] :tag "VP"}))))

(deftest no-model-file-test
  (is (thrown? FileNotFoundException (make-treebank-chunker "nonexistantfile")))
  (is (thrown? FileNotFoundException (make-treebank-parser "nonexistantfile"))))

(try
  (let [parser (make-treebank-parser "parser-model/en-parser-chunking.bin")]
    (deftest parser-test
      (is (= (parser ["This is a sentence ."])
             [(str "(TOP (S (NP (DT This)) (VP (VBZ is) (NP (DT a)"
                   " (NN sentence))) (. .)))")]))
      (is (= (make-tree (first (parser ["This is a sentence ."])))
             '{:chunk
               {:chunk
                ({:chunk
                  {:chunk "This", :tag DT}, :tag NP}
                 {:chunk
                  ({:chunk "is", :tag VBZ}
                   {:chunk ({:chunk "a" :tag DT}
                            {:chunk "sentence" :tag NN}) :tag NP}) :tag VP}
                 {:chunk ".", :tag .}), :tag S}, :tag TOP})))
    #_(deftest treebank-coref-test
      (let [tbl (make-treebank-linker "coref")
            s (parser ["Mary said she would help me ."
                       "I told her I didn't need her help ."])]
        (is (= [] (tbl s))))))
  (catch FileNotFoundException e
    (println "Unable to execute treebank-parser tests."
             "Download the model files to $PROJECT_ROOT/parser-models.")))

(deftest laziness-test
  (let [s ["First sentence." "Second sentence?"]]
    (is (= (type (lazy-chunk s tokenize pos-tag chunker))
           clojure.lang.LazySeq))
    (is (= (first (lazy-chunk s tokenize pos-tag chunker))
           '({:phrase ["First"], :tag "ADVP"}
             {:phrase ["sentence"], :tag "NP"})))))
