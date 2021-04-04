#!/usr/bin/env bash

set -euxo pipefail

shellcheck "$0"

export JAVA_HOME=~/Library/Java/JavaVirtualMachines/adopt-openjdk-11.0.10/Contents/Home/

cd "$(dirname "$(realpath "$0")")/.."

clj-kondo --lint src/clojure --lint test/clojure

lein with-profile compile,dev test

lein install
