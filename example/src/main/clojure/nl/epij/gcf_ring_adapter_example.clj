(ns nl.epij.gcf-ring-adapter-example
  (:require [ring.middleware.json :refer [wrap-json-body]]
            [cheshire.core :as json]))

(defn handler
  [{:keys [body] :as x}]
  (prn x)
  {:status 200
   :body   (str (json/generate-string body {:pretty true}) "\n")})

(def app
  (wrap-json-body handler))
