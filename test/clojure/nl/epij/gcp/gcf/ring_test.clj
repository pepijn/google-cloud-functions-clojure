(ns nl.epij.gcp.gcf.ring-test
  (:require [clojure.test :refer [deftest is testing]])
  (:require [nl.epij.gcp.gcf.ring :as ring]
            [clojure.java.io :as io]
            [ring.middleware.lint :as lint]
            [clojure.string :as str]
            [clojure.spec.alpha :as s]
            [ring.core.spec]
            [clojure.test.check.properties :as props]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.clojure-test :as tct])
  (:import [com.google.cloud.functions HttpRequest HttpResponse]
           [java.util Optional]
           [java.io BufferedWriter Writer StringWriter BufferedReader StringReader ByteArrayOutputStream ByteArrayInputStream]))

(s/check-asserts true)

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

(defn ^HttpResponse create-response
  [state stream]
  (proxy [HttpResponse] []
    (appendHeader [header value]
      (swap! state update :headers #(merge % {header value})))
    (setStatusCode [code message]
      (swap! state assoc :status code)
      (swap! state assoc :message message))
    (getWriter [] (io/writer stream))))

(defn config->request
  [config]
  (-> config create-request (ring/request->ring 8080)))

(def request-config-gen
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

(def handler-config-gen
  (gen/hash-map :status (s/gen :ring.response/status)
                :headers (s/gen :ring.response/headers)
                :body (s/gen :ring.response/body)))

(defn create-handler
  [config]
  (lint/wrap-lint (fn [_] config)))

(def valid-request-prop
  (props/for-all [request-config request-config-gen
                  handler-config handler-config-gen]
    (let [request  (config->request request-config)
          handler  (create-handler handler-config)
          response (handler request)]
      (s/assert :ring/request request)
      (s/assert :ring/response response))))

(def valid-response-prop
  (props/for-all [handler-config handler-config-gen
                  message        (gen/one-of [gen/string (gen/return nil)])]
    (let [os              (ByteArrayOutputStream.)
          state           (atom {:headers {}})
          response        (create-response state os)
          handler-config' (-> handler-config (assoc :message message))
          _               (ring/process-response! handler-config' response)
          config'         (-> handler-config'
                              (update :headers #(reduce-kv (fn [m k v] (assoc m k (str/join v))) {} %)))
          output          (-> @state
                              (assoc :body (.toString os)))]
      ;; TODO: find a way to compare body (mind input stream etc.)
      (= (dissoc config' :body) (dissoc output :body)))))

(comment

 (tc/quick-check 100 valid-response-prop)

 (comment
  (let [os       (ByteArrayOutputStream.)
        state    (atom {})
        response (create-response state os)
        config   (gen/generate handler-config-gen)
        ring     (ring/process-response! config response)]
    (prn os)
    (assoc @state
      :input-body (:body config)
      :body (.toString os)))
  )

 (s/explain-data :ring/response (handler (gen/generate request-config-gen))))

(tct/defspec prop-request 1000 valid-request-prop)

(tct/defspec prop-response 1000 valid-response-prop)

(defn parse-body
  [req]
  (update req :body slurp))

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
            :uri            "/opa"
            :protocol       "N/A"}))
    (testing "whether the linter is working correctly"
      (is (valid-request? req))
      (is (thrown? Exception (handler (dissoc ring :remote-addr)))))
    req))
