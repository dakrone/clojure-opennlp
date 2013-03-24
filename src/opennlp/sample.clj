(ns opennlp.sample
  (:require [clojure.java.io :as io])
  (:import (opennlp.tools.doccat DocumentSample)
           (opennlp.tools.util ObjectStream)))

(defn print-sample [sample ^java.io.Writer w]
  (.write w "#opennlp/sample {")
  (.write w ":category ")
  (binding [*out* w]
    (pr (.getCategory sample)))
  (.write w " :text ")
  (binding [*out* w]
    (pr (vec (.getText sample))))
  (.write w "}"))

(defmethod print-method DocumentSample
  [sample w]
  (print-sample sample w))

(defmethod print-dup DocumentSample
  [sample w]
  (print-sample sample w))

(defn read-document-sample [{:keys [category text]}]
  (DocumentSample. category (into-array String text)))

(defn clojure-document-sample-stream [in]
  (let [i (java.io.PushbackReader. (io/reader in))
        buf (atom [])
        pos (atom 0)]
    (reify
      ObjectStream
      (read [_]
        (if (= @pos (count @buf))
          (when-let [obj (read i false nil)]
            (swap! buf conj obj)
            (swap! pos inc)
            obj)
          (let [p @pos]
            (swap! pos inc)
            (nth @buf p))))
      (close [_]
        (.close i)
        (.close in))
      (reset [_]
        (reset! pos 0)))))
