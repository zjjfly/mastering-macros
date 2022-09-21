(ns com.github.zjjfly.mm.ch5.demo)

(defn length1 [x] (alength x))

(defn length2 [^bytes x] (alength x))

;use following command to run JMH benchmark test
;clj -X:jmh :format :pprint, :status true
