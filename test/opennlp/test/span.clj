(ns opennlp.test.span
  (:use [clojure.test]
        [opennlp.span]))

(deftest in-span-test
  (is (= true (in-span? (->Span 10 12 nil) 10)))
  (is (= true (in-span? (->Span 10 12 nil) 11)))
  (is (= false (in-span? (->Span 10 12 nil) 9)))
  (is (= false (in-span? (->Span 10 12 nil) 12))))

(deftest contains-span-test
  (is (= true (contains-span? (->Span 10 12 nil) (->Span 10 12 nil))))
  (is (= true (contains-span? (->Span 9 12 nil) (->Span 10 12 nil))))
  (is (= true (contains-span? (->Span 10 13 nil) (->Span 10 12 nil))))
  (is (= true (contains-span? (->Span 0 20 nil) (->Span 10 12 nil))))
  (is (= false (contains-span? (->Span 10 12 nil) (->Span 9 12 nil))))
  (is (= false  (contains-span? (->Span 10 12 nil) (->Span 9 11 nil))))
  (is (= false  (contains-span? (->Span 10 12 nil) (->Span 10 13 nil))))
  (is (= false  (contains-span? (->Span 10 12 nil) (->Span 11 13 nil))))
  (is (= false  (contains-span? (->Span 10 12 nil) (->Span 0 20 nil)))))

(deftest right-of-span-test
  (is (= true (right-of-span? (->Span 10 12 nil) 12)))
  (is (= true (right-of-span? (->Span 10 12 nil) 13)))
  (is (= false  (right-of-span? (->Span 10 12 nil) 11)))
  (is (= false  (right-of-span? (->Span 10 12 nil) 9))))

(deftest end-of-span-test
  (is (= true (end-of-span? (->Span 10 12 nil) 11)))
  (is (= false (end-of-span? (->Span 10 12 nil) 12)))
  (is (= false (end-of-span? (->Span 10 12 nil) 9))))

(deftest merge-spans-test
  (is (= (merge-spans (->Span 10 12 :a) (->Span 11 15 :b)) (->Span 10 15 :a))))

(deftest span-disjoint-test
  (is (= true (span-disjoint? (->Span 10 12 nil) (->Span 15 20 nil))))
  (is (= true (span-disjoint? (->Span 10 12 nil) (->Span 12 20 nil))))
  (is (= true (span-disjoint? (->Span 15 20 nil) (->Span 10 12 nil))))
  (is (= true (span-disjoint? (->Span 15 20 nil) (->Span 10 14 nil))))
  (is (= false (span-disjoint? (->Span 10 12 nil) (->Span 11 20 nil))))
  (is (= false (span-disjoint? (->Span 10 12 nil) (->Span 10 20 nil))))
  (is (= false (span-disjoint? (->Span 10 12 nil) (->Span 5 20 nil))))
  (is (= false (span-disjoint? (->Span 10 12 nil) (->Span 5 11 nil))))
  (is (= false (span-disjoint? (->Span 10 12 nil) (->Span 11 12 nil))))
  (is (= false (span-disjoint? (->Span 11 20 nil) (->Span 10 12 nil))))
  (is (= false (span-disjoint? (->Span 10 20 nil) (->Span 10 12 nil))))
  (is (= false (span-disjoint? (->Span 5 20 nil) (->Span 10 12 nil))))
  (is (= false (span-disjoint? (->Span 5 11 nil) (->Span 10 12 nil))))
  (is (= false (span-disjoint? (->Span 11 12 nil) (->Span 10 12 nil)))))

(deftest intersection-span-test
  (is (= (intersection-span (->Span 5 15 :a) (->Span 10 20 :b))
         (->Span 10 15 :intersection)))
  (is (= (intersection-span (->Span 10 20 :b) (->Span 5 15 :a))
         (->Span 10 15 :intersection)))
  (is (= (intersection-span (->Span 5 20 :b) (->Span 10 15 :a))
         (->Span 10 15 :intersection)))
  (is (= (intersection-span (->Span 10 15 :b) (->Span 5 20 :a))
         (->Span 10 15 :intersection))))

(deftest span-length-test
  (is (= 5 (span-length (->Span 5 10 nil))))
  (is (= 1 (span-length (->Span 5 6 nil))))
  (is (= 0 (span-length (->Span 5 5 nil)))))

(deftest subs-span-test
  (is (= "had" (subs-span "Mary had a little lamb." (->Span 5 8 nil)))))

(deftest shift-span-test
  (is (= (shift-span (->Span 5 10 nil) 5) (->Span 10 15 nil)))
  (is (= (shift-span (->Span 5 10 nil) -5) (->Span 0 5 nil))))

(deftest between-span-test
  (is (= (between-span (->Span 5 10 nil) (->Span 15 20 nil))
         (->Span 10 15 :between)))
  (is (= (between-span (->Span 5 10 nil) (->Span 11 20 nil))
         (->Span 10 11 :between)))
  (is (= (between-span (->Span 5 10 nil) (->Span 10 20 nil))
         (->Span 10 10 :between))))
