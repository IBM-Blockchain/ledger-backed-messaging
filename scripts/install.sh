#!/usr/bin/env bash
set -ex -u -o pipefail

ROOT_DIR="$(cd "$(dirname "$0")/." && pwd)"


git clone git@github.com:hyperledger/fabric-samples.git  ${ROOT_DIR}
git clone git@github.com:ibm-blockchain/ledger-backed-messaging.git ${ROOT_DIR}


cd ${ROOT_DIR}/fabric-samples/test-network-k8s
export TEST_NETWORK_FABRIC_VERSION=2.4.0-beta

./network kind
./network up
./network channel create

cd ${ROOT_DIR}/ledger-backed-messaging/ledgerable-event-contract

docker build -t ledgerable-event-contract:latest .
docker tag ledgerable-event-contract:latest localhost:5000/ledgerable-event-contract:latest
docker push localhost:5000/ledgerable-event-contract:latest

mkdir -p ${ROOT_DIR}/fabric-samples/test-network-k8s/chaincode/ledgerable-event-contract
cd ${ROOT_DIR}/fabric-samples/test-network-k8s/chaincode/ledgerable-event-contract

cat > connection.json <<EOF
{
  "address": "org1-cc-ledgerable-event-contract:9999",
  "dial_timeout": "10s",
  "tls_required": false
}
EOF

cat > metadata.json <<EOF
{
  "type": "external",
  "label": "lec_1.0"
}
EOF

cd ${ROOT_DIR}/fabric-samples/test-network-k8s
export TEST_NETWORK_CHAINCODE_IMAGE=localhost:5000/ledgerable-event-contract:latest
export TEST_NETWORK_CHAINCODE_NAME=ledgerable-event-contract
./network chaincode deploy
./network chaincode metadata
./network application


cd ${ROOT_DIR}/ledger-backed-messaging
./scripts/build.sh
./scripts/docker.sh
./scripts/k8sdeploy.sh

