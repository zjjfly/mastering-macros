(ns mm.ch8.code-walking
  (:require [clojure.java.io :as jio]
            [clojure.walk :as cw]
            [riddley.walk :as rw]
            [clojure.repl :as repl])
  (:import (java.io IOException)))

;ruby中允许任何方法，类型或模块定义作为一个隐式的begin（类似Java的try）
;在clojure中没有这一特性，但可以用macro来实现implicit try
(defmacro with-implicit-try [& defns]
  `(do
     ~@(map
         (fn [defn-expression]
           (let [initial-args (take 3 defn-expression)
                 body (drop 3 defn-expression)]
             `(~@initial-args (try ~@body))))
         defns)))

(with-implicit-try
  (defn delete-file [path]
    (jio/delete-file path)
    (catch IOException e false)))

(comment
  ;删除一个不存在的文件
  (delete-file "foo.txt")
  ;false
  ;符合预期
  )

;但目前的实现只能适用于函数定义，如果需要实现一个比较全面的版本，需要使用code walking
;目前有很多这方面的库，对于简单的需求，可以使用clojure.walk
(cw/macroexpand-all '(let [when :now] (when {:now "Go!"})))
;(let* [when :now] (if {:now "Go!"} (do)))

;Riddley相比clojure.walk更强大，这在之前的章节提到过
(rw/macroexpand-all '(let [when :now] (when {:now "Go!"})))
;(let* [when :now] (when {:now "Go!"}))

;riddley提供了一个函数riddley.walk/walk-exprs，可以在某个表达式匹配predicate的时候对其进行替换，它会进行macro的展开
(defn malkovichify [expression]
  (rw/walk-exprs
    symbol?
    (constantly 'malkovich)
    expression))

(malkovichify '(println a b))
;(malkovich malkovich malkovich)

;还要考虑special form的情况
(repl/source special-symbol?)
;(defn special-symbol?
;  "Returns true if s names a special form"
;  {:added "1.0"
;   :static true}
;  [s]
;    (contains? (. clojure.lang.Compiler specials) s))
(sort (keys clojure.lang.Compiler/specials))
;(&
; .
; case*
; catch
; def
; deftype*
; do
; finally
; fn*
; if
; let*
; letfn*
; loop*
; monitor-enter
; monitor-exit
; new
; quote
; recur
; reify*
; set!
; throw
; try
; var
; clojure.core/import*)

;使用riddley实现implicit try
(declare add-try)

(defn should-transform?
  [exp]
  (and (seq? exp)
       (#{'fn* 'do 'loop* 'let* 'letfn* 'reify*} (first exp))))

(defn wrap-fn-body [[bindings & body]]
  (list bindings (cons 'try body)))

(defn- wrap-bindings [bindings]
  (->> bindings
       (partition-all 2)
       (mapcat
         (fn [[k v]]
           (let [[k v]
                 [k (add-try v)]]
             [k v])))
       vec))

(defn- wrap-fn-decl [clauses]
  (let [[name? args? fn-bodies]
        (if (symbol? (first clauses))
          (if (vector? (second clauses))
            [(first clauses) (second clauses) (drop 2 clauses)]
            [(first clauses) nil (doall (map wrap-fn-body (rest clauses)))])
          [nil nil (doall (map wrap-fn-body clauses))])]
    (cond->> fn-bodies
             (and name? args?) (#(list (cons 'try %)))
             (not (and name? args?)) (map add-try)
             args? (cons args?)
             name? (cons name?))))

(defn- wrap-let-like [expression]
  (let [[verb bindings & body] expression]
    `(~verb ~(wrap-bindings bindings)
       (try ~@(add-try body)))))

(defn transform [x]
  (condp = (first x)
    'do (let [[_ & body] x]
          (cons 'try (add-try body)))
    'loop* (wrap-let-like x)
    'let* (wrap-let-like x)
    'letfn* (wrap-let-like x)
    'fn* (let [[verb & fn-decl] x]
           `(fn* ~@(wrap-fn-decl fn-decl)))
    'reify* (let [[verb options & fn-decls] x]
              `(~verb ~options ~@(map wrap-fn-decl fn-decls)))
    x))

(defn add-try [expression]
  (rw/walk-exprs should-transform? transform expression))

(defmacro with-implicit-try [& body]
  (cons 'try (map add-try body)))

(rw/macroexpand-all '(with-implicit-try
                       (defn delete-file [path]
                         (jio/delete-file path)
                         (catch IOException e false))))

(rw/macroexpand-all
  '(with-implicit-try (defn foo []
                        (let [x 1
                              y 2]
                          [x y]))))

;有一些函数的meta data里有:inline这个keyword，它会使函数在运行时使用:inline对应的表达式进行内联
;所以，如果with-redefs要替换有:inline的函数是行不通的
