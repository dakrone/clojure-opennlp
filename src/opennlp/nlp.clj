; Clojure opennlp tools
(ns opennlp.nlp
  (:use [clojure.contrib.seq-utils :only [indexed]])
  (:import [java.io File FileNotFoundException])
  (:import [opennlp.maxent DataStream GISModel])
  (:import [opennlp.maxent.io PooledGISModelReader SuffixSensitiveGISModelReader])
  (:import [opennlp.tools.util Span])
  (:import [opennlp.tools.dictionary Dictionary])
  (:import [opennlp.tools.tokenize TokenizerME])
  (:import [opennlp.tools.sentdetect SentenceDetectorME])
  (:import [opennlp.tools.namefind NameFinderME])
  (:import [opennlp.tools.chunker ChunkerME])
  (:import [opennlp.tools.coref LinkerMode])
  (:import [opennlp.tools.coref.mention Mention DefaultParse])
  (:import [opennlp.tools.lang.english ParserTagger ParserChunker HeadRules TreebankLinker CorefParse])
  (:import [opennlp.tools.parser.chunking Parser])
  (:import [opennlp.tools.parser AbstractBottomUpParser Parse])
  (:import [opennlp.tools.postag POSTaggerME DefaultPOSContextGenerator POSContextGenerator]))

