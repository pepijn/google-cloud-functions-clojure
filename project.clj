(defproject nl.epij/google-cloud-functions-ring-adapter "0.2.0-SNAPSHOT"
  :description "A (Clojure) ring adapter for the Cloud Function Java Runtime on Google Cloud Platform"
  :url "https://github.com/pepijn/google-cloud-functions-clojure"
  :license {:name "MIT"
            :url  "https://opensource.org/licenses/MIT"}
  :source-paths ["src/clojure"]
  :test-paths ["test/clojure"]
  :java-source-paths ["src/java"]
  :javac-options ["-target" "11" "-source" "11" "-Xlint:all" "-Werror"]
  :resource-paths []
  :dependencies [[org.clojure/clojure "1.10.3" :scope "provided"]
                 [com.google.cloud.functions/functions-framework-api "1.0.4"]
                 [com.google.cloud/google-cloud-core "1.95.2"]
                 [net.logstash.logback/logstash-logback-encoder "6.6"]
                 [ch.qos.logback/logback-classic "1.2.3"]
                 [com.fasterxml.jackson.core/jackson-core "2.12.3"]
                 [cheshire/cheshire "5.10.0"]]
  :profiles {:compile {:resource-paths []}
             :dev     {:dependencies [[org.clojure/test.check "1.1.0"]
                                      [ring/ring-devel "1.9.3"]
                                      [ring/ring-spec "0.0.4"]]}}
  :deploy-repositories [["clojars" {:url           "https://clojars.org/repo"
                                    :username      :env/clojars_user
                                    :password      :env/clojars_pass
                                    :sign-releases false}]]
  :plugins [[lein-cljfmt "0.7.0"]])
