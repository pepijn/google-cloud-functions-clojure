(ns nl.epij.gcp.gcf.run-test
  (:require [clojure.test :refer [deftest is]])
  (:require [nl.epij.gcp.gcf.run :as run]
            [clojure.java.io :as io]
            [badigeon.classpath :as classpath])
  (:import [java.nio.file Files Path]
           [java.nio.file.attribute FileAttribute]
           [java.util.zip ZipFile]
           [java.io File]))

(defn compiled-java!
  [body]
  (let [tmp-dir        ^Path (Files/createTempDirectory "gcf-ring" (make-array FileAttribute 0))
        class-path     (classpath/make-classpath {:aliases [:test]})
        compiled-files #(->> (.toFile tmp-dir) (.listFiles))]
    (try
      (run/compile-javac! {:src-dir       "../example/src/java"
                           :compile-path  tmp-dir
                           :javac-options ["-cp" class-path]})
      (body (.toFile tmp-dir))
      (finally (run! io/delete-file (compiled-files))
               (Files/delete tmp-dir)))))

(deftest java-compilation
  (is (= (compiled-java! (fn [dir] (->> dir (.listFiles) (mapv #(.getName %)))))
         ["JsonHttpEcho.class"])))

(defn compiled-entrypoint-jar!
  [body]
  (compiled-java!
   (fn [compile-path]
     (let [tmp-dir   ^Path (Files/createTempDirectory "gcf-ring" (make-array FileAttribute 0))
           artifacts #(->> (.toFile tmp-dir) (.listFiles))
           jar-path  (io/file (.toFile tmp-dir) "entrypoint.jar")]
       (try (run/entrypoint-jar2! {:out-path     (str jar-path)
                                   :compile-path (str compile-path)})
            (body jar-path)
            (finally (run! io/delete-file (artifacts))
                     (Files/delete tmp-dir)))))))

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

(comment

 (def zipf (compiled-entrypoint-jar! (fn [file] (ZipFile. file))))

 (iterator-seq (.entries zipf))

 )
