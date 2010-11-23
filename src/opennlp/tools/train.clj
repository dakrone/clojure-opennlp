; Clojure opennlp training functions
(ns opennlp.tools.train
  (:use [clojure.contrib.io])
  (:import [opennlp.tools.util PlainTextByLineStream])
  (:import [opennlp.tools.util.model ModelType])
  (:import [opennlp.tools.dictionary Dictionary])
  (:import [opennlp.tools.tokenize TokenizerME
	                           TokenizerModel
	                           TokenSampleStream])
  (:import [opennlp.tools.sentdetect SentenceDetectorME
	                             SentenceModel
	                             SentenceSampleStream])
  (:import [opennlp.tools.namefind NameFinderEventStream
	                           NameSampleDataStream
	                           NameFinderME
	                           TokenNameFinderModel])
  (:import [opennlp.tools.chunker ChunkerME ChunkSampleStream ChunkerModel])
  (:import [opennlp.tools.parser ParseSampleStream ParserModel])
  (:import [opennlp.tools.parser.lang.en HeadRules])
  (:import [opennlp.tools.parser.chunking Parser])
  (:import [opennlp.tools.postag POSTaggerME
	                         POSModel
	                         POSDictionary
	                         WordTagSampleStream
	                         POSContextGenerator]))

(defn write-model
  "Write a model to disk"
  [model out-stream]
  (.serialize mod (output-stream out-stream)))

(defn build-dictionary
  "Build a Dictionary based on file in appropriate format"
  [in]
  (Dictionary/parseOneEntryPerLine (reader in)))

(defn build-posdictionary
  "Build a POSDictionary based on file in appropriate format

   A POSDictionary records which part-of-speech tags a word
   may be assigned"
  [in]
  (POSDictionary/create (reader in)))

(defn ^ChunkerModel train-treebank-chunker
  "Returns a treebank chunker based on given training file"
  ([in] (train-treebank-chunker "en" in))
  ([lang in] (train-treebank-chunker lang in 100 5))
  ([lang in iter cut] (ChunkerME/train
		  lang
		  (ChunkSampleStream.
		   (PlainTextByLineStream.
		    (reader in)))
		  cut iter)))

(defn ^ParserModel train-treebank-parser
  "Returns a treebank parser based a training file and a set of head rules"
  ([in headrules] (train-treebank-parser "en" in headrules))
  ([lang in headrules] (train-treebank-parser lang in headrules 100 5))
  ([lang in headrules iter cut]
     (Parser/train lang
		   (new ParseSampleStream
			(new PlainTextByLineStream (reader in)))
		   (new HeadRules (reader headrules))
		   iter
		   cut)))
	   
(defn ^TokenNameFinderModel train-name-finder
  "Returns a name finder based on a given training file"
  ([in] (train-name-finder "en" in))
  ([lang in] (train-name-finder lang in 100 5))
  ([lang in iter cut]
     (NameFinderME/train
      lang
      "default"
      (->> (reader in)
	   (new PlainTextByLineStream)
	   (new NameSampleDataStream))
      {}
      iter
      cut)))

(defn ^TokenizerModel train-tokenizer
  "Returns a tokenizer based on given training file"
  ([in] (train-tokenizer "en" in))
  ([lang in] (train-tokenizer lang in 100 5))
  ([lang in iter cut]   
     (TokenizerME/train
      lang
      (->> (input-stream in)
	   (new PlainTextByLineStream)
	   (new TokenSampleStream))
      false
      cut
      iter)))

(defn ^POSModel train-pos-tagger
  "Returns a pos-tagger based on given training file"
  ([in] (train-pos-tagger "en" in))
  ([lang in] (train-pos-tagger lang in nil))
  ([lang in tagdict] (train-pos-tagger lang in tagdict 100 5))
  ([lang in tagdict iter cut]
     (POSTaggerME/train
      lang
      (new WordTagSampleStream (reader in))
      (ModelType/MAXENT)
      tagdict
      nil
      cut
      iter)))
     
(defn ^SentenceModel train-sentence-detector
  "Returns a sentence model based on a given training file"
  ([in] (train-sentence-detector "en" in))
  ([lang in]
     (SentenceDetectorME/train lang
       (->> (reader in)
	    (new PlainTextByLineStream) 
	    (new SentenceSampleStream))
       true
       nil)))
