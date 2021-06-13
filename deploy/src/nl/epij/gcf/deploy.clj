(ns nl.epij.gcf.deploy
  (:require
    [babashka.process :as process]
    [badigeon.classpath :as classpath]
    [badigeon.javac :as javac]
    [clojure.edn :as edn]
    [clojure.string :as str]
    [clojure.tools.deps.alpha]
    [hf.depstar.uberjar :as depstar]
    [nl.epij.gcf :as gcf])
  (:import
    (java.lang
      Process)))


(defn- run-clj!
  [args]
  (let [args' (concat ["clojure"] args)
        {:keys [err exit] :as proc}
        @(process/process args' {:out :string :err :string})]
    (print err)
    (assert (zero? exit) (str/join " " args'))
    proc))


(defn- get-clj-nss!
  [{::gcf/keys [compile-path entrypoint]}]
  ;; TODO: replace with :aliases
  (let [deps {:aliases {:java {:replace-deps  '{nl.epij/google-cloud-functions-ring-adapter {:mvn/version "0.1.0-SNAPSHOT"}}
                               :replace-paths [(str compile-path)]}}}
        eval `(let [object# (new ~entrypoint)]
                {::handler-ns (-> (.getHandler object#) symbol namespace symbol)
                 ::adapter-ns (-> (.getAdapter object#) symbol namespace symbol)})
        {:keys [out] :as proc} (run-clj! ["-Sdeps" deps "-M:java" "--eval" eval])
        {::keys [handler-ns adapter-ns]} (edn/read-string out)]
    (assoc proc :namespaces [handler-ns adapter-ns])))


(defn compile-javac!
  [{::gcf/keys [src-dir javac-options compile-path]}]
  (let [options (merge-with concat
                            {:javac-options javac-options
                             :compile-path  compile-path}
                            {:javac-options ["-target" "11"
                                             "-source" "11"
                                             "-Xlint:all"]})]
    (javac/javac src-dir options)))


(defn build-jar!
  [{::gcf/keys [compile-path aliases namespaces jar-path extra-paths]
    :or        {aliases []}
    :as        opts}]
  (let [cp (str/join ":" (concat [(classpath/make-classpath {:aliases aliases})
                                  compile-path]
                                 extra-paths))]
    (depstar/build-jar {:jar        jar-path
                        :classpath  cp
                        :compile-ns (if namespaces
                                      namespaces
                                      (-> (merge {::gcf/compile-path compile-path} opts)
                                          (get-clj-nss!)
                                          :namespaces))
                        :exclude    [".+\\.(clj|dylib|dll|so)$"]}))
  jar-path)


(defn assemble-jar!
  [{::gcf/keys [entrypoint java-paths compile-path] :as opts}]
  (assert entrypoint "Supply an entrypoint")
  (assert (symbol? entrypoint) "Entrypoint should be a symbol")
  (doseq [path java-paths]
    (compile-javac! {::gcf/src-dir path ::gcf/compile-path compile-path}))
  (build-jar! opts))


(defonce server (atom nil))


(defn start-server!
  [{::gcf/keys [jar-path entrypoint]
    :as        opts}]
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

  (run-server! {}))
