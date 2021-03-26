(defproject nl.epij/google-cloud-function-ring-adapter "0.1.0-alpha7"
  :description "A (Clojure) ring adapter for the Google Cloud Function Java Runtime on Google Cloud Platform"
  :url "https://github.com/pepijn/google-cloud-function-ring-adapter"
  :license {:name "MIT"
            :url  "https://opensource.org/licenses/MIT"}
  :source-paths ["src/clojure"]
  :java-source-paths ["src/java" "src/resources"]
  :javac-options ["-target" "11" "-source" "11"]
  :dependencies [[org.clojure/clojure "1.10.2"]
                 [com.google.cloud.functions/functions-framework-api "1.0.1"]
                 [com.google.cloud/google-cloud-core "1.94.1"]
                 [net.logstash.logback/logstash-logback-encoder "6.6"]
                 [ch.qos.logback/logback-classic "1.2.3"]])
