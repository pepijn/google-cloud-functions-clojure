#!/usr/bin/env bash

set -euxo pipefail

cd "$(dirname "$(realpath "$0")")/.."

clojure -M:test
