(ns nl.epij.gcp.gcf.run-test
  (:require [clojure.test :refer [deftest is]])
  (:require [nl.epij.gcp.gcf.run :as run]
            [clojure.java.io :as io]
            [badigeon.classpath :as classpath])
  (:import [java.nio.file Files Path]
           [java.nio.file.attribute FileAttribute]))

(defn compile-java
  []
  (let [tmp-dir        ^Path (Files/createTempDirectory "gcf-ring" (make-array FileAttribute 0))
        class-path     (classpath/make-classpath {:aliases [:test]})
        compiled-files #(->> (.toFile tmp-dir) (.listFiles))]
    (try
      (run/compile-javac! {:src-dir       "../example/src/java"
                           :compile-path  tmp-dir
                           :javac-options ["-cp" class-path]})
      (->> (compiled-files) (mapv #(.getName %)))
      (finally (run! io/delete-file (compiled-files))
               (Files/delete tmp-dir)))))

(deftest java-compilation
  (is (= (compile-java) ["JsonHttpEcho.class"])))

(comment

 )
