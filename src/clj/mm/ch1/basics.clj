(ns mm.ch1.basics)

;clojure中的代码即数据，这使得macro成为了可能

;read方法可以把一段代码转成clojure的list，但它的参数是一个input stream。
;使用read-string在read上做了封装，可以使用字符串作为参数
(read-string "(+ 1 2 3 4 5)")
;(+ 1 2 3 4 5)
(class (read-string "(+ 1 2 3 4 5)"))
;clojure.lang.PersistentList

;使用eval可以把一个list作为一段代码执行
(eval (read-string "(+ 1 2 3 4 5)"))
;15
(class (eval (read-string "(+ 1 2 3 4 5)")))
;java.lang.Long
(= (+ 1 2 3 4 5) (eval (read-string "(+ 1 2 3 4 5)")))
;true

;repl和完整的clojure程序实质都是像上面的read&eval的方式运行的
;很容易会想到，在read之后eval之前，可以对read得到的数据进行操作，从而做到代码的修改
(let [exp (read-string "(+ 1 2 3 4 5)")]
  (cons (resolve '*)
        (rest exp)))
;(#'clojure.core/* 1 2 3 4 5)
(eval *1)
;120

;使用list表示代码而不是从字符串读取代码
(let [exp (quote (+ 1 2 3 4 5))]
  (cons (resolve '*)
        (rest exp)))
;(#'clojure.core/* 1 2 3 4 5)
(eval *1)
;120

;除了list字面量之外，位于list首位的必须是动词，动词包括函数，特殊形式，宏
;函数的参数在执行函数之前就会被解析，而特殊形式和宏则按照它们自己的规则，这是它们最重要的区别
;quote就是一种特殊形式，所以之前的代码中传递给quote的list没有先被解析

;在quote表达式中，被圆括号，方括号或花括号包括的代码不会解析
;但如果是数字，字符串，keyword，symbol，则它们在read之后其实就已经算是解析过了
(quote 1)
;1
(quote "xyz")
;"xyz"
(quote :thx)
;:thx
(quote thx)
;thx

;clojure为quote提供了一个reader macro来简化它的写法
'(+ 1 2 3 4 5)
;(+ 1 2 3 4 5)
'map
;map
