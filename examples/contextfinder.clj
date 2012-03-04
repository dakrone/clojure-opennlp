;; This example is very crazy and may be broken right now. Attempt to
;; use at your own risk. Eventually it will be documented.
(ns contextfinder
  (:use [clojure.pprint :only [pprint]]
        [opennlp.nlp]
        [opennlp.tools.filters]))


;; Requires you to run this from the root opennlp directory or have the
;; models downloaded into a "models" folder
(def get-sentences (make-sentence-detector "models/en-sent.bin"))
(def tokenize (make-tokenizer "models/en-token.bin"))
(def pos-tag (make-pos-tagger "models/en-pos-maxent.bin"))



(defn- mindist
  "Give the minimum distance from the first arg to any value in the second arg."
  [n ns]
  (apply min (map #(Math/abs (- n %)) ns)))


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
     (let [iwords (map vector (iterate inc 0) words)
           iterms (map first (filter (fn [e] (= (second e) term)) iwords))]
       (if (= 0 (count iterms))
         (map #(vector % 0) words)
         (map #(vector (second %) (score-word % iterms base)) iwords)))))


(defn nv-filter
  "Filter tagged sentences by noun, verb and >= 3 characters."
  [tagged-sentence]
  (filter #(>= (count (first %)) 3) (nouns-and-verbs tagged-sentence)))


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
  (map #(score-words term (map first (nv-filter %))) tagged-sentences))


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
  "Given a block of text and a search term, return a map of new search
  terms as keys with weighted score values."
  [text term]
  (let [sentences (get-sentences text)
        matched-sentences (get-matching-sentences sentences term)
        tagged-sentences (get-tagged-sentences matched-sentences)
        weighted-sentences (get-weighted-sentences tagged-sentences term)
        new-terms (get-new-terms weighted-sentences)]
    new-terms))


(defn score-sentence
  "Given a sentence and a map of words & their scores, return the score
  of the sentence."
  [sentence score-words]
  (let [tokens (tokenize sentence)]
    (reduce + (map #(get score-words % 0) tokens))))


(defn score-sentences
  "Given a text and a map of words/scores. Return a list of sentences
  and their scores."
  [text score-words]
  (let [sentences (get-sentences text)]
    (for [s sentences]
      [s (score-sentence s score-words)])))


(defn score-text
  "Score a block of text, given a map of score-words."
  [text score-words]
  (let [sentences (get-sentences text)]
    (reduce + (map #(score-sentence % score-words) sentences))))

(def mytext "The Obama administration is considering requiring all automobiles to contain a brake override system intended to prevent sudden acceleration episodes like those that have led to the recall of millions of Toyotas, the Transportation secretary, Ray LaHood, said Tuesday. The override system is meant to deactivate the accelerator when the brake pedal is pressed. That will let the driver stop safely even if the car’s throttle sticks open. Often called a \"smart pedal,\" the feature is already found on many automobiles sold worldwide, including models from BMW, Chrysler, Mercedes-Benz, Nissan and Volkswagen.")

(def scorewords (get-scored-terms mytext "brake"))

;; Get a list of ranked sentences
(pprint (reverse (sort-by second (score-sentences mytext scorewords))))

;; Score the whole text
(println (score-text mytext scorewords))
