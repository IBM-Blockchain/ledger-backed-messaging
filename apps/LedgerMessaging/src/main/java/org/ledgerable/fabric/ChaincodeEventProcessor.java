package org.ledgerable.fabric;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.logging.Logger;

import org.hyperledger.fabric.client.ChaincodeEvent;
import org.hyperledger.fabric.client.CloseableIterator;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.Gateway.Builder;
import org.hyperledger.fabric.client.Network;
import org.ledgerable.events.LedgerableEventService;

public class ChaincodeEventProcessor {

    private static ReentrantLock mutex = new ReentrantLock();

    private static final Logger LOGGER = Logger.getLogger(LedgerableEventService.class.getName());
    private static Map<String, ChaincodeEventProcessor> processors = new HashMap<>();

    public static ChaincodeEventProcessor getChaincodeEventProcessor(Builder builder, String channel,
            String contractName) {

        try {
            mutex.lock();
            if (processors.containsKey(contractName)) {
                return processors.get(contractName);
            } else {

                ChaincodeEventProcessor cep = new ChaincodeEventProcessor(builder, channel, contractName);
                cep.start();
                processors.put(contractName, cep);

                return cep;

            }
        } finally {
            mutex.unlock();
        }
    }

    Builder builder;
    String channel;
    String contractName;
    Consumer<ChaincodeEvent> c;
    CloseableIterator<ChaincodeEvent> it;

    AtomicBoolean started = new AtomicBoolean(false);

    private ChaincodeEventProcessor(Builder builder, String channel, String contractName) {
        this.builder = builder;
        this.channel = channel;
        this.contractName = contractName;
    }

    public void addConsumer(Consumer<ChaincodeEvent> c) {
        this.c = c;
    }

    public void stop() {
        LOGGER.info("Calling close on iterator");
        this.it.close();
        LOGGER.info("Closed iterator");
    }

    public void start() {
        if (started.compareAndSet(false, true)) {
            CompletableFuture.runAsync(() -> {
                try (Gateway gateway = builder.connect()) {
                    Network network = gateway.getNetwork(channel);
                    LOGGER.info("**** Getting chaincode events from Fabric " + contractName + " cepid="
                            + System.identityHashCode(this));
                    this.it = network.getChaincodeEvents(contractName);

                    while (it.hasNext()) {
                        ChaincodeEvent cce = it.next();

                        String chaincodeName = cce.getChaincodeName();
                        String eventName = cce.getEventName();
                        String txId = cce.getTransactionId();
                        long blockId = cce.getBlockNumber();

                        LOGGER.info("**EVT** " + chaincodeName + " " + eventName + "[" + txId + "] " + blockId);
                        c.accept(cce);
                    }

                } catch (Throwable t) {
                    LOGGER.severe("Failed to getEvents" + t.getMessage());
                    throw t;
                } finally {
                    LOGGER.info("**** End of Chaincode Event processing cepid=" + System.identityHashCode(this));
                }
            }).thenRun(() -> {
                LOGGER.info("... chaincode processing accepted and completed");
            });
        }
    }

}
