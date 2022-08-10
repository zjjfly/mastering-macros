(ns com.github.zjjfly.mm.ch1.macroexpansion)

;clojure有专门的工具可以进行宏展开
;macroexpand-1是其中最简单的一个，它是一个普通函数，所以未来防止它的参数被提前解析，需要进行quote
(macroexpand-1 '(when (= 1 2) (println "math is broken")))
;(if (= 1 2) (do (println "math is broken")))

;macroexpand-1只接受是宏调用的表达式，否则不起作用，只会返回原来传入的参数
(macroexpand-1 nil)
;nil
(macroexpand-1 '(+ 1 2))
;(+ 1 2)

(defn macro? [x]
  (:macro (meta (resolve x))))

;重写when这个宏，故意漏写'if
(defmacro broken-if
  [test & body]
  (list test (cons 'do body)))
(comment
  ;使用这个有问题宏，很显然会报错
  (broken-if (= 1 1)
             (println "Math works!"))
  ;class java.lang.Boolean cannot be cast to class clojure.lang.IFn
  )

;使用macroexpand-1来展开宏
(macroexpand-1 '(broken-if (= 1 1)
                           (println "Math works!")))
;((= 1 1) (do (println "Math works!")))

;还有一个函数macroexpand，它类似macroexpand-1，不同的是：如果上一次展开的结果仍是宏调用，macroexpand会继续进行展开
;注意，不论是macroexpand-1还是macroexpand，只对最外层的宏调用进行展开，如果其中有子表达式中调用了宏，不会被展开
(defmacro when-falsy
  [test & body]
  (list 'when (list 'not test)
        (cons 'do body)))


(macroexpand-1 '(when-falsy (= 1 2)
                            (println "hi!")))
;(when (not (= 1 2)) (do (println "hi!")))
(macroexpand '(when-falsy (= 1 2)
                          (println "hi!")))
;(if (not (= 1 2)) (do (do (println "hi!"))))
