(ns nl.epij.gcp.gcf.ring
  (:require [clojure.string :as str]
            [nl.epij.gcp.gcf.env :as env]
            [clojure.java.io :as io])
  (:import (com.google.cloud.functions HttpRequest HttpResponse)
           (java.io BufferedWriter)
           [java.util Optional]))

(defn process-response!
  [{:keys [status message headers body]} http-response]
  (doseq [[key value] headers
          :let [key' (if (keyword? key) (name key) key)]]
    (.appendHeader http-response key' (str/join value)))
  (.setStatusCode http-response status message)
  (when body
    (let [writer ^BufferedWriter (.getWriter http-response)]
      (cond (string? body)
            (.write writer ^String body)

            (seq? body)
            (.write writer ^String (str/join body))

            :else
            (io/copy body writer))
      (.close writer))))

(defn request->ring
  [^HttpRequest http-request port]
  (let [{:strs [host
                x-forwarded-for
                x-forwarded-proto]
         :as   headers}
        (into {}
              (map (fn [[k v]] [(str/lower-case k) (str/join v)]))
              (.getHeaders http-request))
        query-string   ^Optional (.getQuery http-request)
        query-string'  (.orElse query-string nil)
        request-method (-> (.getMethod http-request)
                           str/lower-case
                           keyword)
        uri            (.getPath http-request)]
    {:request-method request-method
     :uri            uri
     :query-string   query-string'
     :headers        headers
     :body           (.getInputStream http-request)

     :server-name    host
     :server-port    port
     :remote-addr    x-forwarded-for
     :scheme         (keyword x-forwarded-proto)

     :protocol       "N/A"}))

(defn adapter
  [^HttpRequest http-request ^HttpResponse http-response handler]
  (let [{::env/keys [port] :as platform-env} (env/extract-env-vars!)
        port' (some-> port (Integer/parseInt))]
    (-> (request->ring http-request port')
        (merge platform-env)
        (handler)
        (process-response! http-response))))
