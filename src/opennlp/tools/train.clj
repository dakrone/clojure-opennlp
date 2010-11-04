; Clojure opennlp training functions
(ns opennlp.tools.train
  (:use [clojure.contrib.io])
  (:use [clojure.contrub.reflect])
  (:import [opennlp.maxent DataStream GISModel PlainTextByLineDataStream])
  (:import [opennlp.maxent.io SuffixSensitiveGISModelWriter])
  (:import [opennlp.tools.lang.english TokenStream HeadRules])
  (:import [opennlp.tools.dictionary Dictionary])
  (:import [opennlp.tools.tokenize TokenizerME])
  (:import [opennlp.tools.sentdetect SentenceDetectorME SDEventStream])
  (:import [opennlp.tools.namefind NameFinderEventStream
	                           NameSampleDataStream
	                           NameFinderME])
  (:import [opennlp.tools.chunker ChunkerME ChunkerEventStream])
  (:import [opennlp.tools.parser ParserEventTypeEnum])
  (:import [opennlp.tools.parser.chunking Parser ParserEventStream])
  (:import [opennlp.tools.postag POSTaggerME
	                         DefaultPOSContextGenerator
	                         POSEventStream
	                         POSContextGenerator]))

(def *beam-size* 3)

(defn write-model [mod out]
  "Write a model to disk"
  (.persist (new SuffixSensitiveGISModelWriter mod out)))

(def write-treebank-models [models outdir]
  "Write a map of models to a directory"
  (map (fn [[k m]] (write-model
		    m
		    (file outdir (.substring (str k) 1) ".bin.gz")))
       models))
  
(defn train-treebank-chunker
  "Returns a treebank chunker based on given training file"
  ([in] (train-treebank-chunker in 100 5))
  ([in iter cut] (ChunkerME/train
		  (ChunkerEventStream.
		   (PlainTextByLineDataStream.
		    (reader in)))
		  iter cut)))

(defn train-treebank-parser
  "Returns a map of treebank parsers based on set of keys
   dictating which models to build and a set of head rules"
  ([in models headrules] (train-treebank-parser in models headrules 100 5))
  ([in models headrules iter cut]
     (let [make-model (fn [enum]
			(Parser/train (new ParserEventStream
					   (new PlainTextByLineDataStream (reader in))
					   (new HeadRules headrules)
					   enum)
				      iter
				      cut))]
       (conj
	(if (contains? models :build)
	  {:build (make-model (ParserEventTypeEnum/BUILD))})
	(if (contains? models :check)
	  {:check (make-model (ParserEventTypeEnum/CHECK))})
	(if (contains? models :tag)
	  {:tag (make-model (ParserEventTypeEnum/TAG))})
	(if (contains? models :chunk)
	  {:chunk (make-model (ParserEventTypeEnum/CHUNK))})))))
	   
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
