(ns opennlp.tools.train
  "This namespace contains tools used to train OpenNLP models"
  (:use [clojure.java.io :only [output-stream reader input-stream]])
  (:import (opennlp.tools.util PlainTextByLineStream TrainingParameters)
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
                                 POSContextGenerator)
           (opennlp.tools.doccat DoccatModel
                                 DocumentCategorizerME
                                 DocumentSampleStream)))

(defn write-model
  "Write a model to disk"
  [^BaseModel model out-stream]
  (with-open [out (output-stream out-stream)]
    (.serialize model out)))

(defn build-dictionary
  "Build a Dictionary based on file in appropriate format"
  [in]
  (with-open [rdr (reader in)]
    (Dictionary/parseOneEntryPerLine rdr)))

(defn build-posdictionary
  "Build a POSDictionary based on file in appropriate format

   A POSDictionary records which part-of-speech tags a word
   may be assigned"
  [in]
  (with-open [is (input-stream in)]
    (POSDictionary/create is)))

(defn ^ChunkerModel train-treebank-chunker
  "Returns a treebank chunker based on given training file"
  ([in] (train-treebank-chunker "en" in))
  ([lang in] (train-treebank-chunker lang in 100 5))
  ([lang in iter cut]
     (with-open [rdr (reader in)]
       (ChunkerME/train
        lang
        (ChunkSampleStream.
         (PlainTextByLineStream. rdr))
        cut iter))))

(defn ^ParserModel train-treebank-parser
  "Returns a treebank parser based a training file and a set of head rules"
  ([in headrules] (train-treebank-parser "en" in headrules))
  ([lang in headrules] (train-treebank-parser lang in headrules 100 5))
  ([lang in headrules iter cut]
     (with-open [rdr (reader headrules)
                 fis (java.io.FileInputStream. in)]
       (Parser/train
        lang
        (ParseSampleStream.
         (PlainTextByLineStream.
          (.getChannel fis) "UTF-8"))
        (HeadRules. rdr) iter cut))))


(defn ^TokenNameFinderModel train-name-finder
  "Returns a trained name finder based on a given training file. Uses a
  non-deprecated train() method that allows for perceptron training with minimum
  modification. Optional arguments include the type of entity (e.g \"person\"),
  custom feature generation and a knob for switching to perceptron training
  (maXent is the default). For perceptron prefer cutoff 0, whereas for
  maXent 5."
  ([in] (train-name-finder "en" in))
  ([lang in] (train-name-finder lang in 100 5))
  ([lang in iter cut & {:keys [entity-type feature-gen classifier]
                        ;;MUST be either "MAXENT" or "PERCEPTRON"
                        :or  {entity-type "default" classifier "MAXENT"}}]
     (with-open [rdr (reader in)]
       (NameFinderME/train
        lang
        entity-type
        (->> rdr
             (PlainTextByLineStream.)
             (NameSampleDataStream.))
        (doto (TrainingParameters.)
          (.put TrainingParameters/ALGORITHM_PARAM classifier)
          (.put TrainingParameters/ITERATIONS_PARAM (Integer/toString iter))
          (.put TrainingParameters/CUTOFF_PARAM     (Integer/toString cut)))
        feature-gen {}))))

(defn ^TokenizerModel train-tokenizer
  "Returns a tokenizer based on given training file"
  ([in] (train-tokenizer "en" in))
  ([lang in] (train-tokenizer lang in 100 5))
  ([lang in iter cut]
     (with-open [rdr (reader in)]
       (TokenizerME/train
        lang
        (->> rdr
             (PlainTextByLineStream.)
             (TokenSampleStream.))
        false
        cut
        iter))))

(defn ^POSModel train-pos-tagger
  "Returns a pos-tagger based on given training file"
  ([in] (train-pos-tagger "en" in))
  ([lang in] (train-pos-tagger lang in nil))
  ([lang in tagdict] (train-pos-tagger lang in tagdict 100 5))
  ([lang in tagdict iter cut]
     (with-open [rdr (reader in)]
       (POSTaggerME/train
        lang
        (WordTagSampleStream. rdr)
        (ModelType/MAXENT)
        tagdict
        nil
        cut
        iter))))

(defn ^SentenceModel train-sentence-detector
  "Returns a sentence model based on a given training file"
  ([in] (train-sentence-detector "en" in))
  ([lang in]
     (with-open [rdr (reader in)]
       (SentenceDetectorME/train lang
                                 (->> rdr
                                      (PlainTextByLineStream.)
                                      (SentenceSampleStream.))
                                 true
                                 nil))))

(defn ^DoccatModel train-document-categorization
  "Returns a classification model based on a given training file"
  ([in] (train-document-categorization "en" in 1 100))
  ([lang in] (train-document-categorization lang in 1 100))
  ([lang in cutoff] (train-document-categorization lang in cutoff 100))
  ([lang in cutoff iterations]
     (with-open [rdr (reader in)]
       (DocumentCategorizerME/train lang
                                    (->> rdr
                                         (PlainTextByLineStream.)
                                         (DocumentSampleStream.))
                                    cutoff iterations))))
