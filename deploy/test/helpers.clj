(ns helpers
  (:require [clojure.java.io :as io])
  (:import [java.nio.file Files Path]
           [java.util.zip ZipFile]
           [java.nio.file.attribute FileAttribute]
           [java.io File]))

(defn delete-dir!
  [dir]
  (run! io/delete-file (->> (.toFile dir) (.listFiles)))
  (Files/delete dir))

(defn with-tmp-dir
  [body]
  (let [tmp-dir ^Path (Files/createTempDirectory "gcf-ring" (make-array FileAttribute 0))]
    (try (body (.toFile tmp-dir))
         (finally (delete-dir! tmp-dir)))))

(defn zip-file
  [file]
  (ZipFile. ^File file))

(defn files-in-zip
  [^ZipFile f]
  (->> f (.entries) iterator-seq (into #{} (map str))))

(defn list-files
  [dir]
  (->> dir (.listFiles) (mapv #(.getName %))))
