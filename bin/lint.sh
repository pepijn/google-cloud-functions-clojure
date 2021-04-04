#!/usr/bin/env bash

set -euxo pipefail

cd "$(dirname "$(realpath "$0")")/.."

clj-kondo --lint ./{**,}/src
