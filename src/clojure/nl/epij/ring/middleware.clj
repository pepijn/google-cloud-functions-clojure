(ns nl.epij.ring.middleware
  (:require [cheshire.core :as json]
            [nl.epij.gcp.gcf.body :as body]
            [nl.epij.gcp.gcf.message :as message]
            [nl.epij.gcp.gcf.log :as log]
            [clojure.string :as str])
  (:import [java.util Base64]))

(defn wrap-pubsub-data
  "Middleware that decodes the Base64 data of the Pubsub message. Also parses the JSON.

  Make sure the body JSON was parsed before, using something like `ring.middleware.json/wrap-json-body`"
  [handler]
  (fn [{:keys [request-method uri body] :as request}]
    (if (get-in body ["message" "data"])
      (let [{:strs [message subscription]} body
            {data-base64  "data"
             message-id   "messageId"
             publish-time "publishTime"} message
            data-json (String. (.decode (Base64/getDecoder) ^String data-base64))
            data      (json/parse-string data-json)
            message   (message/message message-id publish-time data)
            request'  (assoc request :body (body/body message subscription))]
        (log/info (str "Processing HTTP request: " (str/upper-case (name request-method)) " " uri) request)
        (handler request'))
      (do (log/info (str "Processing HTTP request: " (str/upper-case (name request-method)) " " uri) request)
          (handler request)))))
