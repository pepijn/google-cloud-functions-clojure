(defproject nl.epij/google-cloud-function-ring-adapter "0.1.0-SNAPSHOT"
  :description "A (Clojure) ring adapter for the Cloud Function Java Runtime on Google Cloud Platform"
  :url "https://github.com/pepijn/google-cloud-function-ring-adapter"
  :license {:name "MIT"
            :url  "https://opensource.org/licenses/MIT"}
  :source-paths ["src/clojure"]
  :test-paths ["test/clojure"]
  :java-source-paths ["src/java"]
  :javac-options ["-target" "11" "-source" "11" "-Xlint:all" "-Werror"]
  :resource-paths []
  :dependencies [[org.clojure/clojure "1.10.3" :scope "provided"]
                 [nl.epij/google-cloud-function-commons "0.1.0-SNAPSHOT"]]
  :profiles {:compile {:resource-paths []}
             :dev     {:dependencies [[org.clojure/test.check "1.1.0"]
                                      [ring/ring-devel "1.8.2"]
                                      [ring/ring-spec "0.0.4"]]}}
  :deploy-repositories [["clojars" {:url           "https://clojars.org/repo"
                                    :username      :env/clojars_user
                                    :password      :env/clojars_pass
                                    :sign-releases false}]]
  :plugins [[lein-cljfmt "0.7.0"]])
