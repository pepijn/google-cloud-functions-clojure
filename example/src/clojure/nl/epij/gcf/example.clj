(ns nl.epij.gcf.example
  (:require
    [cheshire.core :as json]
    [ring.middleware.json :as m.json]
    [ring.middleware.lint :as m.lint]))


(defn handler
  [req]
  (prn req)
  (let [body (try (json/generate-string req {:pretty true})
                  (catch Exception _e
                    (json/generate-string (dissoc req :body) {:pretty true})))]
    {:status  200
     :headers {"Content-Type" "application/json"}
     :body    (str body "\n")}))


(def app
  (-> handler
      m.json/wrap-json-body
      m.lint/wrap-lint))
