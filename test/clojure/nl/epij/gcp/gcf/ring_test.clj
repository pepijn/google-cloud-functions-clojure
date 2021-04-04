(ns nl.epij.gcp.gcf.ring-test
  (:require [clojure.test :refer [deftest is testing]])
  (:require [nl.epij.gcp.gcf.ring :as ring]
            [clojure.java.io :as io]
            [ring.middleware.lint :as lint]
            [clojure.string :as str])
  (:import [com.google.cloud.functions HttpRequest]
           [java.util Optional]))

(defn ^HttpRequest create-request
  [{:keys [headers query method path body]}]
  (proxy [HttpRequest] []
    (getHeaders [] headers)
    (getQuery [] (Optional/ofNullable query))
    (getMethod [] (-> method name str/upper-case))
    (getPath [] path)
    (getInputStream [] (io/input-stream (.getBytes body)))))

(defn parse-body
  [req]
  (update req :body slurp))

(deftest ring-adapter
  (let [headers {"Host"              ["example.com"]
                 "X-Forwarded-For"   ["0.0.0.0"]
                 "X-Forwarded-Proto" "https"}
        req     (create-request {:headers headers
                                 :query   "yo"
                                 :method  :post
                                 :path    "/opa"
                                 :body    "olar"})
        ring    (ring/request->ring req 8080)
        handler (lint/wrap-lint (fn [_] {:status  200
                                         :headers {}}))]
    (is (= (parse-body ring)
           {:body           "olar"
            :headers        {"host"              "example.com"
                             "x-forwarded-for"   "0.0.0.0"
                             "x-forwarded-proto" "https"}
            :query-string   "yo"
            :remote-addr    "0.0.0.0"
            :request-method :post
            :scheme         :https
            :server-name    "example.com"
            :server-port    8080
            :uri            "/opa"}))
    (testing "whether the linter is working correctly"
      (is (map? (handler ring)))
      (is (thrown? Exception (handler (dissoc ring :remote-addr)))))))
