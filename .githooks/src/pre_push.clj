(ns pre-push
  (:require
    [babashka.fs :as fs]
    [babashka.process :as proc]
    [clojure.string :as str]))


(defn do!
  ([sha]
   (do! sha "."))
  ([sha dir]
   (let [tmp-dir (str (fs/create-temp-dir))]
     (try (run! proc/check (proc/pipeline (proc/pb ['git 'archive sha]
                                                   {:dir dir})
                                          (proc/pb '[tar --extract]
                                                   {:dir tmp-dir})))
          (let [steps [(proc/process ["clj-kondo" "--lint" "."]
                                     {:out :inherit
                                      :err :inherit
                                      :dir tmp-dir})
                       (proc/process ["cljstyle" "check"]
                                     {:out :inherit
                                      :err :inherit
                                      :dir tmp-dir})
                       (proc/process ["lein" "with-profile" "compile,dev" "test"]
                                     {:out :inherit
                                      :err :inherit
                                      :dir tmp-dir})
                       (proc/process ["clojure" "-M:test"]
                                     {:out :inherit
                                      :err :inherit
                                      :dir (fs/file tmp-dir "deploy")})]]
            (run! proc/check steps))
          (finally (fs/delete-tree tmp-dir))))))


(defn -main
  [& _args]
  (let [[_ref sha] (-> *in* slurp (str/split #" "))]
    (do! sha)))


(comment
 (do! "3043969777c39b610777e676dba676fceb3754ba"
      "..")

 )
