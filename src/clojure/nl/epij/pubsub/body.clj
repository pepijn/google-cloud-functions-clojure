(ns nl.epij.pubsub.body)


(defn body
  [message subscription]
  {::message      message
   ::subscription subscription})
