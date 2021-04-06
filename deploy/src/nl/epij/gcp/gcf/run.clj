(ns nl.epij.gcp.gcf.run
  (:require [badigeon.javac :as javac]
            [babashka.process :as process]
            [hf.depstar.uberjar :as depstar]
            [clojure.tools.deps.alpha]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [badigeon.classpath :as classpath])
  (:import (java.lang Process)))

(defn run-clj!
  [args]
  (let [args' (concat ["clojure"] args)
        {:keys [err exit] :as proc}
        @(process/process args' {:out :string :err :string})]
    (print err)
    (assert (zero? exit) (str/join " " args'))
    proc))

(defn get-clj-nss!
  [{:keys [compile-path entrypoint]}]
  ;; TODO: replace with :aliases
  (let [deps {:aliases {:java {:replace-deps  '{nl.epij/google-cloud-function-ring-adapter {:mvn/version "0.1.0-SNAPSHOT"}}
                               :replace-paths [(str compile-path)]}}}
        eval `(let [object# (new ~entrypoint)]
                {::handler-ns (-> (.getHandler object#) symbol namespace symbol)
                 ::adapter-ns (-> (.getAdapter object#) symbol namespace symbol)})
        {:keys [out] :as proc} (run-clj! ["-Sdeps" deps "-M:java" "--eval" eval])
        {::keys [handler-ns adapter-ns]} (edn/read-string out)]
    (assoc proc :namespaces [handler-ns adapter-ns])))

(defonce server (atom nil))

(defn compile-javac!
  [{:keys [src-dir] :as opts}]
  (let [options (merge-with concat opts {:javac-options ["-target" "11"
                                                         "-source" "11"
                                                         "-Xlint:all"]})]
    (javac/javac src-dir options)))

(defn build-jar!
  [{:keys [compile-path aliases namespaces out-path extra-paths]
    :or   {aliases []}
    :as   opts}]
  (let [cp (str/join ":" (concat [(classpath/make-classpath {:aliases aliases})
                                  compile-path]
                                 extra-paths))]
    (depstar/build-jar {:jar        out-path
                        :classpath  cp
                        :compile-ns (if namespaces
                                      namespaces
                                      (-> (merge {:compile-path compile-path} opts)
                                          (get-clj-nss!)
                                          :namespaces))
                        :exclude    [".+\\.(clj|dylib|dll|so)$"]}))
  out-path)

(defn assemble-jar!
  [{:nl.epij.gcf/keys [entrypoint java-paths compile-path jar-path extra-paths]}]
  (assert entrypoint "Supply an entrypoint")
  (assert (symbol? entrypoint) "Entrypoint should be a symbol")
  (doseq [path java-paths]
    (compile-javac! {:src-dir path :compile-path compile-path}))
  (build-jar! {:entrypoint   entrypoint
               :compile-path compile-path
               :out-path     jar-path
               :extra-paths  extra-paths}))

(defn start-server!
  [{:nl.epij.gcf/keys [jar-path entrypoint]
    :as               opts}]
  (assert (nil? @server) "Server already running")
  (assemble-jar! opts)
  (let [deps-map   {:deps  '{com.google.cloud.functions.invoker/java-function-invoker {:mvn/version "1.0.2"}}
                    :paths [jar-path]}
        class-path (classpath/make-classpath {:deps-map deps-map})
        proc       (process/process ["java"
                                     "--class-path" class-path
                                     "com.google.cloud.functions.invoker.runner.Invoker"
                                     "--target" entrypoint]
                                    {:out :inherit :err :inherit})]
    (reset! server proc)))

(defn stop-server!
  []
  (.destroy ^Process (:proc @server))
  (reset! server nil))

(defn run-server!
  [opts]
  @(start-server! opts))

(comment

 (start-server! {})

 (stop-server!)

 (run-server! {})

 )
