(ns nl.epij.gcf.log
  (:require
    [cheshire.core :as json]
    [cheshire.generate :refer [add-encoder encode-str]]
    [clojure.string :as str]
    [clojure.walk :as walk])
  (:import
    (com.fasterxml.jackson.core
      JsonGenerationException)
    (jdk.internal.net.http
      HttpClientFacade
      HttpRequestImpl)
    (net.logstash.logback.argument
      StructuredArguments)
    (org.slf4j
      Logger
      LoggerFactory)))


(def ^Logger logger
  (LoggerFactory/getLogger ^String (.toString *ns*)))


(add-encoder HttpRequestImpl encode-str)
(add-encoder HttpClientFacade encode-str)


(defn assoc-edn
  [m]
  (assoc m :edn (pr-str m)))


(declare log)


(defn structured-arguments
  [level data]
  (let [data' (-> data
                  (assoc-edn)
                  (walk/stringify-keys)
                  (assoc "severity" (-> level name str/upper-case)))]
    (mapcat (fn [[k v]]
              (cond (nil? v)
                    []

                    (coll? v)
                    (try
                      [(StructuredArguments/raw k (json/generate-string v))]
                      (catch JsonGenerationException ^JsonGenerationException e
                        (log "WARNING" "Couldn't generate JSON map for logging" (Throwable->map e))
                        [(StructuredArguments/keyValue k (.toString v))]))

                    (string? v)
                    [(StructuredArguments/keyValue k v)]

                    (number? v)
                    [(StructuredArguments/keyValue k v)]

                    :else
                    [(StructuredArguments/keyValue k (.toString v))]))
            data')))


(defn log
  [level ^String message data]
  (.error logger
          message
          (to-array (structured-arguments level data))))


(defn debug
  [message data]
  (log "DEBUG" message data))


(defn info
  [message data]
  (log "INFO" message data))


(defn warning
  [message data]
  (log "WARNING" message data))


(defn error
  [message data]
  (log "ERROR" message data))


(comment

  (error "berichtje"
         {:event-id   1337
          :api-params {:a 42 :b "yo"}}))
