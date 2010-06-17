(ns opennlp.tools.lazy "Tools for lazily tokenizing and tagging sentences.")


(defn lazy-tokenize
  "Given a sequence of sentences, and a tokenizer, return a lazy sequence of
  tokenized sentences."
  [sentences tokenizer]
  (lazy-seq
    (when-let [s (seq sentences)]
      (cons (tokenizer (first s)) (lazy-tokenize (rest s) tokenizer)))))


(defn lazy-tag
  "Given a sequence of sentences, a tokenizer and a pos-tagger, return a lazy
  sequence of pos-tagged sentences."
  [sentences tokenizer pos-tagger]
  (lazy-seq
    (when-let [s (seq sentences)]
      (cons (pos-tagger (tokenizer (first s))) (lazy-tag (rest s) tokenizer pos-tagger)))))


(defn lazy-chunk
  "Given a sequence of sentences, a tokenizer, a pos-tagger and a chunker,
  return a lazy sequence of treebank-chunked sentences."
  [sentences tokenizer pos-tagger chunker]
  (lazy-seq
    (when-let [s (seq sentences)]
      (cons (chunker (pos-tagger (tokenizer (first s))))
            (lazy-chunk (rest s) tokenizer pos-tagger chunker)))))

; TODO: collapse these 3 functions into a generic one
