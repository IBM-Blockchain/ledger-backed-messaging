# How to setup this demo

This demo brings together two large pieces of middleware, IBM MQ and Hyperledger Fabric.
The majority of the setup here is focussing on the 3 applications, and the configuration of Fabric. 

The MQ Configuration is not a complex MQ setup, and for the purposes of this was setup using the IBM MQ Cloud service.

Hyperledger Fabric is using the v2.4.0-beta, and is shown being configured here in a local 

## IBM MQ Resources required

The following MQ resources are required.

- MQ QueueManager created and accessible via JMS Client connections. Version of MQ is not critical. The demo has been developed using the latest IBM MQ Cloud offering.
- Three queues are required, a trade offer queue, a trade response queue, and queue for the MQ report messages. These can be default MQ Queue configurations, name is not important.
- User identity and a correctly configured channel

Here is an example `application.properties` file that can be used in development

```
mq.host=mqfab-e9b3.qm.eu-gb.mq.appdomain.cloud
mq.port=30756
mq.channel=CLOUD.APP2.SVRCONN
mq.app.password=xxxxxxxxxxxxxxxxxxxxx
mq.app.user=ledgermsg
mq.qm=mqfab
mq.trade.offer.queue=queue:///DEV.QUEUE.2
mq.trade.response.queue=queue:///DEV.QUEUE.1
mq.ledger.action.queue=queue:///LEDGER.ACTION
```

## Hyperledger Fabric

### Overview

- Use the `test-network-k8s` to setup a Fabric infrastructure, either in a local KIND cluster or within an existing K8S cluster. 
- Install the `ledgerable-event-contract` to Fabric
- Install three client applications to the cluster. Two are not directly connected to fabric, but required to complete the scenario. These are the UI, and the Trading Engine to drive MQ Messaging.  THe third application connects MQ and Fabric.

### Recipe

We need to have two github repos cloned.

Clone the `test-network-k8s` to for the Fabric instructure, and this example repo.

```
git clone git@github.com:jkneubuh/fabric-samples.git
git clone git@github.com:ampretia/ledger-backed-messaging.git
```

Start the fabric network

```bash
cd ~/fabric-samples/test-network-k8s
export TEST_NETWORK_CHAINCODE_IMAGE=localhost:5000/ledgerable-event-contract:latest
export TEST_NETWORK_CHAINCODE_NAME=ledgerable-event-contract
export TEST_NETWORK_FABRIC_VERSION=2.4.0-beta

./network kind
./network up
./network channel create
```

At this point we need to build the docker image for the Smart Contract. This uses the external chaincode ability available since Fabric 2.2.

```bash
cd ~/ledger-backed-messaging/ledgerable-event-contract

docker build -t ledgerable-event-contract:latest .
docker tag ledgerable-event-contract:latest localhost:5000/ledgerable-event-contract:latest
docker push localhost:5000/ledgerable-event-contract:latest
```

You can now deploy this chaincode, this creates the k8S deployment for the chaincode, along with the peer commands to approve and commit the chaincode.

``` bash
./network chaincode deploy
```

To validate that everything is setup

```bash
./network chaincode metadata
```

You should see some JSON output here... that's the metadata description of the contract.
If you have `jq` installed, pipe the output to JQ to get a clearer view of what the contract offers.

> A K8S cluster is now setup, with a fabric infrstructure running, with a smart contract installed

Next step is to start the applications

## Applications

There are 3 applications, that need to made available.

TEST_NETWORK_CHAINCODE_IMAGE=ledgerable-event-contract
TEST_NETWORK_CHAINCODE_NAME=ledgerable-event-contract
TEST_NETWORK_FABRIC_VERSION=2.4.0-beta