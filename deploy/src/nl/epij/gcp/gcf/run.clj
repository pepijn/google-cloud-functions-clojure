(ns nl.epij.gcp.gcf.run
  (:require [badigeon.javac :as javac]
            [badigeon.jar :as jar]
            [babashka.process :as process]
            [clojure.java.classpath :as jcp]
            [hf.depstar.uberjar :as depstar]
            [clojure.tools.deps.alpha]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [badigeon.bundle :as bundle]
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

(comment (get-clj-nss! {:entrypoint   'Entrypoint
                        :compile-path "target/uberjar/development/classes"})
         )

(defonce server (atom nil))

(defn compile-javac!
  [{:keys [src-dir] :as opts}]
  (let [options (merge-with concat opts {:javac-options ["-target" "11"
                                                         "-source" "11"
                                                         "-Xlint:all"]})]
    (javac/javac src-dir options)))

(defn entrypoint-jar!
  [{:keys [src-dir compile-path extra-paths class-path out-path manifest-class-path]
    :or   {src-dir             "src/main/java"
           compile-path        "target/jar/classes"
           extra-paths         []
           manifest-class-path {}
           class-path          (->> (jcp/system-classpath) (str/join ":"))}
    :as   opts}]
  (compile-javac! (merge {:src-dir src-dir :compile-path compile-path} opts))
  (let [jar (jar/jar 'entrypoint
                     nil
                     {:allow-all-dependencies? true
                      :out-path                out-path
                      :paths                   (conj extra-paths compile-path)
                      :manifest                {"Class-Path" manifest-class-path}})]
    {:entrypoint-jar jar
     :class-path     (str class-path ":" jar)}))

(defn entrypoint-jar2!
  [{:keys [compile-path extra-paths class-path out-path manifest-class-path]
    :or   {compile-path        "target/jar/classes"
           extra-paths         []
           manifest-class-path {}
           class-path          (->> (jcp/system-classpath) (str/join ":"))}}]
  (let [jar (jar/jar 'entrypoint
                     nil
                     {:allow-all-dependencies? true
                      :out-path                out-path
                      :paths                   (conj extra-paths compile-path)
                      :manifest                {"Class-Path" manifest-class-path}})]
    {:entrypoint-jar jar
     :class-path     (str class-path ":" jar)}))

(defn entrypoint-uberjar!
  [{:keys [compile-path namespaces out-path]
    :or   {compile-path "target/uberjar/classes"
           out-path     (str (bundle/make-out-path 'uberjar nil))}
    :as   opts}]
  (let [uberjar-path       (io/file (str out-path "/output/libs/uberjar.jar"))
        invoker-deps       '{com.google.cloud.functions.invoker/java-function-invoker {:mvn/version "1.0.2"}}
        invoker-class-path (-> (run-clj! ["-Sdeps" {:aliases {:invoker {:replace-deps  invoker-deps
                                                                        :replace-paths []}}}
                                          "-A:invoker"
                                          "-Spath"])
                               :out
                               str/trim)
        {:keys [entrypoint-jar]} (entrypoint-jar! (assoc opts :out-path (str out-path "/output/entrypoint.jar")
                                                              :manifest-class-path "libs/uberjar.jar"))]
    (depstar/build-jar {:jar        uberjar-path
                        :compile-ns (if namespaces
                                      namespaces
                                      (-> (merge {:compile-path compile-path} opts)
                                          (get-clj-nss!)
                                          :namespaces))
                        :exclude    [".+\\.(clj|dylib|dll|so)$"]})
    {:class-path (str uberjar-path ":" entrypoint-jar ":" invoker-class-path)}))

(defn entrypoint-uberjar2!
  [{:keys [compile-path aliases namespaces out-path]
    :or   {aliases []}
    :as   opts}]
  (let [cp (str (classpath/make-classpath {:aliases aliases})
                ":"
                compile-path)]
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
  [{:nl.epij.gcf/keys [entrypoint java-paths compile-path]}]
  (doseq [path java-paths]
    (compile-javac! {:src-dir path :compile-path compile-path}))
  (entrypoint-uberjar2! {:entrypoint   entrypoint
                         :compile-path compile-path}))

(defn start-server!
  [{:keys [entrypoint mode]
    :or   {}
    :as   opts}]
  (assert (nil? @server) "Server already running")
  (assert entrypoint "Supply an entrypoint")
  (assert (symbol? entrypoint) "Entrypoint should be a symbol")
  (let [{class-path' :class-path}
        (case mode
          :jar (entrypoint-jar! opts)
          :uberjar (entrypoint-uberjar! opts))
        proc
        (process/process ["java"
                          "--class-path" class-path'
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
