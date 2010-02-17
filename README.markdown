Clojure library interface to OpenNLP - http://opennlp.sf.net

More info coming soon.

Example usage (from a REPL):
--------------

    (use 'clojure.contrib.pprint)
    (use 'opennlp)

    ; Make our functions with the model file. These assume you're running
    ; from the root project directory.
    opennlp=> (def get-sentences (make-sentence-detector "models/EnglishSD.bin.gz"))
    opennlp=> (def tokenize (make-tokenizer "models/EnglishTok.bin.gz"))
    opennlp=> (def pos-tag (make-pos-tagger "models/tag.bin.gz"))
    
    opennlp=> (pprint (get-sentences "First sentence. Second sentence? Here is another one. And so on and so forth - you get the idea..."))
    ["First sentence. ", "Second sentence? ", "Here is another one. ",
     "And so on and so forth - you get the idea..."]
    nil
    
    opennlp=> (pprint (tokenize "Mr. Smith gave a car to his son on Friday"))
    ["Mr.", "Smith", "gave", "a", "car", "to", "his", "son", "on",
     "Friday"]
    nil
    
    opennlp=> (pprint (pos-tag (tokenize "Mr. Smith gave a car to his son on Friday.")))
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
