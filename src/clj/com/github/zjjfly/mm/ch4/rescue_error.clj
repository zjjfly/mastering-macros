(ns com.github.zjjfly.mm.ch4.rescue-error
  (:require [clojure.test :as test]))

;macro可以限制表达式解析的时候抛出的异常的范围
;典型的是clojure.test的is中使用的一个内部macro：try-expr
(test/is (= 1 1))

;使用函数实现try-expr
(defn try-expr [msg f]
  (try (eval (test/assert-expr msg (list f)))
       (catch Throwable t
         (test/do-report {:type     :error, :message msg,
                          :expected f, :actual t}))))
(defn our-is
  ([f] (our-is f nil))
  ([f msg] (try-expr msg f)))

(our-is (fn [] (= 1 1)))
;true

(our-is (fn [] (= 1 2)))
;FAIL in () (rescue_error.clj:20)
;expected: (#object[com.github.zjjfly.mm.ch4.rescue_error$eval3775$fn__3776 0x70ac6f9f "com.github.zjjfly.mm.ch4.rescue_error$eval3775$fn__3776@70ac6f9f"])
;  actual: (not (#object[com.github.zjjfly.mm.ch4.rescue_error$eval3775$fn__3776 0x4118c91a "com.github.zjjfly.mm.ch4.rescue_error$eval3775$fn__3776@4118c91a"]))

;可以看出，使用函数实现的try-expr的错误信息可读性非常差，而使用macro实现，可以很容易的打印出出错的表达式
;但实践中，macro往往是让人困惑的错误信息的源头
