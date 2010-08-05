Clojure library interface to OpenNLP - http://opennlp.sf.net
============================================================

A library to interface with the OpenNLP (Open Natural Language Processing)
library of functions. Not all functions are implemented yet.

Additional information/documentation:

- [Natural Language Processing in Clojure with clojure-opennlp](http://writequit.org/blog/?p=365)
- [Context searching using Clojure-OpenNLP](http://writequit.org/blog/?p=351)

Basic Example usage (from a REPL):
----------------------------------

    (use 'clojure.contrib.pprint) ; just for this documentation
    (use 'opennlp.nlp)

You will need to make the processing functions using the model files. These
assume you're running from the root project directory. You can also download
the model files from the opennlp project at [http://opennlp.sourceforge.net/models/](http://opennlp.sourceforge.net/models/)

    user=> (def get-sentences (make-sentence-detector "models/EnglishSD.bin.gz"))
    user=> (def tokenize (make-tokenizer "models/EnglishTok.bin.gz"))
    user=> (def pos-tag (make-pos-tagger "models/tag.bin.gz"))
    user=> (def chunker (make-treebank-chunker "models/EnglishChunk.bin.gz"))

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

Treebank-chunking splits and tags phrases from a pos-tagged sentence.
A notable difference is that it returns a list of structs with the
:phrase and :tag keys, as seen below:

    user=> (pprint (chunker (pos-tag (tokenize "The override system is meant to deactivate the accelerator when the brake pedal is pressed."))))
    ({:phrase ["The" "override" "system"], :tag "NP"}
     {:phrase ["is" "meant" "to" "deactivate"], :tag "VP"}
     {:phrase ["the" "accelerator"], :tag "NP"}
     {:phrase ["when"], :tag "ADVP"}
     {:phrase ["the" "brake" "pedal"], :tag "NP"}
     {:phrase ["is" "pressed"], :tag "VP"})
    nil


Treebank-parsing
----------------

<b>Note: Treebank parsing is very memory intensive, make sure your JVM has
a sufficient amount of memory available (using something like -Xmx1024m)
or you will run out of heap space when using a treebank parser.</b>

Treebank parsing gets its own section due to how complex it is. One difference
is in creating a treebank-parser, a map of options is allowed.

Note none of the treebank-parser models are included in the git repo, you will
have to download them separately from the opennlp project.

Regular:

    user=> (def treebank-parser (make-treebank-parser "parser-models/build.bin.gz" "parser-models/check.bin.gz" "parser-models/tag.bin.gz" "parser-models/chunk.bin.gz" "parser-models/head_rules"))

A parser with a tag dictionary file:

    user=> (def treebank-parser (make-treebank-parser "parser-models/build.bin.gz" "parser-models/check.bin.gz" "parser-models/tag.bin.gz" "parser-models/chunk.bin.gz" "parser-models/head_rules" {:tagdict "parser-models/tagdict"}))

A parser with case-sensitive tag dictionary file (default is false):

    user=> (def treebank-parser (make-treebank-parser "parser-models/build.bin.gz" "parser-models/check.bin.gz" "parser-models/tag.bin.gz" "parser-models/chunk.bin.gz" "parser-models/head_rules" {:tagdict "parser-models/tagdict" :case-sensitive true}))

To use the treebank-parser, pass an array of sentences with their tokens
separated by whitespace (preferably using tokenize)

    user=> (treebank-parser ["This is a sentence ."])
    ["(TOP (S (NP (DT This)) (VP (VBZ is) (NP (DT a) (NN sentence))) (. .)))"]

In order to transform the treebank-parser string into something a little easier
for Clojure to perform on, use the (make-tree ...) function:

    user=> (make-tree (first (treebank-parser ["This is a sentence ."])))
    {:chunk {:chunk ({:chunk {:chunk "This", :tag DT}, :tag NP} {:chunk ({:chunk "is", :tag VBZ} {:chunk ({:chunk "a", :tag DT} {:chunk "sentence", :tag NN}), :tag NP}), :tag VP} {:chunk ".", :tag .}), :tag S}, :tag TOP}

Here's the datastructure split into a little more readable format:

    {:tag TOP
     :chunk {:tag S
             :chunk ({:tag NP
                      :chunk {:tag DT
                              :chunk "This"}}
                     {:tag VP
                      :chunk ({:tag VBZ
                               :chunk "is"}
                              {:tag NP
                               :chunk ({:tag DT
                                        :chunk "a"}
                                       {:tag NN
                                        :chunk "sentence"})})}
                     {:tag .
                      :chunk "."})}}

Hopefully that makes it a little bit clearer, a nested map. If anyone else has
any suggesstions for better ways to represent this information, feel free to
send me an email or a patch.

Also note, make-tree uses Clojure's reader, so certain characters are not
supported, they will automatically be replaced:

    ( ) \ / # ~ ` ' " ^ @ ,

For a full reference of replaced chars, you can always look at strip-funny-chars
in src/opennlp/nlp.clj

Treebank parsing is considered beta at this point.


Filters
=======

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

Filtering treebank-chunks
-------------------------

    (use 'opennlp.tools.filters)

    opennlp.nlp=> (pprint (noun-phrases (chunker (pos-tag (tokenize "The override system is meant to deactivate the accelerator when the brake pedal is pressed")))))
    ({:phrase ["The" "override" "system"], :tag "NP"}
     {:phrase ["the" "accelerator"], :tag "NP"}
     {:phrase ["the" "brake" "pedal"], :tag "NP"})
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

You can also create treebank-chunk filters using (chunk-filter ...)

    user=> (chunk-filter fragments #"^FRAG$")
    opennlp.nlp=> (doc fragments)
    -------------------------
    opennlp.nlp/fragments
    ([elements__178__auto__])
      Given a list of treebank-chunked elements, return only the fragments in a list.
    nil

Known Issues
------------
- When using the treebank-chunker on a sentence, please ensure you have a period at the end of the sentence, if you do not have a period, the chunker gets confused and drops the last word.

TODO
----
- <del>Treebank chunker</del> (done!)
- <del>Treebank parser</del> (done!)
- Figure out what license to use.
- Laziness (in progress)
- Filters for treebank-parser
- Return multiple probability results for treebank-parser
- Model training/trainer
- Revisit datastructure format for tagged sentences
- Document *beam-size* functionality
- Document *advance-percentage* functionality
- Build a full test suite (in progress)
