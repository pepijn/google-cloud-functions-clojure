#!/usr/bin/env bash

set -euxo pipefail

cd "$(dirname "$(realpath "$0")")/.."

clojure -Sdeps '{:replace-deps {nl.epij/google-cloud-functions-ring-adapter {:local/root ".."}}}' -M:test
