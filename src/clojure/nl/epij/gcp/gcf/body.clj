(ns nl.epij.gcp.gcf.body)

(defn body
  [message subscription]
  {::message      message
   ::subscription subscription})
