(ns opennlp.tools.filters
  "Namespace used for filtering POS-tagged datastructures by grammatical
  classification. Also provides methods for building your own filters.")

(defmacro pos-filter
  "Declare a filter for pos-tagged vectors with the given name and regex."
  [n r]
  (let [docstring (str "Given a list of pos-tagged elements, "
                       "return only the " n " in a list.")]
    `(defn ~n
       ~docstring
       [elements#]
       (filter (fn [t#] (re-find ~r (second t#))) elements#))))

(defmacro chunk-filter
  "Declare a filter for treebank-chunked lists with the given name and regex."
  [n r]
  (let [docstring (str "Given a list of treebank-chunked elements, "
                       "return only the " n " in a list.")]
    `(defn ~n
       ~docstring
       [elements#]
       (filter (fn [t#] (if (nil? ~r)
                          (nil? (:tag t#))
                          (and (:tag t#)
                               (re-find ~r (:tag t#)))))
               elements#))))

;; It's easy to define your own filters!
(pos-filter nouns #"^NN")
(pos-filter nouns-and-verbs #"^(NN|VB)")
(pos-filter proper-nouns #"^NNP")
(pos-filter verbs #"^VB")

(chunk-filter verb-phrases #"^VP$")
(chunk-filter noun-phrases #"^NP$")
(chunk-filter adverb-phrases #"^ADVP$")
(chunk-filter adjective-phrases #"^ADJP$")

(chunk-filter nil-phrases nil)
