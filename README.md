# google-cloud-functions-ring-adapter [![Test](https://github.com/pepijn/google-cloud-functions-clojure/actions/workflows/test.yml/badge.svg)](https://github.com/pepijn/google-cloud-functions-clojure/actions/workflows/test.yml) [![Clojars Project](https://img.shields.io/clojars/v/nl.epij/google-cloud-functions-ring-adapter.svg)](https://clojars.org/nl.epij/google-cloud-functions-ring-adapter)

A (Clojure) [ring](https://github.com/ring-clojure/ring) adapter for the [Google Cloud Function Java Runtime](https://cloud.google.com/functions/docs/concepts/java-runtime) on Google Cloud Platform.

## Usage

You can run your cloud function locally or deploy it to Google Cloud Functions.
**Before running or deploying, create a [Java entrypoint](https://cloud.google.com/functions/docs/writing#structuring_source_code) ([example](https://github.com/pepijn/google-cloud-functions-clojure/blob/master/example/src/java/JsonHttpEcho.java)).
Inside the entrypoint, specify your fully-qualified ring handler ([example](https://github.com/pepijn/google-cloud-functions-clojure/blob/f0ed93a7347a35923c3c3f065b9a2d8f145766dc/example/src/java/JsonHttpEcho.java#L5)).**

### Running locally

Add an alias to your `deps.edn` if you want to run locally, such as:

```clojure
{:aliases {:run {:extra-deps {nl.epij.gcf/deploy {:git/url   "https://github.com/pepijn/google-cloud-functions-clojure"
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

1. Add a JAR assemble alias in your `deps.edn` file that specifies the entrypoint mentioned above ([example](https://github.com/pepijn/google-cloud-functions-clojure/blob/dd081026461daa18d24a38b83404a75918d7b11a/example/deps.edn#L15-L22))
1. Deploy the cloud function using the [`gcloud` SDK](https://cloud.google.com/sdk/), specifying the directory containing the JAR with `--source`. See the example in the Pathom docs: https://pathom3.wsscode.com/docs/tutorials/serverless-pathom-gcf#gcf-deploy

Check out the [`example/`](https://github.com/pepijn/google-cloud-functions-clojure/tree/master/example) in this repository for more information.


## Rationale

Google Cloud Functions (GCF)—like other serverless products such as AWS Lambda—offer a cheap way to run your application.
You also don't have to worry about deployment specifics of your code and messing around with Docker containers.
Instead, you simply tell GCF the function name to invoke when triggered.

Clojure enthousiasts can now use this project to have that function be a Clojure function.
More specifically, a ring adapter (in the case of an HTTP trigger).
In this library you'll find all that's necessary to reach that goal—batteries (deployment, structured logging, etc.) included.

## Extras

The library includes a [namespace for structured logging](src/clojure/nl/epij/gcf/log.clj) and a [namespace with ring middleware to make working with PubSub-triggered invocations easier](src/clojure/nl/epij/pubsub/middleware.clj). Also, the ring request map has [Google Cloud Functions environment variables](https://cloud.google.com/functions/docs/env-var#newer_runtimes) assoced to it:

- `:nl.epij.gcf.env/function-target`
- `:nl.epij.gcf.env/function-signature-type`
- `:nl.epij.gcf.env/k-service`
- `:nl.epij.gcf.env/k-revision`
- `:nl.epij.gcf.env/port`

### Structured Logging

In order to enable structured logging, add the `logback.xml` file to your classpath (e.g. in a `resources/` directory):

```
<configuration>
    <appender name="jsonConsoleAppender" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <!-- Ignore default logging fields -->
            <fieldNames>
                <timestamp>[ignore]</timestamp>
                <version>[ignore]</version>
                <logger>[ignore]</logger>
                <thread>[ignore]</thread>
                <level>[ignore]</level>
                <levelValue>[ignore]</levelValue>
            </fieldNames>
        </encoder>
    </appender>
    <root level="INFO">
        <appender-ref ref="jsonConsoleAppender"/>
    </root>
</configuration>
```

## FAQ

### Why do I need to write the entrypoint class in Java—can't we compile it from Clojure?

Compiling the Java entrypoint from Clojure works when you're running locally, yes.
But, during deployment, Google instantiates the entrypoint class while not having Clojure on the classpath.
That is a problem since Clojure's compiled Java code contains a `static {}` block that requires Clojure.
Clojure is not available and causes the build to fail.
Therefore, you need a very minimal Java class that points to your ring handler.
By the way, Clojure is only loaded once—even between subsequent Cloud Function invocations—this is good for performance.
