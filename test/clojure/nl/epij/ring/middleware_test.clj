(ns nl.epij.ring.middleware-test
  (:require
    [cheshire.core :as json]
    [clojure.test :refer [deftest is]]
    [nl.epij.ring.middleware :as mw])
  (:import
    (java.util
      Base64)))


(set! *print-namespace-maps* false)


(defn base-64-string
  [data]
  (->> (json/generate-string data)
       (.getBytes)
       (.encode (Base64/getEncoder))
       (String.)))


(deftest wrap-pubsub-data
  (let [data        {"a" 1337 "b" 42}
        data-base64 (base-64-string data)
        response    ((mw/wrap-pubsub-data identity)
                     {:request-method :get
                      :body           {"subscription" "projects/<project-id>/subscriptions/<subscription-id>"
                                       "message"      {"data"         data-base64
                                                       "messageId"    "12345"
                                                       "message_id"   "12345"
                                                       "publishTime"  "2021-03-28T22:05:24.662Z"
                                                       "publish_time" "2021-03-28T22:05:24.662Z"}}})]
    (is (= {:request-method
            :get

            :body
            {:nl.epij.gcp.gcf.body/message      {:nl.epij.gcp.gcf.message/data         {"a" 1337
                                                                                        "b" 42}
                                                 :nl.epij.gcp.gcf.message/message-id   "12345"
                                                 :nl.epij.gcp.gcf.message/publish-time "2021-03-28T22:05:24.662Z"}
             :nl.epij.gcp.gcf.body/subscription "projects/<project-id>/subscriptions/<subscription-id>"}}
           response))
    (is (= data (-> response :body :nl.epij.gcp.gcf.body/message :nl.epij.gcp.gcf.message/data)))))
