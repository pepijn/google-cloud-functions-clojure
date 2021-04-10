(ns nl.epij.pubsub.message)


(defn message
  [id publish-time data]
  {::message-id   id
   ::publish-time publish-time
   ::data         data})
