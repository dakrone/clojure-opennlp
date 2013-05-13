(ns opennlp.test.treebank
  (:use [opennlp.treebank])
  (:use [clojure.test])
  (:use [opennlp.nlp :only [make-tokenizer make-pos-tagger]])
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

(deftest parser-test-normal
  (try
    (let [parser (make-treebank-parser "parser-model/en-parser-chunking.bin")]
      (is (= (parser ["This is a sentence ."])
             [(str "(TOP (S (NP (DT This)) (VP (VBZ is) (NP (DT a)"
                   " (NN sentence))) (. .)))")]))
      (is (= (make-tree (first (parser ["This is a sentence ."])))
             '{:tag TOP
               :chunk (
                       {:tag S
                        :chunk ({:tag NP
                                 :chunk ({:tag DT :chunk ("This")})}
                                {:tag VP
                                 :chunk ({:tag VBZ :chunk ("is")}
                                         {:tag NP
                                          :chunk ({:tag DT :chunk ("a")}
                                                  {:tag NN
                                                   :chunk ("sentence")})})}
                                {:tag . :chunk (".")})})})))
    (catch FileNotFoundException e
      (println "Unable to execute treebank-parser tests."
               "Download the model files to $PROJECT_ROOT/parser-models."))))

(deftest parser-test-with-bad-chars
  (try
    (let [parser (make-treebank-parser "parser-model/en-parser-chunking.bin")]
      (is (= (parser ["2:30 isn't bad"])
             ["(TOP (NP (CD 2:30) (RB isn't) (JJ bad)))"]))
      (is (= (make-tree (first (parser ["2:30 isn't bad"])))
             '{:tag TOP,
               :chunk ({:tag NP,
                        :chunk ({:tag CD,
                                 :chunk ("2:30")}
                                {:tag RB, :chunk ("isn't")}
                                {:tag JJ, :chunk ("bad")})})
               })))
    (catch FileNotFoundException e
      (println "Unable to execute treebank-parser tests."
               "Download the model files to $PROJECT_ROOT/parser-models."))))

#_(deftest treebank-coref-test
    (try
      (let [tbl (make-treebank-linker "coref")
            s (parser ["Mary said she would help me ."
                       "I told her I didn't need her help ."])]
        (is (= [] (tbl s))))
      (catch FileNotFoundException e
        (println "Unable to execute treebank-parser tests."
                 "Download the model files to $PROJECT_ROOT/parser-models."))))

