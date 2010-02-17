; Clojure opennlp tools
(ns opennlp
  (:import [java.io File FileNotFoundException])
  (:import [opennlp.maxent DataStream GISModel])
  (:import [opennlp.maxent.io SuffixSensitiveGISModelReader])
  (:import [opennlp.tools.dictionary Dictionary])
  (:import [opennlp.tools.tokenize TokenizerME])
  (:import [opennlp.tools.sentdetect SentenceDetectorME])
  (:import [opennlp.tools.postag POSTaggerME DefaultPOSContextGenerator POSContextGenerator]))

; OpenNLP property for pos-tagging
(def *beam-size* 3)

(defn- file-exist?
  [filename]
  (.exists (File. filename)))


(defn make-sentence-detector
  "Return a function for detecting sentences based on a given model file."
  [modelfile]
  (if (not (file-exist? modelfile))
    (throw (FileNotFoundException. "Model file does not exist."))
    (fn
      [text]
      (let [model     (.getModel (SuffixSensitiveGISModelReader. (File. modelfile)))
            detector  (SentenceDetectorME. model)
            sentences (.sentDetect detector text)]
        sentences))))


(defn make-tokenizer
  "Return a function for tokenizing a sentence based on a given model file."
  [modelfile]
  (if (not (file-exist? modelfile))
    (throw (FileNotFoundException. "Model file does not exist."))
    (fn
      [sentence]
      (let [model     (.getModel (SuffixSensitiveGISModelReader. (File. modelfile)))
            tokenizer (TokenizerME. model)
            tokens    (.tokenize tokenizer sentence)]
        tokens))))


(defn make-pos-tagger
  "Return a function for tagging tokens based on a given model file."
  [modelfile]
  (if (not (file-exist? modelfile))
    (throw (FileNotFoundException. "Model file does not exist."))
    (fn
      [tokens]
      (let [#^POSContextGenerator cg (DefaultPOSContextGenerator. nil)
            model  (.getModel (SuffixSensitiveGISModelReader. (File. modelfile)))
            tagger (POSTaggerME. *beam-size* model cg nil)
            tags   (.tag tagger 1 tokens)]
        (map #(vector %1 %2) tokens (first tags))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(use 'clojure.contrib.pprint)

; Make our functions with the model file. These assume you're running
; from the root project directory.
(def get-sentences (make-sentence-detector "models/EnglishSD.bin.gz"))
(def tokenize (make-tokenizer "models/EnglishTok.bin.gz"))
(def pos-tag (make-pos-tagger "models/tag.bin.gz"))

(pprint (get-sentences "First sentence. Second sentence? Here is another one. And so on and so forth - you get the idea..."))

;opennlp=> (pprint (get-sentences "First sentence. Second sentence? Here is another one. And so on and so forth - you get the idea..."))
;["First sentence. ", "Second sentence? ", "Here is another one. ",
; "And so on and so forth - you get the idea..."]
;nil

(pprint (tokenize "Mr. Smith gave a car to his son on Friday"))

;opennlp=> (pprint (tokenize "Mr. Smith gave a car to his son on Friday"))
;["Mr.", "Smith", "gave", "a", "car", "to", "his", "son", "on",
; "Friday"]
;nil

(pprint (pos-tag (tokenize "Mr. Smith gave a car to his son on Friday.")))

;opennlp=> (pprint (pos-tag (tokenize "Mr. Smith gave a car to his son on Friday.")))
;(["Mr." "NNP"]
; ["Smith" "NNP"]
; ["gave" "VBD"]
; ["a" "DT"]
; ["car" "NN"]
; ["to" "TO"]
; ["his" "PRP$"]
; ["son" "NN"]
; ["on" "IN"]
; ["Friday." "NNP"])
;nil