; These are in core in clojure 1.2, so only require them if below 2
(if (< (:minor *clojure-version*) 2)
  (use '[clojure.contrib.seq-utils :only [partition-by flatten]])
  nil)


; OpenNLP property for pos-tagging
(def *beam-size* 3)

(defn- file-exist?
  [filename]
  (.exists (File. filename)))


(defn files-exist?
  [filenames]
  (reduce 'and (map file-exist? filenames)))


(defn make-sentence-detector
  "Return a function for detecting sentences based on a given model file."
  [modelfile]
  (if-not (file-exist? modelfile)
    (throw (FileNotFoundException. "Model file does not exist."))
    (fn sentenizer
      [text]
      (let [model     (.getModel (SuffixSensitiveGISModelReader. (File. modelfile)))
            detector  (SentenceDetectorME. model)
            sentences (.sentDetect detector text)]
        (into [] sentences)))))


(defn make-tokenizer
  "Return a function for tokenizing a sentence based on a given model file."
  [modelfile]
  (if-not (file-exist? modelfile)
    (throw (FileNotFoundException. "Model file does not exist."))
    (fn tokenizer
      [sentence]
      (let [model     (.getModel (SuffixSensitiveGISModelReader. (File. modelfile)))
            tokenizer (TokenizerME. model)
            tokens    (.tokenize tokenizer sentence)]
        (into [] tokens)))))


(defn make-pos-tagger
  "Return a function for tagging tokens based on a given model file."
  [modelfile]
  (if-not (file-exist? modelfile)
    (throw (FileNotFoundException. "Model file does not exist."))
    (fn pos-tagger
      [tokens]
      (let [token-array (if (vector? tokens) (into-array tokens) tokens)
            #^POSContextGenerator cg (DefaultPOSContextGenerator. nil)
            model  (.getModel (SuffixSensitiveGISModelReader. (File. modelfile)))
            tagger (POSTaggerME. *beam-size* model cg nil)
            tags   (.tag tagger 1 token-array)]
        (map #(vector %1 %2) tokens (first tags))))))


(defn make-name-finder
  "Return a function for finding names from tokens based on given model file(s)."
  [& modelfiles]
  (if-not (files-exist? modelfiles)
    (throw (FileNotFoundException. "Not all model files exist."))
    (fn name-finder
      [tokens]
      (distinct
        (flatten
          (for [modelfile modelfiles]
            (let [token-array (if (vector? tokens) (into-array tokens) tokens)
                  model   (.getModel (PooledGISModelReader. (File. modelfile)))
                  finder  (NameFinderME. model)
                  matches (.find finder token-array)]
              (map #(nth tokens (.getStart %)) matches))))))))


(defn- split-chunks
  "Partition a sequence of treebank chunks by their phrases."
  [chunks]
  (let [seqnum    (atom 0)
        splitfunc (fn
                    [item]
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


; Thanks chouser
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

(defn make-treebank-chunker
  "Return a function for chunking phrases from pos-tagged tokens based on
  a given model file."
  [modelfile]
  (if-not (file-exist? modelfile)
    (throw (FileNotFoundException. "Model file does not exist."))
    (fn treebank-chunker
      [pos-tagged-tokens]
      (let [model         (.getModel (SuffixSensitiveGISModelReader. (File. modelfile)))
            chunker       (ChunkerME. model)
            [tokens tags] (de-interleave pos-tagged-tokens)
            chunks        (into [] (seq (.chunk chunker tokens tags)))
            sized-chunks  (map size-chunk (split-chunks chunks))
            [types sizes] (de-interleave sized-chunks)
            token-chunks  (split-with-size sizes tokens)]
        (map #(struct treebank-phrase (into [] (last %)) (first %))
             (partition 2 (interleave types token-chunks)))))))


; Docs for treebank chunking:

;(def chunker (make-treebank-chunker "models/EnglishChunk.bin.gz"))
;(pprint (chunker (pos-tag (tokenize "The override system is meant to deactivate the accelerator when the brake pedal is pressed."))))

;(map size-chunk (split-chunks (chunker (pos-tag (tokenize "The override system is meant to deactivate the accelerator when the brake pedal is pressed.")))))

;opennlp.nlp=> (split-with-size (sizes (map size-chunk (split-chunks (chunker (pos-tag (tokenize "The override system is meant to deactivate the accelerator when the brake pedal is pressed.")))))) (tokenize "The override system is meant to deactivate the accelerator when the brake pedal is pressed."))
;(("The" "override" "system") ("is" "meant" "to" "deactivate") ("the" "accelerator") ("when") ("the" "brake" "pedal") ("is" "pressed") ("."))

;(["NP" 3] ["VP" 4] ["NP" 2] ["ADVP" 1] ["NP" 3] ["VP" 2])

;opennlp.nlp=> (pprint (chunker (pos-tag (tokenize "The override system is meant to deactivate the accelerator when the brake pedal is pressed."))))
;#<ArrayList [B-NP, I-NP, I-NP, B-VP, I-VP, I-VP, I-VP, B-NP, I-NP, B-ADVP, B-NP, I-NP, I-NP, B-VP, I-VP, O]>

; So, B-* starts a sequence, I-* continues it. New phrase starts when B-* is encountered
; -------------------------------------------

; Treebank parsing

; Default advance percentage as defined by AbstractBottomUpParser.defaultAdvancePercentage
(def *advance-percentage* 0.95)


(defn- strip-parens
  "Treebank-parser does not like parens and braces, so replace them."
  [s]
  (-> s
    (.replaceAll "\\(" "-LRB-")
    (.replaceAll "\\)" "-RRB-")
    (.replaceAll "\\{" "-LCB-")
    (.replaceAll "\\}" "-RCB-")))


(defn- parse-line
  "Given a line and Parser object, return a list of Parses."
  [line parser]
  (let [line (strip-parens line)
        results (StringBuffer.)
        words (.split line " ")
        p (Parse. line (Span. 0 (count line)) AbstractBottomUpParser/INC_NODE (double 1) (int 0))]
    (loop [parse-index 0 start-index 0]
      (if (> (+ parse-index 1) (count words))
        nil
        (let [token (get words parse-index)]
          ;(println "inserting " token " at " i " pidx " parse-index " sidx " start-index)
          ; Mutable state, but contained only in the parse-line function
          (.insert p (Parse. line
                             (Span. start-index (+ start-index (count token)))
                             AbstractBottomUpParser/TOK_NODE
                             (double 0)
                             (int parse-index)))
          (recur (inc parse-index) (+ 1 start-index (count token))))))
    (.show (.parse parser p) results)
    (.toString results)))


(defn make-treebank-parser
  "Return a function for treebank parsing a sequence of sentences, based on
  given build, check, tag, chunk models and a set of head rules."
  [buildmodel checkmodel tagmodel chunkmodel headrules & opts]
  (if-not (files-exist? [buildmodel checkmodel tagmodel chunkmodel headrules])
    (throw (FileNotFoundException. "One or more of the model or rule files does not exist"))
    (fn treebank-parser
      [text]
      (let [builder (-> (File. buildmodel) SuffixSensitiveGISModelReader. .getModel)
            checker (-> (File. checkmodel) SuffixSensitiveGISModelReader. .getModel)
            parsetagger (if (and (:tagdict opts) (file-exist? (:tagdict opts)))
                          (if (:case-sensitive opts)
                            (ParserTagger. tagmodel (:tagdict opts) true)
                            (ParserTagger. tagmodel (:tagdict opts) false))
                          (ParserTagger. tagmodel nil))
            parsechunker (ParserChunker. chunkmodel)
            headrules (HeadRules. headrules)
            parser (Parser. builder
                            checker
                            parsetagger
                            parsechunker
                            headrules
                            (int *beam-size*)
                            (double *advance-percentage*))
            parses (map #(parse-line % parser) text)]
        (vec parses)))))


(defn- strip-funny-chars
  "Strip out some characters that might cause trouble parsing the tree."
  [s]
  (-> s
    (.replaceAll "'" "-SQUOTE-")
    (.replaceAll "\"" "-DQUOTE-")
    (.replaceAll "~" "-TILDE-")
    (.replaceAll "`" "-BACKTICK-")
    (.replaceAll "," "-COMMA-")
    (.replaceAll "\\\\" "-BSLASH-")
    (.replaceAll "\\/" "-FSLASH-")
    (.replaceAll "\\^" "-CARROT-")
    (.replaceAll "@" "-ATSIGN-")
    (.replaceAll "#" "-HASH-")))


; Credit for this function goes to carkh in #clojure
(defn- tr
  "Generate a tree from the string output of a treebank-parser."
  [to-parse]
  (cond (symbol? to-parse) (str to-parse)
        (seq to-parse) (let [[tag & body] to-parse]
                         `{:tag ~tag :chunk ~(if (> (count body) 1)
                                               (map tr body)
                                               (tr (first body)))})))


(defn make-tree
  "Make a tree from the string output of a treebank-parser."
  [tree-text]
  (let [text (strip-funny-chars tree-text)]
    (tr (read-string text))))



;------------------------------------------------------------------------
;------------------------------------------------------------------------
; Treebank Linking
; WIP, do not use yet.

(declare print-parse)

(defn print-child
  "Given a child, parent and start, print out the child parse."
  [c p start]
  (let [s (.getSpan c)]
    (if (< @start (.getStart s))
      (print (.substring (.getText p) start (.getStart s))))
    (print-parse c)
    (reset! start (.getEnd s))))


(defn print-parse
  "Given a parse and the EntityMentions-to-parse map, print out the parse."
  [p parse-map]
  (let [start (atom (.getStart (.getSpan p)))
        children (.getChildren p)]
    (if-not (= Parser/TOK_NODE (.getType p))
      (do
        (print (str "(" (.getType p)))
        (if (contains? parse-map p)
          (print (str "#" (get parse-map p))))
        (print " ")))
    (map #(print-child % p start) children)
    (print (.substring (.getText p) @start (.getEnd (.getSpan p)))) ; FIXME: don't use substring
    (if-not (= Parser/TOK_NODE (.getType p))
      (print ")"))))


(defn add-mention!
  "Add a single mention to the parse-map with index."
  [mention index parse-map]
  (let [mention-parse (.getParse (.getParse mention))]
    (swap! parse-map assoc mention-parse (+ index 1))))


(defn add-mentions!
  "Add mentions to the parse map."
  [entity index parse-map]
  (map #(add-mention! % index parse-map) (.getMentions entity)))


(defn add-entities
  "Given a list of entities, return a map of parses to entities."
  [entities]
  (let [parse-map (atom {})
        i-entities (indexed entities)]
    (map #(add-mentions! (second %) (first %) parse-map) entities)
    @parse-map))


; This is intended to actually be called.
(defn show-parses
  "Given a list of parses and entities, print them out."
  [parses entities]
  (let [parse-map (add-entities entities)]
    (map #(print-parse % parse-map) parses)))


(defn coref-extent
  [extent p index]
  (if (nil? extent)
    (let [snp (Parse. (.getText p) (.getSpan extent) "NML" 1.0 0)]
      (.insert p snp) ; FIXME
      (.setParse extent (DefaultParse. snp index)))
    nil))


(defn coref-sentence
  [sentence parses index tblinker]
  (let [p (Parse/parseParse sentence)
        extents (.getMentions (.getMentionFinder tblinker) (DefaultParse. p index))]
    (swap! parses #(assoc % (count %) p))
    (map #(coref-extent % p index) extents)
    extents))


; Second Attempt
(defn make-treebank-linker
  "Make a TreebankLinker, given a model directory."
  [modeldir]
  (let [tblinker (TreebankLinker. modeldir LinkerMode/TEST)]
    (fn treebank-linker
      [sentences]
      (let [parses (atom [])
            indexed-sentences (indexed sentences)
            extents (doall (map #(coref-sentence (second %) parses (first %) tblinker) indexed-sentences))]
        (println extents (str (count (first extents))))
        (println @parses)
        (let [mention-array (into-array Mention (first extents))
              foo (println "mentions:" mention-array)
              entities (.getEntities tblinker mention-array)]
          (println "mentions:" mention-array)
          (println "entities:" entities)
          (show-parses @parses entities))))))


(comment

  (def tbl (make-treebank-linker "coref"))
  (def treebank-parser (make-treebank-parser "parser-models/build.bin.gz" "parser-models/check.bin.gz" "parser-models/tag.bin.gz" "parser-models/chunk.bin.gz" "parser-models/head_rules"))
  (def s (treebank-parser ["This is a sentence ."]))
  (tbl s)

)

; First (dumb) Attempt
#_(defn make-treebank-linker
  [modeldir]
  (let [tblinker (TreebankLinker. modeldir LinkerMode/TEST)
        document (ArrayList.) ; do this without an arraylist
        parses   (ArrayList.)
        sentencenum (atom 0)]
    (fn treebank-linker
      [sentences]
      (for [sentence sentences]
        (let [p (Parse/parseParse sentence)
              extents (.getMentions (.getMentionFinder tblinker) (DefaultParse. p sentencenum))]
          (.add parses p)
          (for [extent extents]
            (if (nil? extent)
              (let [snp (Parse. (.getText p) (.getSpan extent) "NML" 1.0 0)]
                (.insert p snp)
                (.setParse extent (DefaultParse. snp sentencenum)))
              nil))
          (.addAll document (Arrays/asList extents))
          (swap! sentencenum inc))))))


;testing
(comment

(use 'opennlp.nlp)

(def treebank-parser (make-treebank-parser "parser-models/build.bin.gz" "parser-models/check.bin.gz" "parser-models/tag.bin.gz" "parser-models/chunk.bin.gz" "parser-models/head_rules"))

; String output
(first (treebank-parser ["This is a sentence ."]))
; => "(TOP (S (NP (DT This)) (VP (VBZ is) (NP (DT a) (NN sentence))) (. .)))"

; Tree output
(make-tree (first (treebank-parser ["This is a sentence ."])))
; => {:chunk {:chunk ({:chunk {:chunk "This", :tag DT}, :tag NP} {:chunk ({:chunk "is", :tag VBZ} {:chunk ({:chunk "a", :tag DT} {:chunk "sentence", :tag NN}), :tag NP}), :tag VP} {:chunk ".", :tag .}), :tag S}, :tag TOP} 

)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(comment

(use 'clojure.contrib.pprint)

; Make our functions with the model file. These assume you're running
; from the root project directory.
(def get-sentences (make-sentence-detector "models/EnglishSD.bin.gz"))
(def tokenize (make-tokenizer "models/EnglishTok.bin.gz"))
(def pos-tag (make-pos-tagger "models/tag.bin.gz"))
(def name-find (make-name-finder "models/namefind/person.bin.gz" "models/namefind/organization.bin.gz"))
(def chunker (make-treebank-chunker "models/EnglishChunk.bin.gz"))

(pprint (get-sentences "First sentence. Second sentence? Here is another one. And so on and so forth - you get the idea..."))

;opennlp.nlp=> (pprint (get-sentences "First sentence. Second sentence? Here is another one. And so on and so forth - you get the idea..."))
;["First sentence. ", "Second sentence? ", "Here is another one. ",
; "And so on and so forth - you get the idea..."]
;nil

(pprint (tokenize "Mr. Smith gave a car to his son on Friday"))

;opennlp.nlp=> (pprint (tokenize "Mr. Smith gave a car to his son on Friday"))
;["Mr.", "Smith", "gave", "a", "car", "to", "his", "son", "on",
; "Friday"]
;nil

(pprint (pos-tag (tokenize "Mr. Smith gave a car to his son on Friday.")))

;opennlp.nlp=> (pprint (pos-tag (tokenize "Mr. Smith gave a car to his son on Friday.")))
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
 
(name-find (tokenize "My name is Lee, not John."))

;opennlp.nlp=> (name-find (tokenize "My name is Lee, not John."))
;("Lee" "John")


)

