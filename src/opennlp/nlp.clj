(ns opennlp.nlp
  "The main namespace for the clojure-opennlp project. Functions for
  creating NLP performers can be created with the tools in this namespace."
  (:use [clojure.java.io :only [input-stream]])
  (:require [opennlp.span :as nspan])
  (:import
   (opennlp.tools.doccat DoccatModel
                         DocumentCategorizerME)
   (opennlp.tools.namefind NameFinderME TokenNameFinderModel)
   (opennlp.tools.postag POSModel POSTaggerME)
   (opennlp.tools.sentdetect SentenceDetectorME SentenceModel)
   (opennlp.tools.tokenize DetokenizationDictionary
                           DetokenizationDictionary$Operation
                           Detokenizer$DetokenizationOperation
                           DictionaryDetokenizer
                           TokenSample
                           TokenizerME
                           TokenizerModel)
   (opennlp.tools.util Span)))

;; OpenNLP property for pos-tagging. Meant to be rebound before
;; calling the tagging creators
(def ^:dynamic *beam-size* (int 3))

;; Caching to use for pos-tagging
(def ^:dynamic *cache-size* (int 1024))

(defn- opennlp-span-strings
  "Takes a collection of spans and the data they refer to. Returns a list of
  substrings corresponding to spans."
  [span-col data]
  (if (seq span-col)
    (if (string? data)
      (seq
        (Span/spansToStrings
          #^"[Lopennlp.tools.util.Span;" (into-array span-col)
          ^String data))
      (seq
        (Span/spansToStrings
          #^"[Lopennlp.tools.util.Span;" (into-array span-col)
          #^"[Ljava.lang.String;" (into-array data))))
    []))

(defn- to-native-span
  "Take an OpenNLP span object and return a pair [i j] where i and j are the
start and end positions of the span."
  [^Span span]
  (nspan/make-span (.getStart span) (.getEnd span) (.getType span)))

(defmulti make-sentence-detector
  "Return a function for splitting sentences given a model file."
  class)

(defmethod make-sentence-detector :default
  [modelfile]
  (with-open [model-stream (input-stream modelfile)]
    (make-sentence-detector (SentenceModel. model-stream))))

(defmethod make-sentence-detector SentenceModel
  [model]
  (fn sentence-detector
    [text]
    {:pre [(string? text)]}
    (let [detector  (SentenceDetectorME. model)
          spans     (.sentPosDetect detector text)
          sentences (opennlp-span-strings spans text)
          probs     (seq (.getSentenceProbabilities detector))]
      (with-meta
        (into [] sentences)
        {:probabilities probs
         :spans         (map to-native-span spans)}))))

(defmulti make-tokenizer
  "Return a function for tokenizing a sentence based on a given model file."
  class)

(defmethod make-tokenizer :default
  [modelfile]
  (with-open [model-stream (input-stream modelfile)]
    (make-tokenizer (TokenizerModel. model-stream))))

(defmethod make-tokenizer TokenizerModel
  [model]
  (fn tokenizer
    [sentence]
    {:pre [(string? sentence)]}
    (let [tokenizer (TokenizerME. model)
          spans     (.tokenizePos tokenizer sentence)
          probs     (seq (.getTokenProbabilities tokenizer))
          tokens    (opennlp-span-strings spans sentence)]
      (with-meta
        (into [] tokens)
        {:probabilities probs
         :spans         (map to-native-span spans)}))))

(defmulti make-pos-tagger
  "Return a function for tagging tokens based on a givel model file."
  class)

(defmethod make-pos-tagger :default
  [modelfile]
  (with-open [model-stream (input-stream modelfile)]
    (make-pos-tagger (POSModel. model-stream))))

(defmethod make-pos-tagger POSModel
  [^POSModel model]
  (fn pos-tagger
    [tokens]
    {:pre [(coll? tokens)]}
    (let [token-array (into-array String tokens)
          tagger (POSTaggerME. model ^int *beam-size* ^int *cache-size*)
          tags (.tag tagger #^"[Ljava.lang.String;" token-array)
          probs (seq (.probs tagger))]
      (with-meta
        (map vector tokens tags)
        {:probabilities probs}))))

(defmulti make-name-finder
  "Return a function for finding names from tokens based on a given
   model file."
  (fn [model & args] (class model)))

(defmethod make-name-finder :default
  [modelfile & args]
  (with-open [model-stream (input-stream modelfile)]
    (make-name-finder (TokenNameFinderModel. model-stream))))

(defmethod make-name-finder TokenNameFinderModel
  [^TokenNameFinderModel model & {:keys [feature-generator beam] :or {beam *beam-size*}}]
  (fn name-finder
    [tokens & contexts]
    {:pre [(seq tokens)
           (every? string? tokens)]}
    (let [finder (NameFinderME.
                   model
                   ^opennlp.tools.util.featuregen.AdaptiveFeatureGenerator feature-generator
                   (int beam))
          a-tokens (into-array String tokens)
          matches (.find finder a-tokens)
          probs (seq (.probs finder))]
      (with-meta
        (distinct (Span/spansToStrings #^"[Lopennlp.tools.util.Span;" matches #^"[Ljava.lang.String;" a-tokens))
        {:probabilities probs
         :spans (map to-native-span matches)}))))


(defmulti make-detokenizer
  "Return a function for taking tokens and recombining them into a sentence
  based on a given model file."
  class)

(defmethod make-detokenizer :default
  [modelfile]
  (with-open [model-stream (input-stream modelfile)]
    (make-detokenizer (DetokenizationDictionary. model-stream))))

;; TODO: clean this up, recursion is a smell
;; TODO: remove debug printlns once I'm satisfied
#_(defn- collapse-tokens
    [tokens detoken-ops]
    (let [sb (StringBuilder.)
          token-set (atom #{})]
      ;;(println :ops detoken-ops)
      (loop [ts tokens dt-ops detoken-ops]
        (let [op (first dt-ops)
              op2 (second dt-ops)]
          ;; (println :op op)
          ;; (println :op2 op)
          ;; (println :ts (first ts))
          ;; (println :sb (.toString sb))
          (cond
           (or (= op2 nil)
               (= op2 Detokenizer$DetokenizationOperation/MERGE_TO_LEFT))
           (.append sb (first ts))

           (or (= op nil)
               (= op Detokenizer$DetokenizationOperation/MERGE_TO_RIGHT))
           (.append sb (first ts))

           (= op DetokenizationDictionary$Operation/RIGHT_LEFT_MATCHING)
           (if (contains? @token-set (first ts))
             (do
               ;; (println :token-set @token-set)
               ;; (println :ts (first ts))
               (swap! token-set disj (first ts))
               (.append sb (first ts)))
             (do
               ;;(println :token-set @token-set)
               ;;(println :ts (first ts))
               (swap! token-set conj (first ts))
               (.append sb (str (first ts) " "))))

           :else
           (.append sb (str (first ts) " ")))
          (when (and op op2)
            (recur (next ts) (next dt-ops)))))
      (str sb)))

;; In the current documentation there is no RIGHT_LEFT_MATCHING and
;; I've never seen such an operation in practice.
;; http://opennlp.apache.org/documentation/apidocs/opennlp-tools/opennlp/tools/tokenize/Detokenizer.DetokenizationOperation.html
(defn- detokenize*
  "Given a sequence of DetokenizationOperations, produce a string."
  [tokens ops]
  (loop [toks        (seq tokens)
         ops         (seq ops)
         result-toks []]
    (if toks
      (let [op    (first ops)
            rtoks (cond
                   (= op Detokenizer$DetokenizationOperation/MERGE_TO_LEFT)
                   (if (not-empty result-toks)
                     (conj (pop result-toks) (first toks) " ")
                     (conj result-toks (first toks) " "))

                   (= op Detokenizer$DetokenizationOperation/MERGE_TO_RIGHT)
                   (conj result-toks (first toks))

                   :else
                   (conj result-toks (first toks) " "))]
        (recur (next toks) (next ops) rtoks))
      (apply str (butlast result-toks)))))

#_(defmethod make-detokenizer DetokenizationDictionary
    [model]
    (fn detokenizer
      [tokens]
      {:pre [(coll? tokens)
             (every? string? tokens)]}
      (let [detoken (DictionaryDetokenizer. model)
            ops     (.detokenize detoken (into-array String tokens))]
        (detokenize* tokens ops))))

(defmethod make-detokenizer DetokenizationDictionary
  [model]
  (fn detokenizer
    [tokens]
    {:pre [(coll? tokens)
           (every? string? tokens)]}
    (-> (DictionaryDetokenizer. model)
        (TokenSample. #^"[Ljava.lang.String;" (into-array String tokens))
        (.getText))))

(defn parse-categories [outcomes-string outcomes]
  "Given a string that represents the opennlp outcomes and an array of
  probability outcomes, zip them into a map of category-probability pairs"
  (zipmap
   (map first (map rest (re-seq #"(\w+)\[.*?\]" outcomes-string)))
   outcomes))

(defmulti make-document-categorizer
  "Return a function for determining a category given a model."
  class)

(defmethod make-document-categorizer :default
  [modelfile]
  (with-open [model-stream (input-stream modelfile)]
    (make-document-categorizer (DoccatModel. model-stream))))

(defmethod make-document-categorizer DoccatModel
  [^DoccatModel model]
  (fn document-categorizer
    [text]
    {:pre [(string? text)]}
    (let [categorizer (DocumentCategorizerME. model)
          outcomes (.categorize categorizer ^String text)]
      (with-meta
        {:best-category (.getBestCategory categorizer outcomes)}
        {:probabilities (parse-categories
                         (.getAllResults categorizer outcomes)
                         outcomes)}))))
