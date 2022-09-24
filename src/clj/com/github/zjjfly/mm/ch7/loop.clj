(ns com.github.zjjfly.mm.ch7.loop)

(defmacro my-while
  [test & body]
  `(loop []
     (when ~test
       ~@body
       (recur))))

(let [a (atom 0)]
  (my-while (< @a 10)
            (println @a)
            (swap! a inc)))

(defmacro do-while
  [test & body]
  `(loop []
     ~@body
     (when ~test (recur))))

(defn play-game [secret]
  (let [guess (atom nil)]
    (do-while (not= (str secret) (str @guess))
              (print "Guess the secret I'm thinking: ")
              (flush)
              (reset! guess (read-line)))
    (println "You got it!")))
(comment
  (play-game "xyz")
  )
