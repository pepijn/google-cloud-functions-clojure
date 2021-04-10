(ns nl.epij.gcp.gcf.run-test
  (:require [clojure.test :refer [deftest is]])
  (:require [nl.epij.gcp.gcf.run :as run]
            [clojure.java.io :as io]
            [badigeon.classpath :as classpath])
  (:import [java.nio.file Files Path]
           [java.nio.file.attribute FileAttribute]
           [java.util.zip ZipFile]
           [java.io File]))

(defn delete-dir!
  [dir]
  (run! io/delete-file (->> (.toFile dir) (.listFiles)))
  (Files/delete dir))

(defn with-tmp-dir
  [body]
  (let [tmp-dir ^Path (Files/createTempDirectory "gcf-ring" (make-array FileAttribute 0))]
    (try (body tmp-dir)
         (finally (delete-dir! tmp-dir)))))

(defn compiled-java!
  [body]
  (with-tmp-dir
   (fn [java-compile-dir]
     (let [class-path (classpath/make-classpath {:aliases [:example]})]
       (try (run/compile-javac! {:src-dir       "../example/src/java"
                                 :compile-path  java-compile-dir
                                 :javac-options ["-cp" class-path]})
            (body (.toFile java-compile-dir)))))))

(deftest java-compilation
  (is (= (compiled-java! (fn [dir] (->> dir (.listFiles) (mapv #(.getName %)))))
         ["JsonHttpEcho.class"])))

(defn files-in-zip
  [^ZipFile f]
  (->> f (.entries) iterator-seq (into #{} (map str))))

(defn compiled-entrypoint-uberjar!
  [body]
  (with-tmp-dir
   (fn [tmp-dir]
     (compiled-java!
      (fn [compile-path]
        (let [jar-path (io/file (.toFile tmp-dir) "uberjar.jar")
              options  '{:entrypoint JsonHttpEcho}]
          (run/build-jar! (merge options
                                 {:out-path     (str jar-path)
                                  :aliases      [:example]
                                  :compile-path compile-path}))
          (body jar-path)))))))

(deftest entrypoint-uberjar-generation
  (let [files (files-in-zip (compiled-entrypoint-uberjar! #(ZipFile. ^File %)))]
    (is (contains? files "JsonHttpEcho.class"))
    (is (contains? files "nl/epij/gcf/example__init.class"))))
