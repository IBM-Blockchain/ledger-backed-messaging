#!/bin/bash

set -eo pipefail

kubectl get pods --namespace test-network | grep ledger-messaging | cut -d' ' -f1 | xargs -I{} kubectl --namespace test-network logs {} | grep FabriTx | cut -d ' ' -f 10 | cut -d'=' -f2 > txids.log
kubectl get pods --namespace test-network | grep ledger-messaging | cut -d' ' -f1 | xargs -I{} kubectl --namespace test-network logs {} | grep fabricTxId | cut -d ' ' -f 10 > event_txids.log

# kubectl --namespace test-network logs ledger-messaging-68f99db598-sl46p | grep FabriTx | cut -d ' ' -f 10 | cut -d'=' -f2 > txids.log
#kubectl --namespace test-network logs ledger-messaging-68f99db598-ljkbh | grep FabriTx | cut -d ' ' -f 10 | cut -d'=' -f2 > txids.log
kubectl --namespace test-network logs deploy/org1-peer1 | grep 'Done with' --text | cut -d' ' -f16 | tr -d '-' | tr -d '[' | tr -d ']' > org1-peer1-txids.log
kubectl --namespace test-network logs deploy/org1-peer2 | grep 'Done with' --text | cut -d' ' -f16 | tr -d '-' | tr -d '[' | tr -d ']' > org1-peer2-txids.log

echo "===================="
echo "PEER1"
grep -f txids.log org1-peer1-txids.log| sort | uniq | wc -l  || true

echo "===================="
echo "PEER2"
grep -f txids.log org1-peer2-txids.log| sort | uniq | wc -l || true


echo "===================="
echo "All"
cat txids.log | sort | uniq | wc -l