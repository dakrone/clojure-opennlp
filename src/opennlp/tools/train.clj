; Clojure opennlp training functions
(ns opennlp.tools.train
  (:use [clojure.contrib.io])
  (:import [opennlp.maxent DataStream GISModel PlainTextByLineDataStream])
  (:import [opennlp.maxent.io SuffixSensitiveGISModelWriter])
  (:import [opennlp.tools.lang.english TokenStream])
  (:import [opennlp.tools.tokenize TokenizerME])
  (:import [opennlp.tools.sentdetect SentenceDetectorME SDEventStream])
  (:import [opennlp.tools.namefind NameFinderEventStream
	                           NameSampleDataStream
	                           NameFinderME])
  (:import [opennlp.tools.chunker ChunkerME ChunkerEventStream])
  (:import [opennlp.tools.parser.chunking Parser])
  (:import [opennlp.tools.postag POSTaggerME
	                         DefaultPOSContextGenerator
	                         POSEventStream
	                         POSContextGenerator]))

(def *beam-size* 3)

(defn write-model [mod out]
  "Write a model to disk"
  (.persist (new SuffixSensitiveGISModelWriter mod out)))

(defn train-treebank-chunker
  "Returns a treebank chunker based on given training file"
  ([in] (train-treebank-chunker in 100 5))
  ([in iter cut] (ChunkerME/train
		  (ChunkerEventStream.
		   (PlainTextByLineDataStream.
		    (reader in)))
		  iter cut)))

(defn train-treebank-parser)

(defn train-name-finder
  "Returns a name finder based on a given training file"
  ([in] (train-name-finder in 100 5))
  ([in iter cut]
     (NamefinderME/train
      (->> (reader in)
	   (new PlainTextByLineDataStream)
	   (new NameSampleDataStream)
	   (new NameFinderEventStream))
      iter
      cut)))

(defn train-tokenizer
  "Returns a tokenizer based on given training file"
  ([in] (train-tokenizer in 100 5))
  ([in iter cut]   
     (TokenizerME/train (new TokenStream (input-stream in))
			iter
			cut)))

(defn train-pos-tagger
  "Returns a pos-tagger based on given training file"
  ([in] (train-pos-tagger in 100 5))
  ([in iter cut]
     (POSTaggerME/train
      (->> (reader in)
	   (new PlainTextByLineDataStream)
	   (new POSEventStream))
      iter
      cut)))
     
(defn train-sentence-detector
  "Returns a sentence model based on a given training file"
  ([in] (train-sentence-detector in 100 5))
  ([in iter cut] (SentenceDetectorME/train (file in) iter cut nil)))
