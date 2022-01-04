# How to setup this demo

This demo brings together two large pieces of middleware, IBM MQ and Hyperledger Fabric.
The majority of the setup here is focussing on the 3 applications, and the configuration of Fabric. 

The MQ Configuration is not a complex MQ setup, and for the purposes of this was setup using the IBM MQ Cloud service.

Hyperledger Fabric is using the v2.4.0-beta, and is shown being configured here in a local 

## IBM MQ Resources required

The following MQ resources are required.

- MQ QueueManager created and accessible via JMS Client connections. Version of MQ is not critical. The demo has been developed using the latest IBM MQ Cloud offering.
- Three queues are required, a *trade offer* queue, a *trade response* queue, and *ledger* queue for the MQ report messages. These can be default MQ Queue configurations, name is not important.
- User identity and a correctly configured channel

With these values for a k8s deployment, configure them in the `k8s-deployment/mq-config-map.yaml` file. There is are two k8s config map that these must be set in.  There are two, once for each application that connects to MQ.  It's realistic that these two applications could connect to different QueueManager

```
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-mq-v1-map
data:
  mq.host: mqfab-e9b3.qm.eu-gb.mq.appdomain.cloud
  mq.port: '30756'
  mq.channel: CLOUD.APP2.SVRCONN
  mq.app.password: xxxxxx
  mq.app.user: ledgermsg
  mq.qm: mqfab
  mq.trade.offer.queue: queue:///DEV.QUEUE.2
```

## Hyperledger Fabric

### Overview

- Use the `test-network-k8s` to setup a Fabric infrastructure, either in a local KIND cluster or within an existing K8S cluster. 
- Install the `ledgerable-event-contract` to Fabric
- Install three client applications to the cluster. Two are not directly connected to fabric, but required to complete the scenario. These are the UI, and the Trading Engine to drive MQ Messaging.  THe third application connects MQ and Fabric.

### Recipe

In this receipe we'll assume that your current working directory is your home (~) directory. You're free to change this, but please alter the commands 

1. Get the code

We need to have two github repos cloned; one to has the setup for the local KIND cluster, the second (where this setup.md is) that contains the demo code and applications.

```
cd ~
git clone git@github.com:hyperledger/fabric-samples.git
git clone git@github.com:ibm-blockchain/ledger-backed-messaging.git
```

2. Start the fabric network

```bash
cd ~/fabric-samples/test-network-k8s
export TEST_NETWORK_CHAINCODE_IMAGE=localhost:5000/ledgerable-event-contract:latest
export TEST_NETWORK_CHAINCODE_NAME=ledgerable-event-contract
export TEST_NETWORK_FABRIC_VERSION=2.4.0-beta

./network kind
./network up
./network channel create
```

3. Build the chaincode docker image
At this point we need to build the docker image for the Smart Contract. This uses the external chaincode ability available since Fabric 2.2.

```bash
cd ~/ledger-backed-messaging/ledgerable-event-contract

docker build -t ledgerable-event-contract:latest .
docker tag ledgerable-event-contract:latest localhost:5000/ledgerable-event-contract:latest
docker push localhost:5000/ledgerable-event-contract:latest
```

4. Deploy the chaincode

You can now deploy this chaincode, this creates the k8S deployment for the chaincode, along with the peer commands to approve and commit the chaincode.

Go back to the `~/fabric-samples/test-network-k8s`.  

Change into the `chaincode` directory; we need to duplicate the `asset-transfer-basic` directory. This directory holds pure json description of where the chaincode is running. We need to create something very similar for the `ledgerable-event-contract'

```
cp -r asset-transfer-basic ledgerable-event-contract
```

Edit the `ledgerable-event-contract/connection.json` so that it is 

```bash
{
  "address": "org1-cc-ledgerable-event-contract:9999",
  "dial_timeout": "10s",
  "tls_required": false
}
```

5. Chaincode Deployment

Go back to the `~/fabric-samples/test-network-k8s`.  

``` bash
export TEST_NETWORK_CHAINCODE_IMAGE=localhost:5000/ledgerable-event-contract:latest
export TEST_NETWORK_CHAINCODE_NAME=ledgerable-event-contract
./network chaincode deploy
```

To validate that everything is setup

```bash
./network chaincode metadata
```

You should see so./netwme JSON output here... that's the metadata description of the contract.
If you have `jq` installed, pipe the output to JQ to get a clearer view of what the contract offers.

> A K8S cluster is now setup, with a fabric infrstructure running, with a smart contract installed

Next step is to start the applications

## Applications

There are 3 applications, that need to made available;

- a WebUI to allow driving of the example. Allows trades to be submitted, and for the link between trades and their audit trail to be show.
- A back-end trade application that interacts with MQ to process and respond to trade messages
  - Note that this is specially coded to simulate a failure with any trade message involving Tomatoes
- The main backend application that pulls the MQ report messages and updates the ledger to give the overall trademessage audit trade


1. Create the K8S Config Maps

We need to create a number of config maps in the cluster that contain the configuration information about where Fabric is, TLS certificates etc. 

From the `fabric-samples/test-network-k8s`

```bash
./network application
```

Four config maps will have been created in the cluster
```
app-fabric-ccp-v1-map    
app-fabric-ids-v1-map    
app-fabric-org1-v1-map   
app-fabric-tls-v1-map
```

2. Build the applications

The two backend applications are Java, and the WebUI is node.js

```
./scripts/build.sh
```
(issues the `./gradlew build` in both of the Java applications)

2) Build the docker images, tag, and push to the registry

```
./scripts/docker.sh
```

3) Apply the application deployments to the cluster

```
./scripts/k8sdeploy.sh
```

This will setup the K8S resources.  The WebUI will be available at [localhost](https://localhost).  You may need to accpet security warnings as there are not certificates setup for the http access.  


# Finished!
This is all finished and you can now drive the application from the webui
