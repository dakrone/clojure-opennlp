Clojure library interface to OpenNLP - http://opennlp.sf.net
============================================================

A library to interface with the OpenNLP (Open Natural Language Processing) library of functions. Not all functions are implemented yet.

Basic Example usage (from a REPL):
----------------------------------

    (use 'clojure.contrib.pprint)
    (use 'opennlp.nlp)

You will need to make the processing functions using the model files. These assume you're running
from the root project directory. You can also download the model files from the opennlp project
at http://opennlp.sourceforge.net/models/

    user=> (def get-sentences (make-sentence-detector "models/EnglishSD.bin.gz"))
    user=> (def tokenize (make-tokenizer "models/EnglishTok.bin.gz"))
    user=> (def pos-tag (make-pos-tagger "models/tag.bin.gz"))
    
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
    
