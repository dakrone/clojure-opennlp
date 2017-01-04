(ns ^{:doc "Namespace containing tools pertaining to the treebank NLP tools.
             This includes treebank chuncking, parsing and linking (coref)."
       :author "Lee Hinman"}
  opennlp.treebank
  (:use [clojure.java.io :only [input-stream]])
  (:require [clojure.string :as str]
            [instaparse.core :as insta])
  (:import (java.util List)
           (opennlp.tools.chunker ChunkerModel ChunkerME)
           (opennlp.tools.cmdline.parser ParserTool)
           (opennlp.tools.parser Parse ParserModel
                                 ParserFactory AbstractBottomUpParser)
           (opennlp.tools.parser.chunking Parser)
           (opennlp.tools.util Span)))

;; Default advance percentage as defined by
;; AbstractBottomUpParser.defaultAdvancePercentage
(def ^:dynamic *advance-percentage* 0.95)

(def ^:dynamic *beam-size* 3)

(defn- split-chunks
  "Partition a sequence of treebank chunks by their phrases."
  [chunks]
  (let [seqnum    (atom 0)
        splitfunc (fn
                    [^String item]
                    (if (.startsWith item "B-")
                      (swap! seqnum inc)
                      @seqnum))]
    (partition-by splitfunc (pop chunks))))


(defn- size-chunk
  "Given a chunk ('B-NP' 'I-NP' 'I-NP' ...), return a vector of the
  chunk type and item count.
  So, for ('B-NP' 'I-NP' 'I-NP') it would return ['B-NP' 3]."
  [tb-chunk]
  (let [chunk-type  (second (re-find #"B-(.*)" (first tb-chunk)))
        chunk-count (count tb-chunk)]
    [chunk-type chunk-count]))


;; Thanks chouser
(defn- split-with-size
  [[v & vs] s]
  (if-not v
    (list s)
    (cons (take v s) (split-with-size vs (drop v s)))))


(defn- de-interleave
  "De-interleave a sequence, returning a vector of the two resulting
  sequences."
  [s]
  [(map first s) (map last s)])


(defstruct treebank-phrase :phrase :tag)

(defmulti make-treebank-chunker
  "Return a function for chunking phrases from pos-tagged tokens based on
  a given model file."
  class)

(defmethod make-treebank-chunker :default
  [modelfile]
  (with-open [modelstream (input-stream modelfile)]
    (make-treebank-chunker (ChunkerModel. modelstream))))

(defmethod make-treebank-chunker ChunkerModel
  [^ChunkerModel model]
  (fn treebank-chunker
    [pos-tagged-tokens]
    (let [chunker (ChunkerME. model)
          [tokens tags] (de-interleave pos-tagged-tokens)
          chunks  (into [] (seq (.chunk chunker 
                                  (into-array ^List tokens) 
                                  (into-array ^List tags))))
          sized-chunks (map size-chunk (split-chunks chunks))
          [types sizes] (de-interleave sized-chunks)
          token-chunks (split-with-size sizes tokens)
          probs (seq (.probs chunker))]
      (with-meta
        (map #(struct treebank-phrase (into [] (last %)) (first %))
             (partition 2 (interleave types token-chunks)))
        {:probabilities probs}))))

(defn phrases
  "Given the chunks from a treebank-chunker, return just a list of phrase
  word-lists."
  [phrases]
  (map :phrase phrases))

(defn phrase-strings
  "Given the chunks from a treebank-chunker, return a list of phrase strings."
  [phrase-chunks]
  (map #(apply str (interpose " " %)) (phrases phrase-chunks)))

;; Docs for treebank chunking:

;;(def chunker (make-treebank-chunker "models/EnglishChunk.bin.gz"))
;;(pprint (chunker (pos-tag (tokenize "The override system is meant to
;;deactivate the accelerator when the brake pedal is pressed."))))

;;(map size-chunk (split-chunks (chunker (pos-tag (tokenize "The
;;override system is meant to deactivate the accelerator when the
;;brake pedal is pressed.")))))

;;opennlp.nlp=> (split-with-size (sizes (map size-chunk (split-chunks
;;(chunker (pos-tag (tokenize "The override system is meant to
;;deactivate the accelerator when the brake pedal is pressed."))))))
;;(tokenize "The override system is meant to deactivate the
;;accelerator when the brake pedal is pressed."))  (("The" "override"
;;"system") ("is" "meant" "to" "deactivate") ("the" "accelerator")
;;("when") ("the" "brake" "pedal") ("is" "pressed") ("."))

;;(["NP" 3] ["VP" 4] ["NP" 2] ["ADVP" 1] ["NP" 3] ["VP" 2])

;;opennlp.nlp=> (pprint (chunker (pos-tag (tokenize "The override
;;system is meant to deactivate the accelerator when the brake pedal
;;is pressed."))))  #<ArrayList [B-NP, I-NP, I-NP, B-VP, I-VP, I-VP,
;;I-VP, B-NP, I-NP, B-ADVP, B-NP, I-NP, I-NP, B-VP, I-VP, O]>

;; So, B-* starts a sequence, I-* continues it. New phrase starts when
;; B-* is encountered

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Treebank parsing
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- strip-parens
  "Treebank-parser does not like parens and braces, so replace them."
  [s]
  (-> s
      (str/replace "\\(" "-LRB-")
      (str/replace "\\)" "-RRB-")
      (str/replace "\\{" "-LCB-")
      (str/replace "\\}" "-RCB-")))


(defn- parse-line
  "Given a line and Parser object, return a list of Parses."
  [line parser]
  (let [line (strip-parens line)
        results (StringBuffer.)
        parse-num 1]
    (.show ^Parse (first (ParserTool/parseLine line parser parse-num)) results)
    (str results)))


(defmulti make-treebank-parser
  "Return a function for treebank parsing a sequence of sentences, based on
  a given model file."
  class)

(defmethod make-treebank-parser :default
  [modelfile]
  (with-open [modelstream (input-stream modelfile)]
    (make-treebank-parser (ParserModel. modelstream))))

(defmethod make-treebank-parser ParserModel
  [model]
  (fn treebank-parser
    [text]
    (let [parser (ParserFactory/create model
                                       *beam-size*
                                       *advance-percentage*)
          parses (map #(parse-line % parser) text)]
      (vec parses))))

(def ^:private s-parser
  (insta/parser
   "E = <'('> T <WS> (T | (E <WS?>)+) <')'> <WS?> ; T = #'[^)\\s]+' ; WS = #'\\s+'"))

;; Credit for this function goes to carkh in #clojure
(defn- tr
  "Transforms treebank string into series of s-like expressions."
  [ptree & [tag-fn]]
  (let [t (or tag-fn symbol)]
    (if (= :E (first ptree))
      {:tag 
      (t (second (second ptree))) :chunk (map #(tr % tag-fn) (drop 2 ptree))}
      (second ptree))))

(defn make-tree
  "Make a tree from the string output of a treebank-parser."
  [tree-text & [tag-fn]]
  (tr (s-parser tree-text) tag-fn))

