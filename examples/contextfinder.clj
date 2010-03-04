; This example is very crazy and may be broken right now. Attempt to
; use at your own risk. Eventually it will be documented.

(ns contextfinder
  (:use [opennlp.nlp])
  (:use [opennlp.tools.filters])
  (:use [clojure.contrib.seq-utils])
  (:use [clojure.contrib.pprint])
  (:use [clojure.contrib.math]))


(def get-sentences (make-sentence-detector "models/EnglishSD.bin.gz"))
(def tokenize (make-tokenizer "models/EnglishTok.bin.gz"))
(def pos-tag (make-pos-tagger "models/tag.bin.gz"))
(def name-find (make-name-finder "models/namefind/person.bin.gz" "models/namefind/organization.bin.gz"))


(defn- mindist
  "Give the minimum distance from the first arg to any value in the second arg."
  [n ns]
  (apply min (map #(abs (- n %)) ns)))


(defn- score-word
  "Score the index-term based on it's distance from any intex-term in the given
  search list. Use the base score to boost matches from 1 up to another value."
  [iword iterms base]
  (let [dist (mindist (first iword) iterms)
        score (if (zero? dist)
                base
                (/ base (* 2 dist)))]
    (if (> dist 2) 0 score)))


(defn score-words
  "Score a list of words linearly based on how far they are from the
  term.  Base score is optional and is 1 by default.  Case sensitive."
  ([term words] (score-words term words 1))
  ([term words base]
   (let [iwords (indexed words)
         iterms (map first (filter (fn [e] (= (second e) term)) iwords))]
     (if (= 0 (count iterms))
       (map #(vector % 0) words)
       (map #(vector (second %) (score-word % iterms base)) iwords)))))

;(into {} (filter #(not= 0 (second %)) (score-words "truck" ["bobby" "fire" "truck" "city" "truck" "state" "colorado"])))

;opennlp.tools.filters=> words
;["bobby" "fire" "truck" "city" "department" "state" "colorado"]
;opennlp.tools.filters=> (score-words "truck" words)
;(["bobby" 1/3] ["fire" 1/2] ["truck" 1] ["city" 1/2] ["department" 1/3] ["state" 0] ["colorado" 0])
;opennlp.tools.filters=> (score-words "truck" words 10)
;(["bobby" 10/3] ["fire" 5] ["truck" 10] ["city" 5] ["department" 10/3] ["state" 0] ["colorado" 0])


(defn contains-token?
  "Given a sentence, does the given term exist in that sentence?"
  [sentence term]
  (let [tokens (tokenize sentence)]
    (boolean (some #{term} tokens))))


(defn get-matching-sentences
  "Given a sequence of sentences, return the sentences containing
  the term."
  [sentences term]
  (filter #(contains-token? % term) sentences))


(defn get-tagged-sentences
  "Return a sequence of POS-tagged sentences."
  [sentences]
  (map #(pos-tag (tokenize %)) sentences))


(defn get-weighted-sentences
  "Given POS-tagged sentences and a term, return a sequence of
  sentences that have been weighted."
  [tagged-sentences term]
  (map #(score-words term (map first (nouns-and-verbs %))) tagged-sentences))


(defn get-new-terms
  "Given a sequence of weighted sentences, return a map of new terms
  to be used for searching."
  [weighted-sentences]
  (into {}
        (reduce conj
                (reduce conj
                        (map #(filter (fn [pair] (not= 0 (second pair))) %)
                             weighted-sentences)))))

(defn get-scored-terms
  [text term]
  (let [sentences (get-sentences text)
        matched-sentences (get-matching-sentences sentences term)
        tagged-sentences (get-tagged-sentences matched-sentences)
        weighted-sentences (get-weighted-sentences tagged-sentences term)
        new-terms (get-new-terms weighted-sentences)]
    new-terms))


(def mytext "The Obama administration is considering requiring all automobiles to contain a brake override system intended to prevent sudden acceleration episodes like those that have led to the recall of millions of Toyotas, the Transportation secretary, Ray LaHood, said Tuesday. The override system is meant to deactivate the accelerator when the brake pedal is pressed. That will let the driver stop safely even if the car’s throttle sticks open. Often called a \"smart pedal,\" the feature is already found on many automobiles sold worldwide, including models from BMW, Chrysler, Mercedes-Benz, Nissan and Volkswagen.")

(get-scored-terms mytext "brake")
