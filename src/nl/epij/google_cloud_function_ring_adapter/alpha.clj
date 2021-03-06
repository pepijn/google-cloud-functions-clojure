(ns nl.epij.google-cloud-function-ring-adapter.alpha
  (:require [clojure.string :as str])
  (:import (com.google.cloud.functions HttpRequest HttpResponse)
           (java.io BufferedWriter)
           [java.util Optional]))

(defn process-response!
  [{:keys [status headers body]} http-response]
  (doseq [[key value] headers]
    (.appendHeader http-response key value))
  (.setStatusCode http-response status)
  (when body (.write ^BufferedWriter (.getWriter http-response) body)))

(defn adapter
  [^HttpRequest http-request ^HttpResponse http-response handler]
  (prn)
  (let [headers       (into {}
                            (map (fn [[k v]] [(str/lower-case k) (str/join v)]))
                            (.getHeaders http-request))
        query-string  ^Optional (.getQuery http-request)
        query-string' (.orElse query-string nil)]
    (-> (handler {:request-method (-> (.getMethod http-request)
                                      str/lower-case
                                      keyword)
                  :uri            (.getUri http-request)
                  :path           (.getPath http-request)
                  :query-string   query-string'
                  :headers        headers
                  :body           (.getInputStream http-request)})
        (process-response! http-response))))
