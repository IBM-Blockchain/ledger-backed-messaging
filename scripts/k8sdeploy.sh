#!/usr/bin/env bash
set -ex -u -o pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

k8s_namespace="test-network"
docker_registry="localhost:5000"

cd "${ROOT_DIR}"
kubectl -n ${k8s_namespace} apply -f k8s-deployment/mq-config-map.yaml
kubectl -n ${k8s_namespace} apply -f k8s-deployment/trade-dashboard.yaml
kubectl -n ${k8s_namespace} apply -f k8s-deployment/trade-engine.yaml
kubectl -n ${k8s_namespace} apply -f k8s-deployment/ledger-messaging.yaml
kubectl -n ${k8s_namespace} rollout status deploy/trade-engine
kubectl -n ${k8s_namespace} rollout status deploy/ledger-messaging
kubectl -n ${k8s_namespace} rollout status deploy/trade-dashboard
