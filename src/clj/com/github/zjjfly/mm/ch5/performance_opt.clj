(ns com.github.zjjfly.mm.ch5.performance-opt
  (:require [criterium.core :as cc]
            [hiphip.int :as hi]
            ))

;clojure的代码如果想要达到和Java一样的性能，需要在代码中的一些额外的优化
;而且这些优化会让代码可读性变差，所以需要用macro让这部分丑陋的代码对用户不可见

;实现一个对集合求和的函数，并测试性能
(defn sum [xs]
  (reduce + xs))
(def collection (range 100))
(comment
  (cc/bench (sum collection))
  )

;实现一个对int数组求和的函数，且带有类型提示，并测试性能
(defn array-sum [^ints xs]
  (loop [index 0
         acc 0]
    (if (< index (alength xs))
      (recur (unchecked-inc index) (+ acc (aget xs index)))
      acc)))
(def array (into-array Integer/TYPE (range 100)))
(comment
  (cc/bench (array-sum array))
  )

;clojure内置了数组的高阶函数areduce，所以使用它来重构array-sum
(defn array-sum [^ints xs]
  (areduce xs i acc (int 0)
           (+ acc (aget xs i))))
(comment
  (cc/bench (array-sum array))
  )

;使用hiphip提供的asum，使用它不需要加类型提示
(comment
  (cc/bench (hi/asum array))
  )

;hiphip的asum是macro，所以支持类似for和doseq的binding
(hi/asum [n array] (* n n))

;asum展开出来和我们自己实现的array-sum函数基本是一样的，但可读性更高
;hiphip实现针对不同类型的asum的方式是在每个原始类型相关的namespace中，加载type_impl.clj的代码来动态声明asum等macro
;这样是为了避免定义生成macro的macro，这种macro会有多级的语法quote，非常难写，可读性也非常差

;另一种优化性能的方式是：把耗时的代码移到编译期
;假设有一个计算估计值的函数：
(defn calculate-estimate [{:keys [optimistic realistic pessimistic]}]
  (let [weighted-mean (/ (+ optimistic (* realistic 4) pessimistic) 6)
        std-dev (/ (- pessimistic optimistic) 6)] (double (+ weighted-mean (* 2 std-dev)))))

(comment
  (cc/bench (calculate-estimate {:optimistic 3 :realistic 5 :pessimistic 8}))
  ;Evaluation count : 166497480 in 60 samples of 2774958 calls.
  ;             Execution time mean : 375.598918 ns
  ;    Execution time std-deviation : 17.056036 ns
  ;   Execution time lower quantile : 357.334554 ns ( 2.5%)
  ;   Execution time upper quantile : 418.113961 ns (97.5%)
  ;                   Overhead used : 2.215258 ns
  )
;把它变成macro
(defmacro calculate-estimate [estimates]
  `(let [estimates# ~estimates
         optimistic# (:optimistic estimates#)
         realistic# (:realistic estimates#)
         pessimistic# (:pessimistic estimates#)
         weighted-mean# (/ (+ optimistic# (* realistic# 4) pessimistic#) 6)
         std-dev# (/ (- pessimistic# optimistic#) 6)]
     (double (+ weighted-mean# (* 2 std-dev#)))))
(comment
  (cc/bench (calculate-estimate {:optimistic 3 :realistic 5 :pessimistic 8}))
  ;Evaluation count : 170708580 in 60 samples of 2845143 calls.
  ;             Execution time mean : 359.285424 ns
  ;    Execution time std-deviation : 12.118017 ns
  ;   Execution time lower quantile : 349.056556 ns ( 2.5%)
  ;   Execution time upper quantile : 390.359104 ns (97.5%)
  ;                   Overhead used : 2.215258 ns
  )

;去掉macro中的quoting，把计算放到了macro展开的时候，这样展开出来的就是一个计算结果
(defmacro calculate-estimate
  [{:keys [optimistic realistic pessimistic]}]
  (let [weighted-mean (/ (+ optimistic (* realistic 4) pessimistic) 6)
        std-dev (/ (- pessimistic optimistic) 6)]
    (double (+ weighted-mean (* 2 std-dev)))))
(comment
  (cc/bench (calculate-estimate {:optimistic 3 :realistic 5 :pessimistic 8}))
  ;Evaluation count : 12000000000 in 60 samples of 200000000 calls.
  ;             Execution time mean : 2.515230 ns
  ;    Execution time std-deviation : 0.051670 ns
  ;   Execution time lower quantile : 2.440534 ns ( 2.5%)
  ;   Execution time upper quantile : 2.608722 ns (97.5%)
  ;                   Overhead used : 2.215258 ns
  ;
  ;Found 1 outliers in 60 samples (1.6667 %)
  ;	low-severe	 1 (1.6667 %)
  ; Variance from outliers : 9.3829 % Variance is slightly inflated by outliers
  )
;可以看出，性能提升了一百多倍！
;hiccup.core/html这个macro也有类似的优化
