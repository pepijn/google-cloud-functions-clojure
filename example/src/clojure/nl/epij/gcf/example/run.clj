(ns nl.epij.gcf.example.run
  (:require [nl.epij.gcp.gcf.run :as run]))

(comment

 (run/assemble-jar! '{:nl.epij.gcf/entrypoint   JsonHttpEcho
                      :nl.epij.gcf/java-paths   ["src/java"]
                      :nl.epij.gcf/compile-path "target/classes"
                      :nl.epij.gcf/jar-path     "target/artifacts/application.jar"})

 (run/start-server! '{:nl.epij.gcf/entrypoint   JsonHttpEcho
                      :nl.epij.gcf/java-paths   ["src/java"]
                      :nl.epij.gcf/compile-path "target/classes"
                      :nl.epij.gcf/jar-path     "target/artifacts/application.jar"})

 (run/stop-server!)

 )
