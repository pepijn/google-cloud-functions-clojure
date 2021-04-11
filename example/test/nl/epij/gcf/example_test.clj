(ns nl.epij.gcf.example-test
  (:require
    [babashka.process :as proc]
    [babashka.wait :as wait]
    [clojure.test :refer [deftest is]]
    [org.httpkit.client :as http]))


(def port 8090)


(defn request!
  []
  (let [server (proc/process ["clojure" "-X:run"]
                             {:out       :inherit
                              :err       :inherit
                              :extra-env {"PORT" port}})]
    (try (wait/wait-for-port "localhost" port)
         @(http/request {:url     (format "http://localhost:%s" port)
                         :headers {"X-Forwarded-For"   "1.3.3.7"
                                   "X-Forwarded-Proto" "http"}})
         (finally (proc/destroy-tree server)))))


(deftest http-request
  (is (= (-> (request!) :status)
         200)))

