(ns nl.epij.gcf.log-test
  (:require
    [clojure.test :refer [deftest is]]
    [nl.epij.gcf.log :as log])
  (:import
    (net.logstash.logback.argument
      StructuredArguments)))


(deftest structured-arguments
  (let [args (log/structured-arguments :error {:random "value" :number 1337})]
    (is (= args
           (list (StructuredArguments/keyValue "random" "value")
                 (StructuredArguments/keyValue "number" 1337)
                 (StructuredArguments/keyValue "edn" "{:random \"value\", :number 1337}")
                 (StructuredArguments/keyValue "severity" "ERROR"))))))
