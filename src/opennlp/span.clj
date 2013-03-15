(ns opennlp.span)

(defrecord Span [start end type])

(defn make-span
  "Make a native span object."
  [start end type]
  (Span. start end type))

(defn in-span?
  "Return true if location k is in span. We assume span is [i,j)."
  [span k]
  (and (>= k (:start span)) (< k (:end span))))

(defn contains-span?
  "Return true if s1 is contains spans s2."
  [s1 s2]
  (and (>= (:start s2) (:start s1))
       (<= (:start s2) (:end s1))
       (>= (:end s2) (:start s1))
       (<= (:end s2) (:end s1))))

(defn right-of-span?
  "Return true if location k is to the right of span."
  [span k]
  (>= k (:end span)))

(defn end-of-span?
  "Return true if location k is the end of span."
  [span k]
  (== (dec (:end span)) k))

(defn merge-spans
  "Given two overlapping spans where the first comes before the second, return a
  merged span with the type of the first."
  [A B]
  (assoc A :end (:end B)))

(defn span-disjoint?
  "Return true of A does not overlap B."
  [A B]
  (or (<= (:end A) (:start B)) (>= (:start A) (:end B))))

(defn span-overlaps?
  "Return true if A overlaps B."
  [A B]
  (not (span-disjoint? A B)))

(defn intersection-span
  "Return the intersection of two spans as a span. Type of new span is
  :intersection."
  [A B]
  {:pre [(not (span-disjoint? A B))]}
  (->Span (max (:start A) (:start B)) (min (:end A) (:end B)) :intersection))

(defn span-length
  "Return the length of the span."
  [s]
  (- (:end s) (:start s)))

(defn subs-span
  "Return the substring corresponding to the span."
  [s span]
  (subs s (:start span) (:end span)))

(defn shift-span
  "Shift a span by i positions."
  [span i]
  (->Span (+ (:start span) i) (+ (:end span) i) (:type span)))

(defn between-span
  "Return a span of the area between two spans A and B. Type of new span is
  :between."
  [a b]
  {:pre [(<= (:end a) (:start b))]}
  (->Span (:end a) (:start b) :between))
