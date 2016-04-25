(ns ^{:doc "Namespace containing tools pertaining to the treebank NLP tools.
             This includes treebank chuncking, parsing and linking (coref)."
       :author "Lee Hinman"}
  opennlp.treebank
  (:use [opennlp.nlp :only [*beam-size*]]
        [clojure.java.io :only [input-stream]])
  (:require [clojure.string :as str]
            [instaparse.core :as insta])
  (:import (java.util List)
           (opennlp.tools.chunker ChunkerModel ChunkerME)
           (opennlp.tools.cmdline.parser ParserTool)
           (opennlp.tools.parser Parse ParserModel
                                 ParserFactory AbstractBottomUpParser)
           (opennlp.tools.parser.chunking Parser)
           (opennlp.tools.coref.mention Mention DefaultParse)
           (opennlp.tools.coref LinkerMode DefaultLinker)
           (opennlp.tools.util Span)))

;; Default advance percentage as defined by
;; AbstractBottomUpParser.defaultAdvancePercentage
(def ^:dynamic *advance-percentage* 0.95)

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
    (let [chunker (ChunkerME. model (int *beam-size*))
          [tokens tags] (de-interleave pos-tagged-tokens)
          chunks  (into [] (seq (.chunk chunker ^List tokens ^List tags)))
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

;;------------------------------------------------------------------------
;;------------------------------------------------------------------------
;; Treebank Linking
;; WIP, do not use yet.

(declare print-parse)

(defn print-child
  "Given a child, parent and start, print out the child parse."
  [^Parse c ^Parse p start]
  (let [s (.getSpan c)]
    (if (< @start (.getStart s))
      (print (subs (.getText p) start (.getStart s))))
    (print-parse c)
    (reset! start (.getEnd s))))

;; This is broken, don't use this.
(defn print-parse
  "Given a parse and the EntityMentions-to-parse map, print out the parse."
  [^Parse p parse-map]
  (let [start (atom (.getStart ^Span (.getSpan p)))
        children (.getChildren p)]
    (if-not (= Parser/TOK_NODE (.getType p))
      (do
        (print (str "(" (.getType p)))
        (if (contains? parse-map p)
          (print (str "#" (get parse-map p))))
        (print " ")))
    (map #(print-child % p start) children)
    ;; FIXME: don't use substring
    (print (subs (.getText p) @start (.getEnd (.getSpan p))))
    (if-not (= Parser/TOK_NODE (.getType p))
      (print ")"))))


(defn add-mention!
  "Add a single mention to the parse-map with index."
  [^Mention mention index parse-map]
  (let [mention-parse (.getParse ^DefaultParse (.getParse mention))]
    (swap! parse-map assoc mention-parse (+ index 1))))


(defn add-mentions!
  "Add mentions to the parse map."
  [entity index parse-map]
  (dorun (map #(add-mention! % index parse-map)
              (iterator-seq (.getMentions entity)))))


(defn add-entities
  "Given a list of entities, return a map of parses to entities."
  [entities]
  (let [parse-map (atom {})
        i-entities (map vector (iterate inc 0) entities)]
    (dorun (map (fn [[index entity]] (add-mentions! entity index parse-map))
                i-entities))
    @parse-map))


;; This is intended to actually be called.
(defn show-parses
  "Given a list of parses and entities, print them out."
  [parse entities]
  (let [parse-map (add-entities entities)]
    (println "parse-map:" parse-map)
    (println "parse:" parse)
    (print-parse parse parse-map)
    parse-map))


(defn coref-extent
  [^Mention extent ^Parse p index]
  (if (nil? extent)
    (let [snp (Parse. (.getText p) (.getSpan extent) "NML" (double 1.0) (int 0))]
      (.insert p snp) ; FIXME
      (.setParse extent (DefaultParse. snp index)))
    nil))


(defn coref-sentence
  [^String sentence parses index ^DefaultLinker tblinker]
  (let [^Parse p (Parse/parseParse sentence)
        extents (.getMentions (.getMentionFinder tblinker)
                              (DefaultParse. p index))]
    (swap! parses #(assoc % (count %) p))
    (map #(coref-extent % p index) extents)
    ;;(println :es (map #(println (bean %)) extents))
    (map bean extents)))

;; TODO: fix this function, currently doesn't parse correctly
(defn parse-extent
  "Given an coref extent, a treebank linker, a parses atom and the index of
  the extent, return a tuple of the coresponding parse and discourse entities"
  [extent ^DefaultLinker tblinker parses pindex]
  (println :ext (bean extent))
  (let [e (filter #(not (nil? (:parse (bean %)))) extent)
        ;;_ (println :e e)
        mention-array (into-array Mention e)
        entities (.getEntities tblinker #^"[Lopennlp.tools.coref.mention.Mention;" mention-array)]
    (println :entities (seq entities) (bean (first entities)))
    [(get @parses pindex) (seq entities)]))

;; Second Attempt
(defn make-treebank-linker
  "Make a TreebankLinker, given a model directory."
  [modeldir]
  (let [tblinker (DefaultLinker. modeldir LinkerMode/TEST)]
    (fn treebank-linker
      [sentences]
      (let [parses (atom [])
            indexed-sentences (map vector (iterate inc 0) sentences)
            extents (doall (map #(coref-sentence (second %) parses
                                                 (first %) tblinker)
                                indexed-sentences))
            i-extents (map vector (iterate inc 0) extents)]
        #_(map #(parse-extent %1 tblinker parses %2) i-extents)
        (doall (map println extents))
        extents))))

;; this is used for the treebank linking, it is a system property for
;; the location of the wordnet installation 'dict' directory
;; see: http://wordnet.princeton.edu/wordnet/
(defn set-wordnet-location!
  "Set the location of the WordNet 'dict' directory"
  [location]
  (System/setProperty "WNSEARCHDIR" location))

;;  What I really need is a good way to express this in Clojure's datastructures.
(comment
  (def tbl (make-treebank-linker "coref"))
  (def treebank-parser
    (make-treebank-parser "parser-model/en-parser-chunking.bin"))
  (def s (treebank-parser ["Mary said she liked me ."]))
  (tbl s)
  )
