(ns pre-push
  (:require
    [babashka.fs :as fs]
    [babashka.process :as proc]
    [clojure.string :as str]))


(defn run-steps!
  [{:keys [dir]}]
  (let [steps [(proc/process ["clj-kondo" "--lint" "."]
                             {:out :inherit
                              :err :inherit
                              :dir dir})
               (proc/process ["cljstyle" "check"]
                             {:out :inherit
                              :err :inherit
                              :dir dir})
               (proc/process ["lein" "with-profile" "compile,dev" "test"]
                             {:out :inherit
                              :err :inherit
                              :dir dir})
               (proc/process ["clojure" "-M:test"]
                             {:out :inherit
                              :err :inherit
                              :dir (fs/file dir "deploy")})
               (proc/process ["clojure" "-M:test"]
                             {:out :inherit
                              :err :inherit
                              :dir (fs/file dir "example")})]]
    (run! proc/check steps)))


(defn do!
  ([sha]
   (do! sha "."))
  ([sha dir]
   (let [tmp-dir (str (fs/create-temp-dir))]
     (try (run! proc/check (proc/pipeline (proc/pb ['git 'archive sha]
                                                   {:dir dir})
                                          (proc/pb '[tar --extract]
                                                   {:dir tmp-dir})))
          (run-steps! {:dir tmp-dir})
          (finally (fs/delete-tree tmp-dir))))))


(defn -main
  [& _args]
  (let [[_ref sha] (-> *in* slurp (str/split #" "))]
    (when sha
      (do! sha))))


(comment
 (do! "3043969777c39b610777e676dba676fceb3754ba"
      "..")

 )
