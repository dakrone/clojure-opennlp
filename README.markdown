Clojure library interface to OpenNLP - http://opennlp.sf.net
============================================================

A library to interface with the OpenNLP (Open Natural Language Processing) library of functions. Not all functions are implemented yet.

Additional information/documentation:

- [Natural Language Processing in Clojure with clojure-opennlp](http://writequit.org/blog/?p=365)
- [Context searching using Clojure-OpenNLP](http://writequit.org/blog/?p=351)

Basic Example usage (from a REPL):
----------------------------------

    (use 'clojure.contrib.pprint) ; just for this documentation
    (use 'opennlp.nlp)

You will need to make the processing functions using the model files. These assume you're running
from the root project directory. You can also download the model files from the opennlp project
at http://opennlp.sourceforge.net/models/

    user=> (def get-sentences (make-sentence-detector "models/EnglishSD.bin.gz"))
    user=> (def tokenize (make-tokenizer "models/EnglishTok.bin.gz"))
    user=> (def pos-tag (make-pos-tagger "models/tag.bin.gz"))

For name-finders in particular, it's possible to have multiple model files:

    user=> (def name-find (make-name-finder "models/namefind/person.bin.gz" "models/namefind/organization.bin.gz"))
    
Then, use the functions you've created to perform operations on text:

    user=> (pprint (get-sentences "First sentence. Second sentence? Here is another one. And so on and so forth - you get the idea..."))
    ["First sentence. ", "Second sentence? ", "Here is another one. ",
     "And so on and so forth - you get the idea..."]
    nil
    
    user=> (pprint (tokenize "Mr. Smith gave a car to his son on Friday"))
    ["Mr.", "Smith", "gave", "a", "car", "to", "his", "son", "on",
     "Friday"]
    nil
    
    user=> (pprint (pos-tag (tokenize "Mr. Smith gave a car to his son on Friday.")))
    (["Mr." "NNP"]
     ["Smith" "NNP"]
     ["gave" "VBD"]
     ["a" "DT"]
     ["car" "NN"]
     ["to" "TO"]
     ["his" "PRP$"]
     ["son" "NN"]
     ["on" "IN"]
     ["Friday." "NNP"])
    nil

    user=> (name-find (tokenize "My name is Lee, not John."))
    ("Lee" "John")

Filtering pos-tagged sequences
------------------------------

    (use 'opennlp.tools.filters)

    user=> (pprint (nouns (pos-tag (tokenize "Mr. Smith gave a car to his son on Friday."))))
    (["Mr." "NNP"]
     ["Smith" "NNP"]
     ["car" "NN"]
     ["son" "NN"]
     ["Friday" "NNP"])
    nil
    user=> (pprint (verbs (pos-tag (tokenize "Mr. Smith gave a car to his son on Friday."))))
    (["gave" "VBD"])
    nil

Creating your own filters:
--------------------------

    user=> (pos-filter determiners #"^DT")
    #'user/determiners
    user=> (doc determiners)
    -------------------------
    user/determiners
    ([elements__52__auto__])
      Given a list of pos-tagged elements, return only the determiners in a list.
    nil
    user=> (pprint (determiners (pos-tag (tokenize "Mr. Smith gave a car to his son on Friday."))))
    (["a" "DT"])
    nil

TODO
----
- Treebank chunker (in progress)
- Treebank parser
- Model training/trainer
- Revisit datastructure format for tagged sentences
- Document *beam-size* functionality
- Build a full test suite
