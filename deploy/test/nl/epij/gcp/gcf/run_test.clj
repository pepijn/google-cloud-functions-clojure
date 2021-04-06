(ns nl.epij.gcp.gcf.run-test
  (:require [clojure.test :refer [deftest is]])
  (:require [nl.epij.gcp.gcf.run :as run]
            [clojure.java.io :as io]
            [badigeon.classpath :as classpath]
            [clojure.tools.deps.alpha :as deps]
            [clojure.string :as str])
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

(defn compiled-entrypoint-jar!
  [body]
  (with-tmp-dir
   (fn [tmp-dir]
     (compiled-java!
      (fn [compile-path]
        (let [jar-path (io/file (.toFile tmp-dir) "entrypoint.jar")]
          (run/entrypoint-jar2! {:out-path     (str jar-path)
                                 :compile-path (str compile-path)})
          (body jar-path)))))))

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
  (with-tmp-dir
   (fn [tmp-dir]
     (compiled-java!
      (fn [compile-path]
        (let [jar-path (io/file (.toFile tmp-dir) "uberjar.jar")
              options  '{:entrypoint JsonHttpEcho}]
          (run/entrypoint-uberjar2! (merge options
                                           {:out-path     (str jar-path)
                                            :aliases      [:example]
                                            :compile-path compile-path}))
          (body jar-path)))))))

(deftest entrypoint-uberjar-generation
  (is (= (count (files-in-zip (compiled-entrypoint-uberjar! #(ZipFile. ^File %))))
         21114)))
(comment

 (let [config '{:nl.epij.gcp.gcf/entrypoint       JsonHttpEcho
                :nl.epij.gcp.gcf/entrypoint-src   ["../example/src/java"]
                :nl.epij.gcp.gcf/entrypoint-alias [:example]}

       {:nl.epij.gcp.gcf/keys [entrypoint
                               entrypoint-src
                               entrypoint-alias]}
       config
       ]
   (run/compile-javac! (conj {:src-dir entrypoint-src}
                             (when-not (empty? entrypoint-alias) {:javac-options ["-cp"]}))))

 )
