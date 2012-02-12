(ns opennlp.nlp
  "The main namespace for the clojure-opennlp project. Functions for
  creating NLP performers can be created with the tools in this namespace."
  (:use [clojure.java.io :only [input-stream]])
  (:import
   (opennlp.tools.namefind NameFinderME TokenNameFinderModel)
   (opennlp.tools.postag POSModel POSTaggerME)
   (opennlp.tools.sentdetect SentenceDetectorME SentenceModel)
   (opennlp.tools.tokenize DetokenizationDictionary
                           DetokenizationDictionary$Operation
                           Detokenizer$DetokenizationOperation
                           DictionaryDetokenizer
                           TokenizerME
                           TokenizerModel)
   (opennlp.tools.util Span)))

;; OpenNLP property for pos-tagging. Meant to be rebound before
;; calling the tagging creators
(def #^{:dynamic true} *beam-size* 3)

;; Caching to use for pos-tagging
(def #^{:dynamic true} *cache-size* 1024)

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
    (let [detector (SentenceDetectorME. model)
          sentences (.sentDetect detector text)
          probs (seq (.getSentenceProbabilities detector))]
      (with-meta
        (into [] sentences)
        {:probabilities probs}))))

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
          tokens (.tokenize tokenizer sentence)
          spans (map #(hash-map :start (.getStart %)
                                :end (.getEnd %))
                     (seq (.tokenizePos tokenizer sentence)))
          probs (seq (.getTokenProbabilities tokenizer))]
      (with-meta
        (into [] tokens)
        {:probabilities probs
         :spans spans}))))

(defmulti make-pos-tagger
  "Return a function for tagging tokens based on a givel model file."
  class)

(defmethod make-pos-tagger :default
  [modelfile]
  (with-open [model-stream (input-stream modelfile)]
    (make-pos-tagger (POSModel. model-stream))))

(defmethod make-pos-tagger POSModel
  [model]
  (fn pos-tagger
    [tokens]
    {:pre [(vector? tokens)]}
    (let [token-array (into-array tokens)
          tagger (POSTaggerME. model *beam-size* *cache-size*)
          tags (.tag tagger token-array)
          probs (seq (.probs tagger))]
      (with-meta
        (map vector tokens tags)
        {:probabilities probs}))))

(defmulti make-name-finder
  "Return a function for finding names from tokens based on a given
   model file."
  class)

(defmethod make-name-finder :default
  [modelfile]
  (with-open [model-stream (input-stream modelfile)]
    (make-name-finder (TokenNameFinderModel. model-stream))))

(defmethod make-name-finder TokenNameFinderModel
  [model]
  (fn name-finder
    [tokens & contexts]
    {:pre [(seq tokens)
           (every? #(= (class %) String) tokens)]}
    (let [finder (NameFinderME. model)
          matches (.find finder (into-array String tokens))
          probs (seq (.probs finder))]
      (with-meta
        (distinct (Span/spansToStrings matches (into-array String tokens)))
        {:probabilities probs}))))

(defmulti make-detokenizer
  "Retun a functin for taking tokens and recombining them into a sentence
  based on a given model file."
  class)

(defmethod make-detokenizer :default
  [modelfile]
  (with-open [model-stream (input-stream modelfile)]
    (make-detokenizer (DetokenizationDictionary. model-stream))))

;; TODO: clean this up, recursion is a smell
;; TODO: remove debug printlns once I'm satisfied
(defn- collapse-tokens
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
    (.toString sb)))

(defmethod make-detokenizer DetokenizationDictionary
  [model]
  (fn detokenizer
    [tokens]
    {:pre [(seq tokens)
           (every? #(= (class %) String) tokens)]}
    (let [detoken (DictionaryDetokenizer. model)
          ops (.detokenize detoken (into-array String tokens))]
      (collapse-tokens tokens ops))))

