(ns nl.epij.gcf.deploy-test
  (:require
    [badigeon.classpath :as classpath]
    [clojure.java.io :as io]
    [clojure.test :refer [deftest is testing]]
    [helpers :refer [with-tmp-dir list-files zip-file files-in-zip]]
    [nl.epij.gcf :as gcf]
    [nl.epij.gcf.deploy :as deploy]))


(defn compiled-java!
  [body]
  (with-tmp-dir
    (fn [java-compile-dir]
      (let [class-path (classpath/make-classpath {:aliases [:example]})]
        (deploy/compile-javac! {::gcf/src-dir       "../example/src/java"
                                ::gcf/compile-path  java-compile-dir
                                ::gcf/javac-options ["-cp" class-path]})
        (body java-compile-dir)))))


(deftest java-compilation
  (is (= (compiled-java! list-files)
         ["JsonHttpEcho.class"])))


(defn assembled-uberjar!
  [body]
  (with-tmp-dir
    (fn [tmp-dir]
      (compiled-java!
        (fn [compile-path]
          (let [jar-path (io/file tmp-dir "uberjar.jar")]
            (deploy/build-jar! {::gcf/entrypoint   'JsonHttpEcho
                                ::gcf/jar-path     jar-path
                                ::gcf/aliases      [:example]
                                ::gcf/compile-path compile-path})
            (body jar-path)))))))


(deftest uberjar-assembly
  (let [files (files-in-zip (assembled-uberjar! zip-file))]
    (testing "contains the Java entrypoint that Google Cloud Functions will call (application class path)"
      (is (contains? files "JsonHttpEcho.class")))
    (testing "contains the application ring handler (application class path)"
      (is (contains? files "nl/epij/gcf/example__init.class")))
    (testing "contains the ring adapter (library class path)"
      (is (contains? files "nl/epij/gcf/ring__init.class")))))
