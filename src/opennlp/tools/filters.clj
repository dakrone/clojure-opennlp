(ns opennlp.tools.filters
  (:use [clojure.contrib.seq-utils])
  (:use [clojure.contrib.math]))

(defmacro pos-filter
  "Declare a filter for pos-tagged vectors with the given name and regex."
  [n r]
  (let [docstring (str "Given a list of pos-tagged elements, return only the " n " in a list.")]
    `(defn ~n
       ~docstring
       [elements#]
       (filter (fn [t#] (re-find ~r (second t#))) elements#))))


; It's easy to define your own filters!
(pos-filter nouns #"^NN")
(pos-filter proper-nouns #"^NNP")
(pos-filter verbs #"^VB")


; Naive first attempt.
;(defn score-words
  ;"Score a list of words linearly based on how far they are from the
  ;term.  Base score is optional and is 1 by default.  Case sensitive."
  ;([term words]
   ;(score-words term words 1))
  ;([term words basescore]
   ;(let [index (.indexOf words term)]
     ;(if (= -1 index)
       ;(map #(vector % 0) words) ; no matches
       ;(map
         ;(fn
           ;[word]
           ;(let [idx  (.indexOf words word)
                 ;dist (abs (- index idx))
                 ;score (/ basescore (+ 1 dist))]
             ;(if (> dist 2)
               ;(vector word 0)
               ;(vector word score)))) words)))))

; Jon
; This is not used yet.
(defn- mindist
  "Give the minimum distance from the first arg to any value in the second arg."
  [n ns]
  (apply min (map #(abs (- n %)) ns)))

(defn- score-word
  "Score the index-term based on it's distance from any intex-term in the given
  search list. Use the base score to boost matches from 1 up to another value."
  [iword iterms base]
  (let [dist (mindist (first iword) iterms)
        score (/ base (+ 1 dist))]
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

;opennlp.tools.filters=> words
;["bobby" "fire" "truck" "city" "department" "state" "colorado"]
;opennlp.tools.filters=> (score-words "truck" words)
;(["bobby" 1/3] ["fire" 1/2] ["truck" 1] ["city" 1/2] ["department" 1/3] ["state" 0] ["colorado" 0])
;opennlp.tools.filters=> (score-words "truck" words 10)
;(["bobby" 10/3] ["fire" 5] ["truck" 10] ["city" 5] ["department" 10/3] ["state" 0] ["colorado" 0])

