(ns nl.epij.gcf.env)


(defn extract-env-vars!
  "Extract vars set by the GCF runtime: https://cloud.google.com/functions/docs/env-var"
  []
  {::function-target         (System/getenv "FUNCTION_TARGET")
   ::function-signature-type (System/getenv "FUNCTION_SIGNATURE_TYPE")
   ::k-service               (System/getenv "K_SERVICE")
   ::k-revision              (System/getenv "K_REVISION")
   ::port                    (System/getenv "PORT")})
