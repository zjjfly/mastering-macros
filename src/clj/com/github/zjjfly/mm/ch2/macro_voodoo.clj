(ns com.github.zjjfly.mm.ch2.macro-voodoo
  (:require [clojure.pprint :as pp]))

;clojure的macro中有两个默认的值：&env，&from
;写一个macro来看一下它们里面的内容
(defmacro info-about-caller
  []
  (pp/pprint {:form &form :env &env})
  `(println "macro was called!"))

(info-about-caller)
;{:form (info-about-caller), :env nil}
(let [foo "bar"] (info-about-caller))
;{:form (info-about-caller),
; :env
; {foo
;  #object[clojure.lang.Compiler$LocalBinding 0x18e8bf41 "clojure.lang.Compiler$LocalBinding@18e8bf41"]}}
(let [foo "bar" baz "quux"] (info-about-caller))
;{:form (info-about-caller),
; :env
; {foo
;  #object[clojure.lang.Compiler$LocalBinding 0x188a5511 "clojure.lang.Compiler$LocalBinding@188a5511"],
;  baz
;  #object[clojure.lang.Compiler$LocalBinding 0x1f7676f "clojure.lang.Compiler$LocalBinding@1f7676f"]}}

;可以看出，&env值存放了一个本地变量的map，key是本地变量的名称字符串，value是编译器内部使用的类的实例
;这可以让我们在宏展开的时候访问到本地变量，一般的使用场景是从中取出某个本地变量放到展开的宏中
(defmacro inspect-caller-locals
  []
  (->> (keys &env)
       (map (fn [k] [`'~k k]))
       (into {})))
(let [foo "bar" baz "quux"] (inspect-caller-locals))

;{foo "bar", baz "quux"}
;这里的`'~的实际上等价于(list 'quote k)或`(quote ~k)

;&form是调用这个macro的表达式，已list存放
;这个不常用，因为在宏的声明里实际上已经知道宏的名称和传入的参数
;自己实现一个macro来返回调用它的表达式
(defmacro inspect-called-form
  [& args]
  {:form (list 'quote (cons 'inspect-called-form args))})
(inspect-called-form 1 2 3)
;{:form (inspect-called-form 1 2 3)}
;使用&form来实现
(defmacro inspect-called-form
  [& args]
  {:form (list 'quote &form)})
(inspect-called-form 1 2 3)

;&form真正的价值是它保留了表达式的元数据
(def form ^{:doc "this is good stuff"} (inspect-called-form 1 2 3))
(meta (:form form))
;{:line 1, :column 155, :doc "this is good stuff"}
