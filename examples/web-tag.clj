(use 'opennlp) ; http://github.com/dakrone/clojure-opennlp
(use 'clojure.contrib.pprint)
(use 'clojure.contrib.duck-streams)
(use 'clojure.contrib.seq-utils)

(defn strip-html-tags
  "Messily strip html tags from a web page"
  [string]
  (.replaceAll
    (.replaceAll
      (.replaceAll
        (.replaceAll string "<script .*?>.*?</script>" " ")
      "<style .*?>.*?</style>" " ")
      "<.*?>" " ")
    "[ ]+" " "))

(defn fetch-page
  [url]
  (let [html (.replaceAll (slurp* url) "[\t\n\r]" " ")]
    (re-find #"<body.*?</body>" html)))

(defn fetch-plain-page
  [url]
  (strip-html-tags (fetch-page url)))

(def get-sentences (make-sentence-detector "models/EnglishSD.bin.gz"))
(def tokenize (make-tokenizer "models/EnglishTok.bin.gz"))
(def pos-tag (make-pos-tagger "models/tag.bin.gz"))

(defn- tag-sentences
  [sent-seq]
  (map #(pos-tag (tokenize %)) sent-seq))

(defn tag-page
  [url]
  (let [page      (fetch-plain-page url)
        sentences (get-sentences page)
        sent-seq  (partition-all 10 sentences)]
    (pmap tag-sentences sent-seq)))

(tag-page "http://writequit.org")
