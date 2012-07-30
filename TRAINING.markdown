Training your own OpenNLP models
================================

While sometimes the models provided with OpenNLP are sufficient, many times 
they are insufficient. Either because your text is not in one of the supported 
languages or because the text is provided in an unconventional way. 

In the following document I will show you how to train your own models.

For these examples you will need:

    (use 'opennlp.nlp)
    (use 'opennlp.tools.train)

Sentence Detector
-----------------

To train a sentence splitter, simply provide a training file that includes
one sentence on every line.

sentdetect.train
    
    Being at the polls was just like being at church.
    I didn't smell a drop of liquor, and we didn't have a bit of trouble.
    The campaign leading to the election was not so quiet.
    It was marked by controversy, anonymous midnight phone calls and veiled threats of violence.
    During the election campaign, both candidates, Davis and Bush, reportedly received anonymous telephone calls.
    Ordinary Williams said he , too , was subjected to anonymous calls soon after he scheduled the election.
    Many local citizens feared that there would be irregularities at the polls. 
    Williams got himself a permit to carry a gun and promised an orderly election.
    He attended New York University before switching to Georgetown University in Washington.

A model can then be trained and passed into make-sentence-detector

    (def sent-model (train-sentence-detector "sentdetect.train"))
    (def get-sentences (make-sentence-detector sent-model))
    (get-sentences "Being at the polls was just like being at church. I didn't smell a drop of liquor, and we didn't have a bit of trouble.")

Tokenizer
---------

To train the tokenizer, include one sentence per line. The Tokenizer will
split on whitespace and explictly marked <SPLIT> tags. <SPLIT> tags are
most useful for separating words from punctuation. As an example:
 
tokenizer.train

    Being at the polls was just like being at church<SPLIT>.
    I didn't smell a drop of liquor<SPLIT>, and we didn't have a bit of trouble<SPLIT>.
    The campaign leading to the election was not so quiet<SPLIT>.

A model can then be trained and passed into make-tokenizer

     (def token-model (train-tokenizer "tokenizer.train"))
     (def tokenize (make-tokenizer token-model))
     (tokenize "Being at the polls was just like being at church.")

Part Of Speech Tagger
---------------------

To train a Part Of Speech tagger, provide one sentence per line. 
On each line every token should be separated by whitespace. The 
tokens themselves should be in ```word_tag``` format. Punctuation
is tagged as itself. As an example:

postagger.train

     Being_VBG at_IN the_DT polls_NNS was_VBD just_RB like_IN being_VBG at_IN church_NN ._.
     I_PRP did_VBD n't_RB smell_VB a_DT drop_NN of_IN liquor_NN ,_, and_CC we_PRP did_VBD n't_RB have_VB a_DT bit_NN of_IN trouble_NN ._.

A model can then be trained and passed into make-pos-tagger

     (def pos-model (train-pos-tagger "postagger.train"))
     (def pos-tag (make-pos-tagger pos-model))
     (pos-tag (tokenize "Being at the polls was just like being at church."))

Addition information

- [Succient summary of Treebank tags](http://www.ling.upenn.edu/courses/Fall_2003/ling001/penn_treebank_pos.html)
- [LDC Penn Treebank Tagging Guide](ftp://ftp.cis.upenn.edu/pub/treebank/doc/tagguide.ps.gz)

Treebank Chunker
----------------

Chunking uses the training format specified in the [CONLL 2000](http://www.cnts.ua.ac.be/conll2000/chunking/)
shared task. 

Training the chunker requires placing each word on a separate line.
Each line should have three space-delimited columns. The first 
for the ```word```, the second for the part-of-speech ```tag```, and 
lastly a column for ```chunk_tag```. 

The chunk tags start with a B or I followed by a dash and the type of 
the chunk. B delimits where a chunks begins, and I delimits where a 
chunk continues. If the chunk-tag is O, it means the word is not 
associated with any chunk. 

A blank line should exist between sentences. As an example:

chunker.train

     He PRP B-NP
     reckons VBZ B-VP
     the DT B-NP
     current JJ I-NP
     account NN I-NP
     deficit NN I-NP
     will MD B-VP
     narrow VB I-VP
     to TO B-PP
     only RB B-NP
     # # I-NP
     1.8 CD I-NP
     billion CD I-NP
     in IN B-PP
     September NNP B-NP
     . . O

     Chancellor NNP O
     of IN B-PP
     the DT B-NP
     Exchequer NNP I-NP
     Nigel NNP B-NP
     Lawson NNP I-NP
     's POS B-NP
     restated VBN I-NP
     commitment NN I-NP
     to TO B-PP
     a DT B-NP
     firm NN I-NP
     monetary JJ I-NP
     policy NN I-NP
     has VBZ B-VP
     helped VBN I-VP
     to TO I-VP
     prevent VB I-VP
     a DT B-NP
     freefall NN I-NP
     in IN B-PP
     sterling NN B-NP
     over IN B-PP
     the DT B-NP
     past JJ I-NP
     week NN I-NP
     . . O
     
     But CC O
     analysts NNS B-NP
     reckon VBP B-VP
     underlying VBG B-NP
     support NN I-NP
     for IN B-PP
     sterling NN B-NP
     has VBZ B-VP
     been VBN I-VP
     eroded VBN I-VP
     by IN B-PP
     the DT B-NP
     chancellor NN I-NP
     's POS B-NP
     failure NN I-NP
     to TO B-VP
     announce VB I-VP
     any DT B-NP
     new JJ I-NP
     policy NN I-NP
     measures NNS I-NP
     in IN B-PP
     his PRP$ B-NP
     Mansion NNP I-NP
     House NNP I-NP
     speech NN I-NP
     last JJ B-NP
     Thursday NNP I-NP
     . . O
     

A model can then be trained and passed into make-treebank-chunker

     (def chunk-model (train-treebank-chunker "chunker.train"))
     (def chunker (make-treebank-chunker chunk-model))
     (chunker (pos-tag (tokenize "He reckons the current account deficit will narrow to only #1.8 billion in September.")))

Named Entity finder
-------------------

To train the named entity finder, provide a training file with a single
sentence on each line. On these lines the entities should be delimited
with sgml tags <START> name <END>. An example follows:

named_org.train

      The departure of the <START> Giants <END> and the <START> Dodgers <END> to California left New York with only the <START> Yankees <END>.
      When he was unable to bring about immediate expansion, he sought to convince another <START> National League <END> club to move here.

A model can then be trained and passed into make-name-finder

      (def namefinder-model (train-name-finder "named_org.train"))
      (def name-find (make-name-finder namefinder-model))
      (name-find (tokenize "The Giants win the World Series."))

Treebank Parser
---------------

To train the treebank parser, provide a single sentence on each
line in the treebank format. To get an idea of the format, either
read [Treebank Annotating Guidelines](http://www.ldc.upenn.edu/Catalog/docs/LDC99T42/prsguid1.pdf) or
pass in some sentences with a previously trained model to get a feel.

parser.train

      (TOP (S (NP-SBJ (DT Some) )(VP (VBP say) (NP (NNP November) ))(. .) ))
      (TOP (S (NP-SBJ (PRP I) )(VP (VBP say) (NP (CD 1992) ))(. .) ('' '') ))
      (TOP (S (NP (DT This)) (VP (VBZ is) (NP (DT a) (NN sentence))) (. .)))

A model can then be trained and passed into make-treebank-parser. 
Headrules can be obtained from [OpenNLP](http://opennlp.sourceforge.net/models/english/parser/head_rules)

      (def parser-model (train-treebank-parser "parser.train" "headrules"))
      (def treebank-parser (make-treebank-parser parser-model))
      (treebank-parser ["This is a sentence ."])


POS Dictionary
--------------

A POS Dictionary, also referred to as a tagdict, is a data structure providing
a means of determining which tags are valid for a particular word. 

The format for a tagdict is one word per line. This word should then be followed
by a whitespace-delimited list of tags valid for that word.

     word tag1 tag2 ....

This dictionary can be created with ```build-posdictionary``` and then passed
to ```train-pos-tagger```

     (def tagdict (build-posdictionary "tagdict"))
     (def pos-tag (make-pos-tagger (train-pos-tagger "en" "postagger.train" tagdict)))


Document Categorization
-----------------------

To train a document categorizing tool, provide text with the format
similar to (as an example, a sentiment detector):

    Happy squealed
    Happy delight
    Happy upbeat
    Happy success
    Happy dream
    Happy smile
    Happy smiles
    Happy well
    Happy enjoy
    Happy sunny
    Unhappy foreboding
    Unhappy prisoner
    Unhappy frowning
    Unhappy confused
    Unhappy disapproving
    Unhappy upset

You can then train a model with this file:

    (def doccat-model (train-document-categorization "training/doccat.train"))
    (def doccat (make-document-categorizer doccat-model)
    (doccat "I like to smile.")
    => "Happy"

If you already have a model trained and saved as a .bin file, just load it as follows:
    
    (def doccat-model "models/en-doccat.bin"))
    (def doccat (make-document-categorizer doccat-model))
    (doccat "I like to smile.")
    => "Happy"

Notes
-----
If you get an Exception, you might just not have enough data.

