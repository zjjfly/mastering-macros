(ns mm.ch4.code-wrapper
  (:require [clojure.java.io :as io])
  (:import (java.io File)
           (java.text SimpleDateFormat)
           (java.util Date)))

;第一种类型的macro：对传入的表达式进行wrap，并在前后加一些的处理
;这种类型的macro主要是为了避免写很多boilerplate code

;在Clojure社区比较喜欢词法作用域，即符号的值由其所处的代码位置决定，而不是在调用栈的位置
;所以，一般会让函数尽可能用参数接收函数中需要的值。词法绑定的典型例子是函数参数，let和loop绑定
(defn circle-area [radius]
  (* Math/PI (* radius radius)))

(circle-area 10.0)
;314.1592653589793

;与词法作用域相反的是动态作用域，即函数中需要的值是从函数外部注入的，典型例子是clojure的以类似*foo*这种名称命名的变量
(declare ^:dynamic *radius*)
(defn circle-area []
  (* Math/PI (* *radius* *radius*)))
;使用动态绑定改变动态变量的值（只在单个线程起作用）
(binding [*radius* 5.0] (circle-area))
;78.53981633974483

;虽然习惯是使用词法作用域，但动态作用域也有其作用，当要经过一系列函数来传递某些值到比较底层的函数的时候
;底层函数的一个例子是println函数，它的里面会把输出打印到*out*这个动态变量
(defn log [message]
  (let [timestamp (.format (SimpleDateFormat. "yyyy-MM-dd 'T' HH:mmZ") (Date.))]
    (println timestamp "[INFO]" message)))

(defn process-events [events]
  (doseq [event events]
    ;; do some meaningful work based on the event
    (log (format "Event %s has been processed" (:id event)))))

;使用动态绑定来改变输出的对象
(let [file (File. (System/getProperty "user.home") "event-stream.log")]
  (with-open [file (io/writer file :append true)]
    (binding [*out* file]
      (process-events [{:id 88896} {:id 88898}]))))

;但如果我们想要改成别的地方，就想要重新写一遍上面的代码，这是不可接受的
;为此，可以写一个macro来实现
(defmacro with-out-file
  [file & body]
  `(with-open [writer# (io/writer ~file :append true)]
     (binding [*out* writer#]
       ~@body)))

(let [file (File. (System/getProperty "user.home") "event-stream.log")]
  (with-out-file file
                 (process-events [{:id 88894} {:id 88895} {:id 88897}])
                 (process-events [{:id 88896} {:id 88898}])))

;clojure内置的macro也有和with-out-file类似的，如with-out-str，它可以把传入的表达式中所有输出到*out*的
;内容作为一个字符串返回，这在单元测试的时候比较有用
(with-out-str
  (print "Hi"))
;"Hi"
;与之相对的还有with-in-str，它把一个字符串作为输入，并把*in*绑定到这个字符串的Reader，这在单元测试中也很有用
(with-in-str "x"
             (read-line))
;"x"

;使用函数来实现with-out-file
(defn with-out-file
  [file body-fn]
  (with-open [writer (io/writer file :append true)]
    (binding [*out* writer]
      (body-fn))))

(let [file (File. (System/getProperty "user.home") "event-stream.log")]
  (with-out-file file
                 (fn []
                   (process-events [{:id 88894} {:id 88895} {:id 88897}])
                   (process-events [{:id 88896} {:id 88898}]))))

;还有一种实现是把函数和macro结合起来
(defn with-out-file-fn [file body-fn]
  (with-open [writer (io/writer file :append true)]
    (binding [*out* writer] (body-fn))))

(defmacro with-out-file
  [file & body]
  `(with-out-file-fn ~file (fn [] ~@body)))

(let [file (File. (System/getProperty "user.home") "event-stream.log")]
  (with-out-file file
                 (process-events [{:id 88894} {:id 88895} {:id 88897}])
                 (process-events [{:id 88896} {:id 88898}])))
;综合看来，第三种方式是最好的，它即让macro更加简洁，也让用户可以在需要的时候使用函数版本
