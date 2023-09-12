(ns mm.ch8.error-handling
  (:require [clojure.tools.macro :as m]
            [mm.ch8.pattern-matching :as pm]
            [n01se.syntax.repl :as syntax]
            [clojure.repl :as repl]))

;为了提供macro给其他用户使用，并尽量的用户友好，需要为macro写doc，并提供容易理解的错误信息
;以之前实现的模式匹配为例来演示如何改善错误信息

;假设要使用match来匹配keyword
(comment
  (pm/match :a
            :b true
            :c false)
  ;Unexpected error (UnsupportedOperationException) macroexpanding pm/match.
  ;count not supported on this type: Keyword
  )
;可以看出错误信息会让使用者一头雾水，如果不看源码，不会知道这个macro只支持vector的匹配
;为此，我们可以加入对参数的检查
(defmacro match [input & more]
  {:pre [(vector? input)]}
  (let [clauses (partition 2 more)]
    `(cond ~@(mapcat (partial pm/match-clause input) clauses))))
(comment
  (match :a
         :b true
         :c false)
  ;Unexpected error (AssertionError) macroexpanding match.
  ;Assert failed: (vector? input)
  )
;看上去好了很多，但如果用户输入的匹配子句不是成对的呢？你不得不在加上一个前置条件
(defmacro match [input & more]
  {:pre [(vector? input)
         (even? (count more))]}
  (let [clauses (partition 2 more)]
    `(cond ~@(mapcat (partial pm/match-clause input) clauses))))
;但match这个macro比较简单，分支也很少，如果是一个更复杂的macro呢？
;假设我们实现一个会给函数假设默认doc的defn
(defn- default-docstring
  [name]
  (str "The author carelessly forgot to provide a docstring for `"
       name
       "`. Sorry!"))
(defmacro my-defn
  [name & body]
  (let [[name args] (m/name-with-attributes name body)
        name (if (:doc (meta name))
               name
               (vary-meta name assoc :doc (default-docstring name)))]
    `(defn ~name ~@args)))
(my-defn foo [])
(repl/doc foo)
;-------------------------
;mm.ch8.error-handling/foo
;([])
;  The author carelessly forgot to provide a docstring for `foo`. Sorry!

;name-with-attributes是tools.macro提供的一个强大的函数，它还提供了其他一些有用的工具
;macrolet,类似letfn，定义一个局部的macro
(m/macrolet [(do-twice [form] `(do ~form ~form))]
            (do-twice (println "hi")))
;hi
;hi

;defsymbolmacro，定义symbol macro
(m/defsymbolmacro hi
                  (println "hi"))
(m/with-symbol-macros
  hi)
;hi

;symbol-macrolet,可以用定义local的symbol macro（展开的时候symbol会被特定的表达式替换）
(let [counter (atom 0)]
  (m/symbol-macrolet [bump! (swap! counter inc)]
                     bump!
                     bump!
                     bump!
                     @counter))
;3

;类似macroexpand-1和macroexpand的函数，但可以展开symbol macro
(m/mexpand-1 hi)
;(println "hi")
(m/mexpand hi)
;(println "hi")

;mexpand-all，比clojure.walk/macroexpand-all更robust的实现，同样支持symbol macro
(m/mexpand-all '(match [0 1]
                       [0 y] {:axis "Y" :y y}
                       [x 0] {:axis "X" :x x}
                       [x y] {:x x :y y}
                       :else hi))
;(if
; (let*
;  [and__5579__auto__ (clojure.core/= (clojure.core/count [0 1]) 2)]
;  (if
;   and__5579__auto__
;   (clojure.core/every?
;    clojure.core/identity
;    (clojure.core/map mm.ch8.pattern-matching/match-item? (quote [0 y]) [0 1]))
;   and__5579__auto__))
; (let* [y 1] {:axis "Y", :y y})
; (if
;  (let*
;   [and__5579__auto__ (clojure.core/= (clojure.core/count [0 1]) 2)]
;   (if
;    and__5579__auto__
;    (clojure.core/every?
;     clojure.core/identity
;     (clojure.core/map mm.ch8.pattern-matching/match-item? (quote [x 0]) [0 1]))
;    and__5579__auto__))
;  (let* [x 0] {:axis "X", :x x})
;  (if
;   (let*
;    [and__5579__auto__ (clojure.core/= (clojure.core/count [0 1]) 2)]
;    (if
;     and__5579__auto__
;     (clojure.core/every?
;      clojure.core/identity
;      (clojure.core/map mm.ch8.pattern-matching/match-item? (quote [x y]) [0 1]))
;     and__5579__auto__))
;   (let* [x 0 y 1] {:x x, :y y})
;   (if :else (println "hi") nil))))

;

;my-defn的可能出现的错误就有很多，用户可能会在函数名，doc，参数列表，metadata map等等地方犯错
;显然，这些错误是无法枚举的，所以需要使用被称为启发式macro的技术

;在刚刚学习使用内置的defn定义函数的时候会犯的一些错误：
(comment
  (defn "Squares a number" square [x] (* x x))
  ;Syntax error macroexpanding clojure.core/defn at (src/clj/com/github/zjjfly/mm/ch8/error_handling.clj:131:3).
  ;"Squares a number" - failed: simple-symbol?
  )
(comment
  (defn square-pair
    [(x y)]
    (list (* x x) (* y y)))
  ;Syntax error macroexpanding clojure.core/defn at (src/clj/com/github/zjjfly/mm/ch8/error_handling.clj:136:3).
  ;(x y) - failed: vector? at: [:fn-tail :arity-n :bodies :params] spec: :clojure.core.specs.alpha/param-list
  ;((x y)) - failed: Extra input at: [:fn-tail :arity-1 :params] spec: :clojure.core.specs.alpha/param-list
  )

;使用seqex来的defn
(syntax/defn "Squares a number" square [x] (* x x))
;syndoc可以生成类似BNF的语法定义
(syntax/syndoc syntax/defn)
