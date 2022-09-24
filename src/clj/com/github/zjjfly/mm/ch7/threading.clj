(ns com.github.zjjfly.mm.ch7.threading
  (:import (clojure.lang ISeq)))

;Clojure中很常用的一个macro是->，它可以把多重嵌套的表达式变成一连串表达式的pipeline，这可以大大增加可读性
;不使用->
(* (+ 4 3) 2)
;使用->
(-> 4
    (+ 3)
    (* 2))
;->可以保留meta data
(-> 10
    ^ISeq range
    .iterator
    (doto .next .next)
    .next)

;使用函数实现->
(defn thread-first-fn [x & fns]
  (reduce (fn [acc f] (f acc))
          x fns))
(thread-first-fn 1
                 #(+ % 2)
                 #(* % 3)
                 #(+ % 4)
                 #(* % 5))
(defn thread-first-fn' [x & fns]
  ((apply comp (reverse fns)) x))
(thread-first-fn' 1
                  #(+ % 2)
                  #(* % 3)
                  #(+ % 4)
                  #(* % 5))
;但函数实现的缺点是，参数里只能使用函数，无法使用macro

;->还有几个类似的很有用的macro，如->>，as->，cond->，some->
;as->当传入的函数不是都能把参数作为第一个参数调用的时候非常有用
(as-> 1 i
      (+ 2 i)
      (* i 3))
;cond->接收一个初始值和若干test/form对，当test为true时，执行form，否则跳过
(cond-> 1
        true inc
        false dec)
;some->当传入的函数会生成null值的时候，为了避免在下一个函数调用的时候因为传入null而报错的情况下使用的
(some-> {:a 1 :b 2}
        :c
        inc)
;nil
