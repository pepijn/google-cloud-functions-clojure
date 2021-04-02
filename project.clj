(defproject nl.epij/google-cloud-function-ring-adapter "0.1.0-SNAPSHOT"
  :description "A (Clojure) ring adapter for the Google Cloud Function Java Runtime on Google Cloud Platform"
  :url "https://github.com/pepijn/google-cloud-function-ring-adapter"
  :license {:name "MIT"
            :url  "https://opensource.org/licenses/MIT"}
  :source-paths ["src/clojure"]
  :test-paths ["test/clojure"]
  :java-source-paths ["src/java" "src/resources"]
  :javac-options ["-target" "11" "-source" "11"]
  :dependencies [[org.clojure/clojure "1.10.3" :scope "provided"]
                 [nl.epij/google-cloud-function-commons "0.1.0-SNAPSHOT"]])
