# google-cloud-function-ring-adapter

A (Clojure) [ring](https://github.com/ring-clojure/ring) adapter for the [Google Cloud Function Java Runtime](https://cloud.google.com/functions/docs/concepts/java-runtime) on Google Cloud Platform.
This library is still in alpha state.

## Usage

In order to use this, I recommend following the Java quick start: https://cloud.google.com/functions/docs/quickstart-java
Then, in your pom.xml, use the Clojure maven plugin: https://github.com/talios/clojure-maven-plugin
Create a Java stub to call your ring handler through the adapter.

There probably is a more elegant way to make it work, but I didn't find it yet.
