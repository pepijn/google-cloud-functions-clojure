(ns nl.epij.gcp.gcf.message)

(defn message
  [id publish-time data]
  {::message-id   id
   ::publish-time publish-time
   ::data         data})
