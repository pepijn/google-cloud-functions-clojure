(ns nl.epij.google-cloud-function-ring-adapter.alpha
  (:require [clojure.string :as str])
  (:import (com.google.cloud.functions HttpRequest HttpResponse)
           (java.io BufferedWriter)))

(defn adapter
  [^HttpRequest http-request ^HttpResponse http-response handler]
  (let [{:keys [status headers body]}
        (handler {:request-method (-> (.getMethod http-request)
                                      str/lower-case
                                      keyword)
                  :uri            (.getUri http-request)
                  :path           (.getPath http-request)
                  :query-string   (.getQuery http-request)
                  :headers        (.getHeaders http-request)
                  :body           (.getInputStream http-request)})]
    (doseq [[key value] headers]
      (.appendHeader http-response key value))
    (.setStatusCode http-response status)
    (when body (.write ^BufferedWriter (.getWriter http-response) body))))
