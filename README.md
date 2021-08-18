# Ledger Backed Messaging

An investigation is using Hyperledger Fabric to store confirmation report messages from [IBM MQ](https://www.ibm.com/uk-en/products/mq) on the ledger.

## Scenario

The scenario is based around a 'Trader' submitting a 'TradeOffer' saying for example they are offering to supply 300 pineapples for 45.0 unit price. This offer is submitted to a market, which responds with if the offer is accepted or not (and what price the market is willing to pay).

IBM MQ is very very good at not losing data (Yes I worked on it for nearly 20 years, and some of the tests including pulling out power cables).  In order to help with the confidence, when a message is put to a queue, and removed from a queue a separate 'report' message can be sent. 'Confirm-on-Arrival COA'  and 'Confirm-on-Delivery' COD


For this example, we're using a request-response style of messaging. A client application puts a message onto a queue, this is picked up by a second client application. It's processed, and a response message is sent to the original client application.

Both the request and response message are sent with the options for COA and COD messages. These can include the full contents of the message they are reporting on. 

A separate application is listening on the queue that the report messages arrived on. They are consumed and the details of the report is sent to a SmartContract transaction. This then stores the details on the ledger, including a hash of the original report.

## How does this give assurance?

Let's walk through how this works focussing on assurance.

1. Bob wants to submit a trade offering 300 pineapples for 45.0 unit price
2. He enters this on the UI, and is given a _TradeID_ and a _TradeHash_
   1. these are created entirely client side. the TradeID is created using the standard processes to give ids that are very very unlikely to conflict. 
   2. the TradeHash is the a hash created from a fixed format of the data. 
   3. These can be recorded by Bob (and in the UI these are going to be stored in browser storage)
3. The UI forwards the request to the 'TradingEngine' application via REST
4. This is then sent via the JMS API to IBM MQ, with the settings for the report messages
5. All messages are sent using the 'Transacted' JMS Contexts, this means that the application has to send the message and then commit the result. This protects against things like network failures etc. 
6. The message is then consumed by the other half of the 'TradingEngine' - the market - that 'processes' the trade and sends a response message
   1. The processing logic here is randomly reject 30% of trades.
   2. The messages are also consume via transacted sessions, so anything that goes wrong means the messaging actions are backed out. Eg if the the random processing fails, the trade message is automatically returned to the queue.
7. Bob can refresh the UI to get response message

### Where does Fabric fit in?

1. The report messages are consumed, again via a transacted JMS Context. 
2. This report message will give the TradeId, the JMS Message ID (so is it the trade request or response), and actual contents of the trade, and if this report is for a message arriving on the queue or being consumed.
3. The TradeID,MessageID,action (arrive/consume), and a hash of actual trade are computed
4. A transaction is submitted to the SmartContract to record this information.
5. Once that completes the JMSContext is committed. 

### How does Bob use the data in the Fabric ledger?

Let's say that the Market processing ignores any trade with an even quantity. What this means is that the message will be consumed by the Market but no response is sent. Report messages however will have been created when the trade request arrived on the queue, and when it was consumed. 

Bob when checking the UI will see no response to his trade request. Using the TradeID, he can query the Fabric ledger, (via another SmartContract transaction). This will show him the fact the trade message arrived, and was consumed. He knows the TradeHash, so can say with confidence that his trade as he meant to send it ws consumed but never processed. 

With this infomration stored on the Ledger he can raise this issue with the 'TradingEngine' managers.

Similarly, the TraadingEngine managers, can view the information for a given trade on the ledger. This can tell them that for example the trade response was consumed. If Bob then says he never saw the response, then investigations can focus on the correct component. 

As the actual trade data is hashed, there is a degree of privacy for the events on the ledger. But still being able to say the events matched the trade data.
