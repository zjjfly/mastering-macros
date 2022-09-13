(ns com.github.zjjfly.mm.ch4.cleanup-resource
  (:import (java.io FileInputStream)))

;macro的还有一个作用是进行资源的清理，如关闭文件和输入输出流等
;现有的macro，with-open，就是实现了这个功能
(defn read-file [^String file-path]
  (let [buffer (byte-array 1024)
        contents (StringBuilder.)]
    (with-open [file-stream (FileInputStream. file-path)]
      (while (not= -1 (.read file-stream buffer))
        (.append contents (String. buffer))))
    (str contents)))

(read-file "deps.edn")
