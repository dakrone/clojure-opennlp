(ns #^{:doc "The main namespace for the clojure-opennlp project. Functions for
  creating NLP performers can be created with the tools in this namespace."
       :author "Lee Hinman"}
  opennlp.nlp
  (:use [clojure.contrib.seq-utils :only [indexed]])
  (:import [java.io File FileNotFoundException FileInputStream])
  (:import [opennlp.tools.util Span])
  (:import [opennlp.tools.tokenize TokenizerModel TokenizerME
            DictionaryDetokenizer DetokenizationDictionary Detokenizer
            Detokenizer$DetokenizationOperation])
  (:import [opennlp.tools.sentdetect SentenceModel SentenceDetectorME])
  (:import [opennlp.tools.namefind TokenNameFinderModel NameFinderME])
  (:import [opennlp.tools.postag POSModel POSTaggerME]))


;; OpenNLP property for pos-tagging. Meant to be rebound before
;; calling the tagging creators
(def #^{:dynamic true} *beam-size* 3)

;; Caching to use for pos-tagging
(def #^{:dynamic true} *cache-size* 1024)

(defn file-exist?
  [filename]
  (.exists (File. filename)))

(defn files-exist?
  [filenames]
  (reduce 'and (map file-exist? filenames)))

(defmulti make-sentence-detector
  "Return a function for splitting sentences given a model file."
  class)

(defmethod make-sentence-detector String
  [modelfile]
  (if-not (file-exist? modelfile)
    (throw (FileNotFoundException. "Model file does not exist."))
    (with-open [model-stream (FileInputStream. modelfile)]
      (make-sentence-detector (SentenceModel. model-stream)))))

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

(defmethod make-tokenizer String
  [modelfile]
  (if-not (file-exist? modelfile)
    (throw (FileNotFoundException. "Model file does not exist."))
    (with-open [model-stream (FileInputStream. modelfile)]
      (make-tokenizer (TokenizerModel. model-stream)))))

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

(defmethod make-pos-tagger String
  [modelfile]
  (if-not (file-exist? modelfile)
    (throw (FileNotFoundException. "Model file does not exist."))
    (with-open [model-stream (FileInputStream. modelfile)]
      (make-pos-tagger (POSModel. model-stream)))))

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

(defmethod make-name-finder String
  [modelfile]
  (if-not (file-exist? modelfile)
    (throw (FileNotFoundException. "Model file does not exist."))
    (with-open [model-stream (FileInputStream. modelfile)]
      (make-name-finder (TokenNameFinderModel. model-stream)))))

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
        (distinct (map #(get tokens (.getStart %)) matches))
        {:probabilities probs}))))

(defmulti make-detokenizer
  "Retun a functin for taking tokens and recombining them into a sentence
  based on a given model file."
  class)

(defmethod make-detokenizer String
  [modelfile]
  (if-not (file-exist? modelfile)
    (throw (FileNotFoundException. "Model file does not exist."))
    (with-open [model-stream (FileInputStream. modelfile)]
      (make-detokenizer (DetokenizationDictionary. model-stream)))))

(defn- collapse-tokens
  [tokens detoken-ops]
  (let [sb (StringBuilder.)]
    (loop [ts tokens dt-ops detoken-ops]
      (println :ts ts)
      (println :dt dt-ops)
      (let [op (first dt-ops)
            op2 (second dt-ops)]
        (println :op op)
        (println :op2 op2)
        (if (and op
                 (or op2
                     (= op2 Detokenizer$DetokenizationOperation/MERGE_TO_LEFT)
                     (= op Detokenizer$DetokenizationOperation/MERGE_TO_RIGHT)))
          (.append sb (first ts))
          (if (> (count dt-ops) 1)
            (.append sb (str (first ts) " "))
            (.append sb (str (first ts)))))
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

