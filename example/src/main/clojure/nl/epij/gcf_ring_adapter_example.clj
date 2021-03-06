(ns nl.epij.gcf-ring-adapter-example
  (:require [ring.middleware.json :as m.json]
            [ring.middleware.lint :as m.lint]
            [cheshire.core :as json]))

(defn handler
  [{:keys [body] :as x}]
  (prn x)
  {:status 200
   :body   (str (json/generate-string body {:pretty true}) "\n")})

(def app
  (-> handler
      m.json/wrap-json-body
      #_m.lint/wrap-lint))
