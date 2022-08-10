(ns build
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [clojure.tools.build.api :as b])
  (:import (java.io File)))

(def lib ::clj-template)
;(def version (format "1.2.%s" (b/git-count-revs nil)))
(def version "1.0")
(def clj-source "src/clj")
(def java-source "src/java")
(def resources "src/resources")
(def test-clj-source "test/clj")
(def test-java-source "test/java")
(def test-resources "test/resources")
(def target-dir "target")
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def jar-file (format "target/%s-%s.jar" (name lib) version))
(def uber-file (format "target/%s-%s-standalone.jar" (name lib) version))

(defn java-file-exist?
  [dir]
  (let [files (file-seq (io/file dir))]
    (some #(s/ends-with? (.getName %) ".java")
          (filter #(.isFile %) files))))

(defn init
  "init project structure,create necessary directories"
  [_]
  (let [source-dirs [clj-source java-source resources]
        test-dirs [test-clj-source test-java-source test-resources]
        all-dirs (concat source-dirs test-dirs)]
    (doseq [^File f (map io/file all-dirs)]
      (when (not (.exists f))
        (.mkdirs f)))))

(defn clean
  "Delete the build target directory"
  [_]
  (println (str "Cleaning " target-dir))
  (b/delete {:path target-dir}))

(defn prep
  "prepare for building"
  [_]
  (init _)
  (b/write-pom {:class-dir class-dir
                :lib       lib
                :version   version
                :basis     basis
                :src-dirs  [java-source clj-source]})
  (b/copy-dir {:src-dirs   [resources]
               :target-dir class-dir}))

(defn compile-java
  "compile java source files"
  [_]
  (when (java-file-exist? java-source)
    (b/javac {:src-dirs   [java-source]
              :class-dir  class-dir
              :basis      basis
              :javac-opts ["-source" "8" "-target" "8"]})))

(defn compile-clj
  "compile clojure source files"
  [_]
  (b/compile-clj {:basis     basis
                  :src-dirs  [clj-source]
                  :class-dir class-dir}))

(defn compile-all
  "compile all source files"
  [_]
  (compile-java _)
  (compile-clj _))

(defn jar
  "package jar file"
  [_]
  (clean _)
  (prep _)
  (compile-all _)
  (b/jar {:class-dir class-dir
          :jar-file  jar-file
          :main      nil}))

(defn uber
  "package uberjar file"
  [_]
  (clean _)
  (prep _)
  (compile-all _)
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis     basis}))
