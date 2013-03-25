(ns opennlp.test.sample
  (:use [clojure.test]
        [opennlp.sample])
  (:import (opennlp.tools.doccat DocumentSample)))

(use-fixtures :once
              (fn [f]
                (when (and (>= (:major *clojure-version*) 1)
                           (or (> (:major *clojure-version*) 1)
                               (> (:minor *clojure-version*) 3)))
                  (f))))

(deftest test-samples-round-trip
  (let [d (DocumentSample. "foo" "bar")]
    (is (= d (read-string (pr-str d))))))

(deftest test-clojure-document-sample-stream
  (let [d (DocumentSample. "foo" "bar")
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
