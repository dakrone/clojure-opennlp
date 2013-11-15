(ns opennlp.test.tools.filters
  (:use [opennlp.nlp]
        [opennlp.treebank]
        [opennlp.tools.filters]
        [clojure.test]))

(def tokenize (make-tokenizer "models/en-token.bin"))
(def pos-tag (make-pos-tagger "models/en-pos-maxent.bin"))
(def chunker (make-treebank-chunker "models/en-chunker.bin"))

(deftest t-nil-noun-phrase
  (is (empty? (noun-phrases
               '({:phrase ["And"], :tag nil}))))
  (is (= [{:phrase ["And"], :tag "NP"}]
         (noun-phrases
          '({:phrase ["And"], :tag "NP"})))))

(deftest t-nil-phrases
  (is (= [{:phrase ["And"], :tag nil}]
         (nil-phrases
          '({:phrase ["And"], :tag nil})))))

(deftest t-noun-phrases
  (let [text (str "And when the party entered the assembly room, it consisted "
                  "of only five altogether; Mr. Bingley, his two sisters, the "
                  "husband of the eldest, and another young man.")
        np (-> text tokenize pos-tag chunker noun-phrases)]
    (is (= [{:phrase ["the" "party"] :tag "NP"}
            {:phrase ["the" "assembly" "room" ","] :tag "NP"}
            {:phrase ["it"] :tag "NP"}
            {:phrase ["only" "five" "altogether" ";"] :tag "NP"}
            {:phrase ["Mr." "Bingley" ","] :tag "NP"}
            {:phrase ["his" "two" "sisters" ","] :tag "NP"}
            {:phrase ["the" "husband"] :tag "NP"}
            {:phrase ["the" "eldest" "," "and"] :tag "NP"}
            {:phrase ["another" "young" "man"], :tag "NP"}]
           np))))
