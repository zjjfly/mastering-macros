(ns com.github.zjjfly.mm.ch8.pattern-matching
  (:require [clojure.core.match :as cm]
            [speclj.core :as spec]))

;使用macro实现类似Scala的match...case....
;先实现一个只能匹配int的模式匹配
(declare poor-match)
(spec/describe
  "pattern matching"
  (spec/it
    "matches an int"
    (let [match-simple-int-input (fn [n] (poor-match [n]
                                                     [0] :zero
                                                     [1] :one
                                                     [2] :two
                                                     :else :other))]
      (spec/should= :zero (match-simple-int-input 0))
      (spec/should= :one (match-simple-int-input 1))
      (spec/should= :two (match-simple-int-input 2))
      (spec/should= :other (match-simple-int-input 3)))))

(defn poor-match-clause [input [match-expression result]]
  (if (= :else match-expression)
    [:else result]
    [`(= ~input ~match-expression)
     result]))

(defmacro poor-match
  [input & more]
  (let [clauses (partition 2 more)]
    `(cond
       ~@(mapcat (partial poor-match-clause input)
                 clauses))))

;模式匹配的一个重要特性是可以进行在匹配的同时绑定变量，使用一个新的macro实现这一特性
(defn match-item? [matchable-item input]
  (cond (symbol? matchable-item) true
        (vector? matchable-item) (and (sequential? input)
                                      (every? identity (map match-item? matchable-item input)))
        :else (= input matchable-item)))

(defn create-test-expression [input match-expression]
  `(and (= (count ~input) ~(count match-expression))
        (every? identity
                (map match-item? '~match-expression ~input))))

(defn create-bindings-map [input match-expression]
  (let [pairs (map vector match-expression (concat input (repeat nil)))]
    (into {} (filter (fn [[k v]]
                       (not (or (keyword? k)
                                (number? k) (nil? k))))
                     pairs))))

(defn create-result-with-bindings [input match-expression result]
  (let [bindings-map (create-bindings-map input match-expression)]
    `(let [~@(mapcat identity bindings-map)] ~result)))

(defn match-clause [input [match-expression result]]
  (if (= :else match-expression)
    [:else result]
    [(create-test-expression input match-expression)
     (create-result-with-bindings input match-expression result)]))

(defmacro match [input & more]
  (let [clauses (partition 2 more)]
    `(cond ~@(mapcat (partial match-clause input) clauses))))

(spec/describe
  "pattern matching with binding"
  (spec/it "matches and binds"
           (let [match-and-bind (fn [[a b]]
                                  (match [a b]
                                         [0 y] {:axis "Y" :y y}
                                         [x 0] {:axis "X" :x x}
                                         [x y] {:x x :y y}))]
             (spec/should= {:axis "Y" :y 5} (match-and-bind [0 5]))
             (spec/should= {:axis "Y" :y 3} (match-and-bind [0 3]))
             (spec/should= {:axis "X" :x 1} (match-and-bind [1 0]))
             (spec/should= {:axis "X" :x 2} (match-and-bind [2 0]))
             (spec/should= {:x 1 :y 2} (match-and-bind [1 2]))
             (spec/should= {:x 2 :y 1} (match-and-bind [2 1]))))
  (spec/it "handles vector destructuring"
           (let [match-and-bind
                 (fn [[a b]]
                   (match [a b]
                          [0 [y & more]] {:axis "Y" :y y :more more}
                          [[x & more] 0] {:axis "X" :x x :more more}
                          [x y] {:x x :y y}))]
             (spec/should= {:axis "Y" :y 5 :more [6 7]} (match-and-bind [0 [5 6 7]]))
             (spec/should= {:axis "X" :x 1 :more [2 3]} (match-and-bind [[1 2 3] 0]))
             (spec/should= {:x 1 :y 2} (match-and-bind [1 2]))))
  )

;使用match来实现merge sort
(defn my-merge [xs ys]
  (loop [acc [] xs xs ys ys]
    (match [(seq xs) (seq ys)]
           [nil b] (concat acc b)
           [a nil] (concat acc a)
           [[x & x-rest] [y & y-rest]]
           (if (< x y)
             (recur (conj acc x) x-rest ys)
             (recur (conj acc y) xs y-rest)))))
(spec/describe
  "merge sort"
  (spec/it "implements merge (from merge-sort)"
           (spec/should= [1 2 3] (my-merge [1 2 3] []))
           (spec/should= [1 2 3] (my-merge [1 2 3] nil))
           (spec/should= [1 2 3 4] (my-merge [1 2 3] [4]))
           (spec/should= [1 2 3] (my-merge [] [1 2 3]))
           (spec/should= [1 2 3] (my-merge nil [1 2 3]))
           (spec/should= [1 2 3 4 5 6 7] (my-merge [1 3 4 7] [2 5 6]))))

(comment
  ;执行spec
  (spec/run-specs)
  )

(comment
  ;使用core.match中实现的模式匹配，它比我们自己实现的更成熟，也更高效
  (doseq [n (range 1 101)]
    (println
      (cm/match [(mod n 3) (mod n 5)]
                [0 0] "FizzBuzz"
                [0 _] "Fizz"
                [_ 0] "Buzz"
                :else n))))
