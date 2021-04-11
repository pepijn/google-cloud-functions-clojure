# google-cloud-function-ring-adapter [![Test](https://github.com/pepijn/google-cloud-function-ring-adapter/actions/workflows/test.yml/badge.svg)](https://github.com/pepijn/google-cloud-function-ring-adapter/actions/workflows/test.yml) [![Clojars Project](https://img.shields.io/clojars/v/nl.epij/google-cloud-function-ring-adapter.svg)](https://clojars.org/nl.epij/google-cloud-function-ring-adapter)

A (Clojure) [ring](https://github.com/ring-clojure/ring) adapter for the [Google Cloud Function Java Runtime](https://cloud.google.com/functions/docs/concepts/java-runtime) on Google Cloud Platform.
This library is still in alpha state—some namespaces might change before the first release.

## Usage

Check out the `example/`.

## Rationale

Google Cloud Functions (GCF)—and other serverless products like AWS Lambda—offer a cheap way to run your application.
You also don't have to worry about deployment of your code and messing around with Docker containers.
Instead, you simply tell GCF what function to invoke when triggered.

For Clojure enthousiasts that function should be a Clojure function.
More specifically, a ring adapter (in the case of an HTTP trigger).
In this library you'll find all that's necessary to reach that goal—batteries (deployment, structured logging, etc.) included.
