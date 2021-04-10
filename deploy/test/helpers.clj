(ns helpers
  (:require
    [clojure.java.io :as io])
  (:import
    (java.io
      File)
    (java.nio.file
      Files
      Path)
    (java.nio.file.attribute
      FileAttribute)
    (java.util.zip
      ZipFile)))


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
  [^File file]
  (ZipFile. file))


(defn files-in-zip
  [^ZipFile f]
  (->> f (.entries) iterator-seq (into #{} (map str))))


(defn list-files
  [dir]
  (->> dir (.listFiles) (mapv #(.getName %))))
