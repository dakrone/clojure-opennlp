Clojure library interface to OpenNLP - https://opennlp.apache.org/
============================================================

A library to interface with the OpenNLP (Open Natural Language Processing)
library of functions. Not all functions are implemented yet.

Additional information/documentation:

- [Natural Language Processing in Clojure with clojure-opennlp](http://writequit.org/blog/index.html%3Fp=365.html)
- [Context searching using Clojure-OpenNLP](http://writequit.org/blog/index.html%3Fp=351.html)

Read the source from Marginalia

- http://dakrone.github.com/clojure-opennlp/

[![Continuous Integration status](https://secure.travis-ci.org/dakrone/clojure-opennlp.png)](http://travis-ci.org/dakrone/clojure-opennlp)

Known Issues
------------
- When using the treebank-chunker on a sentence, please ensure you
have a period at the end of the sentence, if you do not have a period,
the chunker gets confused and drops the last word. Besides, your
sentences should all be grammactially correct anyway right?


Usage from Leiningen:
--------------------

```clojure
[clojure-opennlp "0.5.0"] ;; uses Opennlp 1.9.0
```

clojure-opennlp works with clojure 1.5+

Basic Example usage (from a REPL):
----------------------------------

```clojure
(use 'clojure.pprint) ; just for this documentation
(use 'opennlp.nlp)
(use 'opennlp.treebank) ; treebank chunking, parsing and linking lives here
```

You will need to make the processing functions using the model files. These
assume you're running from the root project directory. You can also download
the model files from the opennlp project at [http://opennlp.sourceforge.net/models-1.5](http://opennlp.sourceforge.net/models-1.5)

```clojure
(def get-sentences (make-sentence-detector "models/en-sent.bin"))
(def tokenize (make-tokenizer "models/en-token.bin"))
(def detokenize (make-detokenizer "models/english-detokenizer.xml"))
(def pos-tag (make-pos-tagger "models/en-pos-maxent.bin"))
(def name-find (make-name-finder "models/namefind/en-ner-person.bin"))
(def chunker (make-treebank-chunker "models/en-chunker.bin"))
```

The tool-creators are multimethods, so you can also create any of the
tools using a model instead of a filename (you can create a model with
the training tools in src/opennlp/tools/train.clj):

```clojure
(def tokenize (make-tokenizer my-tokenizer-model)) ;; etc, etc
```

Then, use the functions you've created to perform operations on text:

Detecting sentences:

```clojure
(pprint (get-sentences "First sentence. Second sentence? Here is another one. And so on and so forth - you get the idea..."))
["First sentence. ", "Second sentence? ", "Here is another one. ",
 "And so on and so forth - you get the idea..."]
```

Tokenizing:

```clojure
(pprint (tokenize "Mr. Smith gave a car to his son on Friday"))
["Mr.", "Smith", "gave", "a", "car", "to", "his", "son", "on",
 "Friday"]
```

Detokenizing:

```clojure
(detokenize ["Mr.", "Smith", "gave", "a", "car", "to", "his", "son", "on", "Friday"])
"Mr. Smith gave a car to his son on Friday."
```

Ideally, s == (detokenize (tokenize s)), the detokenization model XML
file is a work in progress, please let me know if you run into
something that doesn't detokenize correctly in English.


Part-of-speech tagging:

```clojure
(pprint (pos-tag (tokenize "Mr. Smith gave a car to his son on Friday.")))
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
```

Name finding:

```clojure
(name-find (tokenize "My name is Lee, not John."))
("Lee" "John")
```

Treebank-chunking splits and tags phrases from a pos-tagged sentence.
A notable difference is that it returns a list of structs with the
:phrase and :tag keys, as seen below:

```clojure
(pprint (chunker (pos-tag (tokenize "The override system is meant to deactivate the accelerator when the brake pedal is pressed."))))
({:phrase ["The" "override" "system"], :tag "NP"}
 {:phrase ["is" "meant" "to" "deactivate"], :tag "VP"}
 {:phrase ["the" "accelerator"], :tag "NP"}
 {:phrase ["when"], :tag "ADVP"}
 {:phrase ["the" "brake" "pedal"], :tag "NP"}
 {:phrase ["is" "pressed"], :tag "VP"})
```

For just the phrases:

```clojure
(phrases (chunker (pos-tag (tokenize "The override system is meant to deactivate the accelerator when the brake pedal is pressed."))))
(["The" "override" "system"] ["is" "meant" "to" "deactivate"] ["the" "accelerator"] ["when"] ["the" "brake" "pedal"] ["is" "pressed"])
```

And with just strings:

```clojure
(phrase-strings (chunker (pos-tag (tokenize "The override system is meant to deactivate the accelerator when the brake pedal is pressed."))))
("The override system" "is meant to deactivate" "the accelerator" "when" "the brake pedal" "is pressed")
```

Document Categorization:

See opennlp.test.tools.train for better usage examples.

```clojure
(def doccat (make-document-categorizer "my-doccat-model"))

(doccat "This is some good text")
"Happy"
```

Probabilities of confidence
---------------------------

The probabilities OpenNLP supplies for a given operation are available
as metadata on the result, where applicable:

```clojure
(meta (get-sentences "This is a sentence. This is also one."))
{:probabilities (0.9999054310803004 0.9941126097177366)}

(meta (tokenize "This is a sentence."))
{:probabilities (1.0 1.0 1.0 0.9956236737394807 1.0)}

(meta (pos-tag ["This" "is" "a" "sentence" "."]))
{:probabilities (0.9649410482478001 0.9982592902509803 0.9967282012835504 0.9952498677248117 0.9862225658078769)}

(meta (chunker (pos-tag ["This" "is" "a" "sentence" "."])))
{:probabilities (0.9941248001899835 0.9878092935921453 0.9986106511439116 0.9972975733070356 0.9906377695586069)}

(meta (name-find ["My" "name" "is" "John"]))
{:probabilities (0.9996272005494383 0.999999997485361 0.9999948113868132 0.9982291838206192)}
```



Beam Size
---------

You can rebind ```opennlp.nlp/*beam-size*``` (the default is 3) for
the pos-tagger and treebank-parser with:

```clojure
(binding [*beam-size* 1]
  (def pos-tag (make-pos-tagger "models/en-pos-maxent.bin")))
```


Advance Percentage
---------

You can rebind ```opennlp.treebank/*advance-percentage*``` (the default is 0.95) for
the treebank-parser with:

```clojure
(binding [*advance-percentage* 0.80]
  (def parser (make-treebank-parser "parser-model/en-parser-chunking.bin")))
```


Treebank-parsing
----------------

<b>Note: Treebank parsing is very memory intensive, make sure your JVM has
a sufficient amount of memory available (using something like -Xmx512m)
or you will run out of heap space when using a treebank parser.</b>

Treebank parsing gets its own section due to how complex it is.

Note none of the treebank-parser model is included in the git repo, you will
have to download it separately from the opennlp project.

Creating it:

```clojure
(def treebank-parser (make-treebank-parser "parser-model/en-parser-chunking.bin"))
```

To use the treebank-parser, pass an array of sentences with their tokens
separated by whitespace (preferably using tokenize)

```clojure
(treebank-parser ["This is a sentence ."])
["(TOP (S (NP (DT This)) (VP (VBZ is) (NP (DT a) (NN sentence))) (. .)))"]
```

In order to transform the treebank-parser string into something a little easier
for Clojure to perform on, use the (make-tree ...) function:

```clojure
(make-tree (first (treebank-parser ["This is a sentence ."])))
{:chunk {:chunk ({:chunk {:chunk "This", :tag DT}, :tag NP} {:chunk ({:chunk "is", :tag VBZ} {:chunk ({:chunk "a", :tag DT} {:chunk "sentence", :tag NN}), :tag NP}), :tag VP} {:chunk ".", :tag .}), :tag S}, :tag TOP}
```

Here's the datastructure split into a little more readable format:

```clojure
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
```

Hopefully that makes it a little bit clearer, a nested map. If anyone else has
any suggesstions for better ways to represent this information, feel free to
send me an email or a patch.

Treebank parsing is considered beta at this point.


Filters
=======

Filtering pos-tagged sequences
------------------------------

```clojure
(use 'opennlp.tools.filters)

(pprint (nouns (pos-tag (tokenize "Mr. Smith gave a car to his son on Friday."))))
(["Mr." "NNP"]
 ["Smith" "NNP"]
 ["car" "NN"]
 ["son" "NN"]
 ["Friday" "NNP"])

(pprint (verbs (pos-tag (tokenize "Mr. Smith gave a car to his son on Friday."))))
(["gave" "VBD"])
```

Filtering treebank-chunks
-------------------------

```clojure
(use 'opennlp.tools.filters)

(pprint (noun-phrases (chunker (pos-tag (tokenize "The override system is meant to deactivate the accelerator when the brake pedal is pressed")))))
({:phrase ["The" "override" "system"], :tag "NP"}
 {:phrase ["the" "accelerator"], :tag "NP"}
 {:phrase ["the" "brake" "pedal"], :tag "NP"})
```

Creating your own filters:
--------------------------

```clojure
(pos-filter determiners #"^DT")
#'user/determiners
(doc determiners)
-------------------------
user/determiners
([elements__52__auto__])
  Given a list of pos-tagged elements, return only the determiners in a list.

(pprint (determiners (pos-tag (tokenize "Mr. Smith gave a car to his son on Friday."))))
(["a" "DT"])
```

You can also create treebank-chunk filters using (chunk-filter ...)

```clojure
(chunk-filter fragments #"^FRAG$")

(doc fragments)
-------------------------
opennlp.nlp/fragments
([elements__178__auto__])
  Given a list of treebank-chunked elements, return only the fragments in a list.
```


Being Lazy
==========

There are some methods to help you be lazy when tagging methods, depending on the operation desired,
use the corresponding method:

    #'opennlp.tools.lazy/lazy-get-sentences
    #'opennlp.tools.lazy/lazy-tokenize
    #'opennlp.tools.lazy/lazy-tag
    #'opennlp.tools.lazy/lazy-chunk
    #'opennlp.tools.lazy/sentence-seq

Here's how to use them:

```clojure
(use 'opennlp.nlp)
(use 'opennlp.treebank)
(use 'opennlp.tools.lazy)

(def get-sentences (make-sentence-detector "models/en-sent.bin"))
(def tokenize (make-tokenizer "models/en-token.bin"))
(def pos-tag (make-pos-tagger "models/en-pos-maxent.bin"))
(def chunker (make-treebank-chunker "models/en-chunker.bin"))

(lazy-get-sentences ["This body of text has three sentences. This is the first. This is the third." "This body has only two. Here's the last one."] get-sentences)
;; will lazily return:
(["This body of text has three sentences. " "This is the first. " "This is the third."] ["This body has only two. " "Here's the last one."])

(lazy-tokenize ["This is a sentence." "This is another sentence." "This is the third."] tokenize)
;; will lazily return:
(["This" "is" "a" "sentence" "."] ["This" "is" "another" "sentence" "."] ["This" "is" "the" "third" "."])

(lazy-tag ["This is a sentence." "This is another sentence."] tokenize pos-tag)
;; will lazily return:
((["This" "DT"] ["is" "VBZ"] ["a" "DT"] ["sentence" "NN"] ["." "."]) (["This" "DT"] ["is" "VBZ"] ["another" "DT"] ["sentence" "NN"] ["." "."]))

(lazy-chunk ["This is a sentence." "This is another sentence."] tokenize pos-tag chunker)
;; will lazily return:
(({:phrase ["This"], :tag "NP"} {:phrase ["is"], :tag "VP"} {:phrase ["a" "sentence"], :tag "NP"}) ({:phrase ["This"], :tag "NP"} {:phrase ["is"], :tag "VP"} {:phrase ["another" "sentence"], :tag "NP"}))
```

Feel free to use the lazy functions, but I'm still not 100% set on the
layout, so they may change in the future. (Maybe chaining them so
instead of a sequence of sentences it looks like (lazy-chunk (lazy-tag
(lazy-tokenize (lazy-get-sentences ...))))).

<b>Generating a lazy sequence of sentences from a file using
opennlp.tools.lazy/sentence-seq:</b>

```clojure
(with-open [rdr (clojure.java.io/reader "/tmp/bigfile")]
  (let [sentences (sentence-seq rdr get-sentences)]
    ;; process your lazy seq of sentences however you desire
    (println "first 5 sentences:")
    (clojure.pprint/pprint (take 5 sentences))))
```


Training
--------
There is code to allow for training models for each of the
tools. Please see the documentation in TRAINING.markdown


License
-------
Copyright (C) 2010 Matthew Lee Hinman

Distributed under the Eclipse Public License, the same as Clojure uses. See the file COPYING.


Contributors
------------
- Rob Zinkov - zaxtax
- Alexandre Patry - apatry


TODO
----
- <del>add method to generate lazy sequence of sentences from a file</del> (done!)
- <del>Detokenizer</del> (still more work to do, but it works for now)
- Do something with parse-num for treebank parsing
- <del>Split up treebank stuff into its own namespace</del> (done!)
- <del>Treebank chunker</del> (done!)
- <del>Treebank parser</del> (done!)
- <del>Laziness </del> (done! for now.)
- Treebank linker (WIP)
- <del>Phrase helpers for chunker</del> (done!)
- <del>Figure out what license to use.</del> (done!)
- Filters for treebank-parser
- Return multiple probability results for treebank-parser
- <del>Explore including probability numbers</del> (probability numbers added as metadata)
- <del>Model training/trainer</del> (done!)
- Revisit datastructure format for tagged sentences
- <del>Document *beam-size* functionality</del>
- <del>Document *advance-percentage* functionality</del>
- Build a full test suite:
-- <del>core tools</del> (done)
-- <del>filters</del> (done)
-- <del>laziness</del> (done)
-- training (pretty much done except for tagging)
