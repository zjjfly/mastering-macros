(ns mm.ch4.eval-in-context)

;macro可以决定要怎么解析或是否解析传入的表达式

;comment实际是一个macro，它接受任意的表达式，但不做任何处理，也不会解析表达式
(comment
  (println "Hi"))
;nil

;dosync也是一个macro，作为STM的入口
(def ant-1 (ref {:id 1 :x 0 :y 0}))
(def ant-2 (ref {:id 2 :x 10 :y 10}))
(dosync
  (alter ant-1 update-in [:x] inc)
  (alter ant-1 update-in [:y] inc)
  (alter ant-2 update-in [:x] dec)
  (alter ant-2 update-in [:y] dec))

;future这个macro可以让表达式在Future中解析
(def f (future (Thread/sleep 5000)
               (println "done!")
               (+ 41 1)))
@f
;done!
;=> 42
;阅读future的源码，会发现一段这样的代码：
;(^{:once true} fn* [] ~@body)
;其中的{:once true}表示在函数执行一次之后，会把其中使用的closed-over locals回收
(let [x :a
      f (^:once fn* [] (println x))]
  (f)
  (f))
;:a
;nil
(let [x :a
      f (fn [] (println x))]
  (f)
  (f))
;:a
;:a
