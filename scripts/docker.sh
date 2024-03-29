#!/usr/bin/env bash
set -ex -u -o pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

k8s_namespace="test-network"
docker_registry="localhost:5000"

cd "${ROOT_DIR}"
docker build -t trade-dashboard ./apps/Dashboard
docker build -f apps/LedgerMessaging/src/main/docker/Dockerfile.jvm -t ledgermessaging apps/LedgerMessaging
docker build -f apps/TradingEngine/src/main/docker/Dockerfile.jvm -t tradeengine apps/TradingEngine
docker tag trade-dashboard ${docker_registry}/trade-dashboard:latest
docker tag ledgermessaging ${docker_registry}/ledgermessaging:latest
docker tag tradeengine ${docker_registry}/tradeengine:latest
docker push ${docker_registry}/trade-dashboard:latest
docker push ${docker_registry}/ledgermessaging:latest
docker push ${docker_registry}/tradeengine:latest

