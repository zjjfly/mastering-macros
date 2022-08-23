(ns com.github.zjjfly.mm.ch2.macro-hygiene)

;宏卫生问题，指的是在宏中把在调用宏的表达式中使用的一些symbol覆盖率，使其指向了别的值
;例子：
(defmacro make-adder [x] `(fn [~'y] (+ ~x ~'y)))
(macroexpand-1 '(make-adder 10))
;(macroexpand-1 '(make-adder 10))
(def y 100)
;假设想要使用上面定义的y来调用宏，会发现结果出乎预料
((make-adder (+ y 3)) 5)
;13，不是预想的108
;把宏展开，会发现出问题的原因
(macroexpand-1 '(make-adder (+ y 3)))
;(macroexpand-1 '(make-adder (+ y 3)))
;y被宏中定义的函数的参数shadow（or capture）了，

;解决这个问题的方式其实也很简单，就是在定义宏的时候，不使用这些容易和其他var冲突的命名，而是用算法随机生成一个较复杂的名称
;clojure提供了这样的一个函数gensym用来生成随机的名称
(gensym)
;G__4649
(gensym)
;G__4659
(gensym "xyz")
;xyz4654
(gensym "xyz")
;xyz4664
;使用gensym重新定义make-adder
(defmacro make-adder [x]
  (let [y (gensym)]
    `(fn [~y] (+ ~x ~y))))
((make-adder (+ y 3)) 5)
;108
;但这样的写法还是有点啰嗦，因为clojure提供了一种简写方式，只要在symbol后加上#号就可以，叫做auto-gensym
(defmacro make-adder
  [x]
  `(fn [y#]
     (+ ~x y#)))
((make-adder (+ y 3)) 5)
;108

;还有其他几种情况会使用auto-gensym
;在catch中
(defmacro safe-math-expression?
  [expression]
  `(try ~expression
        true
        (catch ArithmeticException e# false)))
;在let或letfn中
(defmacro my-and
  ([] true)
  ([x] x)
  ([x & next]
   `(let [and# ~x]
      (if and# (my-and ~@next) and#))))
