# TradingEngine

This is application has two functions:
- It provides a REST API for trader client applications to subject trades and get the responses
- It also providers a very basic algorthm to generate the responses for the trades.

Both of these could be split into different applications, and for a production use case would be recommended.

To help provide the REST API, this uses Quarkus https://quarkus.io/. Information on running the app in quarkus is at the bottom and is copied from the Quarkus getting started examples.

## Trading 

### REST API
Three APIs are provided for use the UI clients

| Endpoint                             | Action | Description                                   |
| ------------------------------------ | ------ | --------------------------------------------- |
| v1/trader/{traderId}/trade           | POST   | Submits a Offer TradeMessage for the traderId |
|                                      |        | Body - JSON of a an Offer TradeMessage        |
|                                      |        | 200 - if the trade is accepted for processing |
| v1/trader/{traderId}/trade           | GET    | Returns JSON a list of Trades                 |
| v1/trader/{traderId}/trade/{tradeId} | GET    | Returns the status of a given Trade           |

> Note that the trades are held in memory and not in any persistent storage

### Messaging - Trader Request / Reponse

The trade submission results in a message being sent to the `TRADE_OFFER_Q` (actual queue name defined in the application properties). The body of this message is the JSON of the trademessage, with the following message properties. 

```java
    msg.setIntProperty("JMS_IBM_Report_COA", CMQC.MQRO_COA_WITH_FULL_DATA);
    msg.setIntProperty("JMS_IBM_Report_COD", CMQC.MQRO_COD_WITH_FULL_DATA);

    msg.setJMSReplyTo(context.createQueue("queue:///LEDGER.ACTION"));
    msg.setIntProperty(WMQConstants.JMS_IBM_REPORT_PASS_MSG_ID, CMQC.MQRO_PASS_MSG_ID);
    msg.setIntProperty(WMQConstants.JMS_IBM_REPORT_PASS_CORREL_ID, CMQC.MQRO_PASS_CORREL_ID);
    msg.setStringProperty("TRADE_ID", t.tradeId);
    msg.setStringProperty("TRADE_MSG_TYPE",t.type.toString());
```

In this case the COA/COD report messages are requested with full data, this means that the confirmation of the messages being put onto and got from the queue are reported. Full data is set, so that a hash of the trademessage can be calculated.

To get the reponse to the trade, the client application will call the REST API, that results in a get of a message from the `TRADE_RESPONSE_Q`.  There are a variety of different approaches to getting a response to a message.
 
In this case a 'get-by-correlation-id' approach is used, with the correlation ID being the unique TradeID. A non-blocking call is used here.

### Messaging - Market Trade Responses

The 'Market Clearing' is implemented very simply; there is a message listener that will consume all messages that are sent to the `TRADE_OFFER_Q`.  A very simple approach is then taken - pick a random number between 0 and 10, reject the trade if above 7. A form of failure is implemented for any offer of *Tomatoes*

This can be used to demo how the lack of audit messages can show where in the system an error occured.
## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
./gradlew quarkusDev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

## Packaging and running the application

The application can be packaged using:
```shell script
./gradlew build
```
It produces the `quarkus-run.jar` file in the `build/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `build/quarkus-app/lib/` directory.

If you want to build an _über-jar_, execute the following command:
```shell script
./gradlew build -Dquarkus.package.type=uber-jar
```

The application is now runnable using `java -jar build/quarkus-app/quarkus-run.jar`.

## Creating a native executable

You can create a native executable using: 
```shell script
./gradlew build -Dquarkus.package.type=native
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: 
```shell script
./gradlew build -Dquarkus.package.type=native -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./build/my-artifactId-my-version-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/gradle-tooling.
