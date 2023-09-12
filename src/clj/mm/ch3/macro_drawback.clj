(ns mm.ch3.macro-drawback
  (:require [clojure.string :as string]))

;macro的一些缺点

;缺点1.macro不是值
(defmacro square [x] `(* ~x ~x))
(comment
  (map square (range 10))
  ;Can't take value of a macro: #'mm.ch3.macro-drawback/square
  )
;对于这个例子来说，可以用一个匿名函数wrap这个macro
(map #(square %) (range 10))
;(0 1 4 9 16 25 36 49 64 81)
;但并不是所有macro都可以通过这个方式来间接的变为一个值
(defmacro do-multiplication
  [exp]
  (cons '* (rest exp)))
(do-multiplication (+ 1 2))
;2
(comment
  (map #(do-multiplication %) ['(+ 3 4) '(- 2 3)])
  ;Don't know how to create ISeq from: clojure.lang.Symbol
  ;报错的原因是，macro是在编译时展开的，它不会知道在运行时传入匿名函数的是数字还是一个list
  )
;实际上也是有办法把macro当做函数使用的，只需要对其进行deref
(@#'square nil nil 9)                                       ;第一、二个参数是&form和&env
;(clojure.core/* 9 9)
;结果和调用macroexpand-1相似，但一般不推荐这么写

;缺点2.macro有传染性，即会强迫调用它的代码也不得不以macro实现
;例子：
(defmacro log [& args]
  `(println (str "[INFO] " (string/join " : " ~(vec args)))))

(log "that went well")
;[INFO] that went well
(log "item #1 created" "by user #42")
;[INFO] item #1 created : by user #42
(defn send-email [user messages]
  (Thread/sleep 1000))
(def admin-user "kathy@example.com")
(def current-user "colin@example.com")
;现在要实现一个函数来发送邮件给上面两个用户
(comment
  (defn notify-everyone [messages]
    (apply log messages)
    (send-email admin-user messages)
    (send-email current-user messages))
  ;Can't take value of a macro: #'mm.ch3.macro-drawback/log
  ;因为macro无法作为值，所以编译不过
  )
;使用macro来实现这个函数
(defmacro notify-everyone [messages]
  `(do
     (send-email admin-user ~messages)
     (send-email current-user ~messages)
     (log ~@messages)))
(notify-everyone ["item #1 processed" "by worker #72"])
;[INFO] item #1 processed : by worker #72
;这样使用macro来调用macro的方式会由于macro的限制，不断的向上传染，导致代码里有很多macro
;更好的方式是使用函数
(defn log [& args]
  (println (str "[INFO] " (string/join " : " args))))
(log "hi" "there")
;[INFO] hi : there
(apply log ["hi" "there"])
;[INFO] hi : there

;缺点3：写出正确的macro很困难
;假如我们自己实现一个功能和and一样的macro
(defmacro my-and
  ([] true)
  ([x] x)
  ([x & next]
   `(if ~x (my-and ~@next) ~x)))

(my-and)
;true
(my-and (= 1 2))
;false
(my-and true true)
;true
(my-and true false)
;false
(my-and true true false)
;false
(my-and true true nil)
;nil
(my-and 1 2 3)
;3
;上面的几个测试的结果和and的行为一致，但如果传入一个表达式给my-and呢？
(my-and (do (println "hi there") (= 1 2)) (= 3 4))
;hi there
;hi there
;=> false
;打印了两次"hi there"，这显然不是我们期望的，展开看一下造成这个的原因
(macroexpand-1 '(my-and (do (println "hi there") (= 1 2)) (= 3 4)))
;(if
; (do (println "hi there") (= 1 2))
; (mm.ch3.macro-drawback/my-and (= 3 4))
; (do (println "hi there") (= 1 2)))
;可以看出，会解析两次这个表达式，这是写macro很容易犯的一个错误
;在没有明确需要的情况下，要保证只解析一次传入macro的表达式
(defmacro my-and-fixed ([] true)
  ([x] x)
  ([x & next]
   `(let [arg# ~x]
      (if arg# (my-and-fixed ~@next) arg#))))
(my-and-fixed (do (println "hi there") (= 1 2)) (= 3 4))
;hi there
;=> false

;在ClojureScript中，macro会先在JVM中展开，之后才在js引擎中执行
;所以有时候js引擎无法做到的事情也可以通过把这些操作放到macro来实现，虽然这样做太magic了
