# google-cloud-function-ring-adapter [![Test](https://github.com/pepijn/google-cloud-function-ring-adapter/actions/workflows/test.yml/badge.svg)](https://github.com/pepijn/google-cloud-function-ring-adapter/actions/workflows/test.yml) [![Clojars Project](https://img.shields.io/clojars/v/nl.epij/google-cloud-function-ring-adapter.svg)](https://clojars.org/nl.epij/google-cloud-function-ring-adapter)

A (Clojure) [ring](https://github.com/ring-clojure/ring) adapter for the [Google Cloud Function Java Runtime](https://cloud.google.com/functions/docs/concepts/java-runtime) on Google Cloud Platform.
This library is still in alpha state—some namespaces might change before the first release.

## Usage

You can run your cloud function locally or deploy it to Google Cloud Functions.
**Before running or deploying, create a [Java entrypoint](https://cloud.google.com/functions/docs/writing#structuring_source_code) ([example](https://github.com/pepijn/google-cloud-function-ring-adapter/blob/master/example/src/java/JsonHttpEcho.java)).
Inside the entrypoint, specify your fully-qualified ring handler ([example](https://github.com/pepijn/google-cloud-function-ring-adapter/blob/f0ed93a7347a35923c3c3f065b9a2d8f145766dc/example/src/java/JsonHttpEcho.java#L5)).**

### Running locally

Add an alias to your `deps.edn` if you want to run locally, such as:

```clojure
{:aliases {:run {:extra-deps {nl.epij.gcf/deploy {:git/url   "https://github.com/pepijn/google-cloud-function-ring-adapter"
                                                  :sha       "3772d2489d8f590df1b28b87a70d364b6311a0cd"
                                                  :deps/root "deploy"}}
                 :exec-fn    nl.epij.gcf.deploy/run-server!
                 :exec-args  {:nl.epij.gcf/entrypoint   JsonHttpEcho
                              :nl.epij.gcf/java-paths   ["src/java"]
                              :nl.epij.gcf/compile-path "target/classes"
                              :nl.epij.gcf/jar-path     "target/artifacts/application.jar"}}}}
```

Then run the server:

```bash
PORT=13337 clojure -X:run
```

Finally, send HTTP requests to it:
```bash
curl localhost:13337
```

### Deploying to Cloud Functions

Before you can do the first HTTP request to your deployed Cloud Function ring handler, you need to take the following steps:

1. Add a JAR assemble alias in your `deps.edn` file that specifies the entrypoint mentioned above ([example](https://github.com/pepijn/google-cloud-function-ring-adapter/blob/f0ed93a7347a35923c3c3f065b9a2d8f145766dc/example/deps.edn#L15-L22))
1. Deploy the cloud function using the [`gcloud` SDK](https://cloud.google.com/sdk/), specifying the directory containing the JAR with `--source` (example coming soon)

Check out the [`example/`](https://github.com/pepijn/google-cloud-function-ring-adapter/tree/master/example) in this repository for more information.


## Rationale

Google Cloud Functions (GCF)—like other serverless products such as AWS Lambda—offer a cheap way to run your application.
You also don't have to worry about deployment specifics of your code and messing around with Docker containers.
Instead, you simply tell GCF the function name to invoke when triggered.

Clojure enthousiasts can now use this project to have that function be a Clojure function.
More specifically, a ring adapter (in the case of an HTTP trigger).
In this library you'll find all that's necessary to reach that goal—batteries (deployment, structured logging, etc.) included.

## FAQ

### Why do I need to write the entrypoint class in Java—can't we compile it from Clojure?

Compiling the Java entrypoint from Clojure works when you're running locally, yes.
But, during deployment, Google instantiates the entrypoint class while not having Clojure on the classpath.
That is a problem since Clojure's compiled Java code contains a `static {}` block that requires Clojure.
Clojure is not available and causes the build to fail.
Therefore, you need a very minimal Java class that points to your ring handler.
By the way, Clojure is only loaded once—even between subsequent Cloud Function invocations—this is good for performance.
