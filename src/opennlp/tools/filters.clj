(ns opennlp.tools.filters)

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

