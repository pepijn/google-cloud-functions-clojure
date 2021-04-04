(ns nl.epij.gcp.gcf.ring-test
  (:require [clojure.test :refer [deftest is testing]])
  (:require [nl.epij.gcp.gcf.ring :as ring]
            [clojure.java.io :as io]
            [ring.middleware.lint :as lint]
            [clojure.string :as str]
            [clojure.spec.alpha :as s]
            [ring.core.spec :as ring.spec]
            [clojure.test.check.properties :as props]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.clojure-test :as tct])
  (:import [com.google.cloud.functions HttpRequest]
           [java.util Optional]))

(def handler
  (lint/wrap-lint (fn [_] {:status 200 :headers {}})))

(defn ^HttpRequest create-request
  [{:keys [headers query method path body]}]
  (proxy [HttpRequest] []
    (getHeaders [] headers)
    (getQuery [] (Optional/ofNullable query))
    (getMethod [] (-> method name str/upper-case))
    (getPath [] path)
    (getInputStream []
      (io/input-stream (cond-> body
                               (string? body)
                               (.getBytes))))))

(defn valid-request?
  [req]
  (let [ring (-> req create-request (ring/request->ring 8080))]
    (handler ring)))

(def request-gen
  (gen/hash-map :headers (gen/let [base         (s/gen :ring.request/headers)
                                   host         (s/gen :ring.request/server-name)
                                   remote-addr  (s/gen :ring.request/remote-addr)
                                   remote-proto (s/gen :ring.request/scheme)]
                           (merge base {"Host"              host
                                        "X-Forwarded-For"   remote-addr
                                        "X-Forwarded-Proto" (name remote-proto)}))
                :query (s/gen :ring.request/query-string)
                :method (gen/elements #{:post :get})
                :path (s/gen :ring.request/uri)
                :body (s/gen :ring.request/body)))

(def valid-request-prop
  (props/for-all [request request-gen] (valid-request? request)))

(tct/defspec prop-request 100 valid-request-prop)

(comment
 (tc/quick-check 1000 valid-request-prop)

 (s/explain-data :ring/response (gen/generate request-gen)))

(defn parse-body
  [req]
  (update req :body slurp))

(comment
 (remove-ns 'nl.epij.gcp.gcf.ring-test)
 )

(deftest ring-adapter
  (let [headers {"Host"              ["example.com"]
                 "X-Forwarded-For"   ["0.0.0.0"]
                 "X-Forwarded-Proto" "https"}
        req     {:headers headers
                 :query   "yo"
                 :method  :post
                 :path    "/opa"
                 :body    "olar"}
        ring    (ring/request->ring (create-request req) 8080)
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
      (is (valid-request? req))
      (is (thrown? Exception (handler (dissoc ring :remote-addr)))))
    req))
