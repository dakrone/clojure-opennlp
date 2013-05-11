(ns opennlp.test.treebank-tree
  (:use [opennlp.treebank])
  (:use [clojure.test])
  (:import [java.io File FileNotFoundException]))

(deftest make-tree-test-time-and-integer-strings
  (try
    (let [parser (make-treebank-parser "parser-model/en-parser-chunking.bin")]
      (is (= (make-tree (first (parser ["2 is an integer ."])))
             '{:tag TOP
               :chunk ({:tag S
                        :chunk ({:tag NP
                                 :chunk ({:tag CD :chunk ("2")})}
                                {:tag VP
                                 :chunk ({:tag VBZ :chunk ("is")}
                                         {:tag NP
                                          :chunk ({:tag DT :chunk ("an")}
                                                  {:tag NN :chunk
                                                   ("integer")})})}
                                {:tag . :chunk (".")})})}))
      (is (= (make-tree (first (parser ["\"2:30\" is a time ."])))
             '{:tag TOP
               :chunk ({:tag S
                        :chunk ({:tag NP
                                 :chunk ({:tag NN
                                          :chunk
                                          ("\"2:30\"")})}
                                {:tag VP
                                 :chunk ({:tag VBZ :chunk ("is")}
                                         {:tag NP
                                          :chunk ({:tag DT :chunk ("a")}
                                                  {:tag NN :chunk ("time")})})}
                                {:tag . :chunk (".")})})})))
    (catch FileNotFoundException e
      (println "Unable to execute treebank-parser tests."
               "Download the model files to $PROJECT_ROOT/parser-model."))))

