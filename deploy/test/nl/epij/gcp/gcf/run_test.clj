(ns nl.epij.gcp.gcf.run-test
  (:require [clojure.test :refer [deftest is]])
  (:require [nl.epij.gcp.gcf.run :as run]
            [clojure.java.io :as io]
            [badigeon.classpath :as classpath]
            [clojure.tools.deps.alpha :as deps])
  (:import [java.nio.file Files Path]
           [java.nio.file.attribute FileAttribute]
           [java.util.zip ZipFile]
           [java.io File]))

(defn delete-dir!
  [dir]
  (run! io/delete-file (->> (.toFile dir) (.listFiles)))
  (Files/delete dir))

(defn compiled-java!
  [body]
  (let [tmp-dir        ^Path (Files/createTempDirectory "gcf-ring" (make-array FileAttribute 0))
        class-path     (classpath/make-classpath {:aliases [:example]})]
    (try
      (run/compile-javac! {:src-dir       "../example/src/java"
                           :compile-path  tmp-dir
                           :javac-options ["-cp" class-path]})
      (body (.toFile tmp-dir))
      (finally (delete-dir! tmp-dir)))))

(deftest java-compilation
  (is (= (compiled-java! (fn [dir] (->> dir (.listFiles) (mapv #(.getName %)))))
         ["JsonHttpEcho.class"])))

(defn compiled-entrypoint-jar!
  [body]
  (compiled-java!
   (fn [compile-path]
     (let [tmp-dir   ^Path (Files/createTempDirectory "gcf-ring" (make-array FileAttribute 0))
           jar-path  (io/file (.toFile tmp-dir) "entrypoint.jar")]
       (try (run/entrypoint-jar2! {:out-path     (str jar-path)
                                   :compile-path (str compile-path)})
            (body jar-path)
            (finally (delete-dir! tmp-dir)))))))

(defn files-in-zip
  [^ZipFile f]
  (->> f (.entries) iterator-seq (mapv str)))

(deftest entrypoint-jar-generation
  (is (= (files-in-zip (compiled-entrypoint-jar! #(ZipFile. ^File %)))
         ["META-INF/MANIFEST.MF"
          "META-INF/badigeon/entrypoint/entrypoint/deps.edn"
          "META-INF/maven/entrypoint/entrypoint/pom.xml"
          "JsonHttpEcho.class"
          "META-INF/maven/entrypoint/entrypoint/pom.properties"])))

(defn compiled-entrypoint-uberjar!
  [body]
  (compiled-java!
   (fn [compile-path]
     (let [tmp-dir   ^Path (Files/createTempDirectory "gcf-ring" (make-array FileAttribute 0))
           jar-path  (io/file (.toFile tmp-dir) "entrypoint-uberjar.jar")
           deps      (deps/slurp-deps (io/file "../example/deps.edn"))
           options   (get-in deps [:aliases :run-local :exec-args])]
       (try (run/entrypoint-uberjar2! (merge options
                                             {:out-path     (str jar-path)
                                              :aliases      [:example]
                                              :compile-path compile-path}))
            (body jar-path)
            (finally (delete-dir! tmp-dir)))))))

(deftest entrypoint-uberjar-generation
  (is (= (count (files-in-zip (compiled-entrypoint-uberjar! #(ZipFile. ^File %))))
         21113)))
