#!/usr/bin/env bash
set -ex -u -o pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

cd "${ROOT_DIR}"/apps/LedgerMessaging; ./gradlew build -x test
cd "${ROOT_DIR}"/apps/TradingEngine; ./gradlew build -x test

