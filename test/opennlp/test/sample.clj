(ns opennlp.test.sample
  (:require [clojure.test :refer :all]
            [opennlp.sample :refer [clojure-document-sample-stream]])
  (:import (opennlp.tools.doccat DocumentSample)))

(deftest test-samples-round-trip
  (let [d #opennlp/sample {:category "foo" :text ["bar"]}]
    (is (= d (read-string (pr-str d))))))

(deftest test-clojure-document-sample-stream
  (let [d #opennlp/sample {:category "foo" :text ["bar"]}
        x (java.io.ByteArrayInputStream.
           (.getBytes
            (with-out-str
              (prn d)
              (prn d))))
        s (clojure-document-sample-stream x)]
    (is (= (.read s) d))
    (is (= (.read s) d))
    (is (nil? (.read s)))
    (.reset s)
    (is (= (.read s) d))
    (is (= (.read s) d))
    (is (nil? (.read s)))))
