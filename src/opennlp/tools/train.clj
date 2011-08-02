(ns opennlp.tools.train
  "This namespace contains tools used to train OpenNLP models"
  (:use [clojure.java.io :only [output-stream reader]])
  (:import (opennlp.tools.util PlainTextByLineStream)
           (opennlp.tools.util.model BaseModel ModelType)
           (opennlp.tools.dictionary Dictionary)
           (opennlp.tools.tokenize TokenizerME
                                   TokenizerModel
                                   TokenSampleStream)
           (opennlp.tools.sentdetect SentenceDetectorME
                                     SentenceModel
                                     SentenceSampleStream)
           (opennlp.tools.namefind NameFinderEventStream
                                   NameSampleDataStream
                                   NameFinderME
                                   TokenNameFinderModel)
           (opennlp.tools.chunker ChunkerME ChunkSampleStream ChunkerModel)
           (opennlp.tools.parser ParseSampleStream ParserModel)
           (opennlp.tools.parser.lang.en HeadRules)
           (opennlp.tools.parser.chunking Parser)
           (opennlp.tools.postag POSTaggerME
                                 POSModel
                                 POSDictionary
                                 WordTagSampleStream
                                 POSContextGenerator)))

(defn write-model
  "Write a model to disk"
  [#^BaseModel model out-stream]
  (.serialize model (output-stream out-stream)))

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
     (Parser/train
      lang
      (ParseSampleStream.
       (PlainTextByLineStream.
        (.getChannel (java.io.FileInputStream. in)) "UTF-8"))
      (HeadRules. (reader headrules)) iter cut)))

(defn ^TokenNameFinderModel train-name-finder
  "Returns a name finder based on a given training file"
  ([in] (train-name-finder "en" in))
  ([lang in] (train-name-finder lang in 100 5))
  ([lang in iter cut]
     (NameFinderME/train
      lang
      "default"
      (->> (reader in)
           (PlainTextByLineStream.)
           (NameSampleDataStream.))
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
      (->> (reader in)
           (PlainTextByLineStream.)
           (TokenSampleStream.))
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
      (WordTagSampleStream. (reader in))
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
                                    (PlainTextByLineStream.)
                                    (SentenceSampleStream.))
                               true
                               nil)))
