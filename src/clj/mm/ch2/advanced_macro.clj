(ns mm.ch2.advanced-macro)

;使用到目前为止的所学，来实现一个assert
(defmacro my-assert [x]
  (list 'when-not x (list 'throw
                          (list 'new 'AssertionError
                                (list 'str "Assert failed: "
                                      (list 'pr-str (list 'quote x)))))))
(comment
  (my-assert (= 1 2))
  ;Assert failed: (= 1 2)
  )

;使用目前的所学来实现的宏都是使用list或cons拼接verb和参数为一个list，但这样写的可读性非常差
;clojure中存在称为syntax-quote的技术可以避免这个问题，它可以让我们写的macro和展开的结果更接近

;假设我们想要在list中插入一个值，使用一般的quote是办不到的
(def a 4)
'(1 2 3 a 5)
;(1 2 3 a 5)
;可以看到，a并没有解析为4，需要使用list函数才行
(list 1 2 3 a 5)
;(1 2 3 4 5)
;或者使用syntax-quote和unquote
`(1 2 3 ~a 5)
;(1 2 3 4 5)

;如果看clojure.core/assert的源码，会发现其中就使用了syntax-quote和unquote
;(defmacro assert [x]
; (when *assert*
;   `(when-not ~x
;      (throw (new AssertionError (str "Assert failed: " (pr-str '~x)))))))

;其中的'~是先unquote然后进行quote
(def a 4)
`(1 2 3 '~a 5)
;(1 2 3 (quote 4) 5)
;和quote一样，unquote也是一个reader macro，~是简写，对应的是clojure.core/unquote
;clojure的reader会在syntax-quote中查找clojure.core/unquote，如果找到会进行unquote
`(1 2 3 (quote (clojure.core/unquote a)) 5)
;(1 2 3 (quote 4) 5)
;unquote一般不会再syntax-quote之外使用，但Leiningen的project.clj中可以，但现在也不鼓励使用它，而是更推荐read-eval
;read-eval是另一个reader macro，写法是#=，它可以使代码在read的时候被解析
(read-string "#=(+ 1 2)")
;3

;还有一种常用的语法叫unquote-splicing，写法是~@，对应clojure.core/unquote-splicing
;假设你想要把一个未知长度的list放入另一个list，应该怎么做？
(def other-numbers '(4 5 6 7 8))
;尝试用unquote，但不起作用
`(1 2 3 ~other-numbers 9 10)
;(1 2 3 (4 5 6 7 8) 9 10)
;使用clojure的concat
(concat '(1 2 3) other-numbers '(9 10))
;(1 2 3 4 5 6 7 8 9 10)
;但concat无法在syntax-quote中实现相同的效果，这种情况就要使用unquote-splicing
`(1 2 3 ~@other-numbers 9 10)
;(1 2 3 4 5 6 7 8 9 10)
;clojure的reader会在syntax-quote中查找clojure.core/unquote，如果找到会进行unquote-splicing，类似unquote
`(1 2 3 (clojure.core/unquote-splicing other-numbers) 9 10)
;(1 2 3 4 5 6 7 8 9 10)

;syntax-quote还会把其中的symbol加上在当前上下文对应的namespace
;这个特性也是很有用的，这样可以避免在不同的namespace的时候symbol解析为不同的值
;不适应syntax-quote来写一个宏
(defmacro squares
  [xs]
  (list 'map '#(* % %) xs))
(squares [1 2 3])
;(1 4 9)
(comment
  (ns foo
    (:refer-clojure :exclude [map]))
  (in-ns 'foo)
  (def map {:a 1 :b 2})
  (mm.ch2.advanced-macro/squares [1 2 3])
  ;[1 2 3]
  ;因为map解析为了一个map而不是函数，所以map的第一个函数参数变成了要查找的key，第二个参数变成了找不到key的时候返回的默认值
  )
(in-ns 'mm.ch2.advanced-macro)
;使用syntax-quote实现这个宏
(defmacro squares
  [xs]
  `(map #(* % %) ~xs))
(squares [1 2 3])
;(1 4 9)
(comment
  (ns foo
    (:refer-clojure :exclude [map]))
  (in-ns 'foo)
  (def map {:a 1 :b 2})
  (mm.ch2.advanced-macro/squares [1 2 3])
  ;(1 4 9)
  )

;syntax-quote为symbol加上namespace的特性有时候也会引发问题
;如果你在syntax-quote值使用fn来定义函数
(defmacro squares [xs] `(map (fn [x] (* x x)) ~xs))
(comment
  (squares (range 10))
  ;failed: Extra input at: [:fn-tail :arity-1 :params] spec: :clojure.core.specs.alpha/param-list
  ;mm.ch2.advanced-macro/x
  ;原因是因为fn的参数x加上了namespace，而当前的namespace是没有x这个var的
  )
;可以使用~'来防止syntax-quote加上namespace
`(* ~'x ~'x)
;(clojure.core/* x x)
(defmacro squares [xs] `(map (fn [~'x] (* ~'x ~'x)) ~xs))
(squares (range 10))
;(0 1 2 3 4 5 6 7 8 9)
