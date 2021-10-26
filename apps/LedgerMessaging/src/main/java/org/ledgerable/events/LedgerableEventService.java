// SPDX-License-Identifier: Apache-2.0

package org.ledgerable.events;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import java.util.logging.Logger;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.hyperledger.fabric.client.ChaincodeEvent;
import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.Network;
import org.hyperledger.fabric.client.Proposal;
import org.hyperledger.fabric.client.Status;
import org.hyperledger.fabric.client.SubmittedTransaction;
import org.hyperledger.fabric.client.Transaction;
import org.hyperledger.fabric.protos.peer.TransactionPackage.TxValidationCode;
import org.ledgerable.adts.LedgerableEvent;
import org.ledgerable.fabric.FabricService;
import org.ledgerable.fabric.TxRequestStore;

import io.quarkus.runtime.Quarkus;


/**
 * Service for handling Ledgerable Events. This provides the client side view of
 * the LedgerableEventContract
 */
public class LedgerableEventService extends FabricService {
    private static final Logger LOGGER = Logger.getLogger(LedgerableEventService.class.getName());

    public LedgerableEventService(TxRequestStore txStore, ExecutorService executor) {
        super(txStore, executor);

    }
    /** Submit via async means, the transaction */
    public void _submit(Supplier<Proposal> pbSupplier, String txRequestId) {
        try {
            LOGGER.info("Submitting for " + txRequestId);
            // get the proposal needed
            Proposal proposal = pbSupplier.get();
            // endorse from the Peers
            Transaction tx = proposal.endorse();

            LOGGER.info("Endorsed for " + txRequestId + " FabriTx=" + proposal.getTransactionId());

            // keep track of the transaction id, indexed under the applications request id
            txStore.updateTxValidationCode(txRequestId, tx.getTransactionId(), Optional.empty());

            CompletableFuture.supplyAsync(() -> {
                LOGGER.info("AsyncSubmit for " + tx.getTransactionId());
                SubmittedTransaction submittedTx = tx.submitAsync();
                submittedTx.getStatus();
                return submittedTx;
            }, this.executor).thenApply(submittedTx -> {
                LOGGER.info("GetStatus for " + tx.getTransactionId());
                String tx_id = submittedTx.getTransactionId();
                Status status = submittedTx.getStatus();
                TxValidationCode txcode = status.getCode();
                this.txStore.updateTxValidationCode(txRequestId, tx_id, Optional.of(txcode));

                if (status.isSuccessful()) {
                    byte[] result = submittedTx.getResult();
                    this.txStore.updateFinalResult(txRequestId, result);
                    LOGGER.info("=========================== Final result for  " + txRequestId + " under fabric tx="
                            + tx_id);
                    return false;
                } else {
                    // Set to resubmit the tx if there's a MVCC conflict
                    if (txcode.equals(TxValidationCode.MVCC_READ_CONFLICT)) {
                        LOGGER.warning("Got a MVCC Conflict on txid, resubmitting=" + tx_id);
                        return true;

                    } else {
                        LOGGER.warning("Something else failed " + status.toString());
                        return false; // need to abort
                    }

                }

            }).thenAccept(action -> {
                // if the action is to resubmit, call _submit
                if (action) {
                    LOGGER.info("Resubmititng " + txRequestId + " FabriTx=" + proposal.getTransactionId());
                    _submit(pbSupplier, txRequestId);
                }
            });
        } catch (Throwable t) {
            LOGGER.severe(t.toString());
            processException(t);
        }
    }

    public String submitEventAsync(LedgerableEvent le) {
        final String txRequestId = UUID.randomUUID().toString();
        try (Gateway gateway = builder.connect()) {

            Network network = gateway.getNetwork(networkChannel);
            Contract contract = network.getContract(contractName);

            Jsonb jsonb = JsonbBuilder.create();
            String json = jsonb.toJson(le);

            LOGGER.info("submit: " + json);
            this.txStore.addTx(txRequestId);

            Supplier<Proposal> pbSupplier = () -> contract.newProposal("create").addArguments(json).build();
            this._submit(pbSupplier, txRequestId);

        } catch (Throwable e) {
            LOGGER.severe("Failed to submit transaction " + e.getMessage());
            Quarkus.asyncExit();
            processException(e);
            throw new RuntimeException(e);
        }
        return txRequestId;
    }


    public String submitEventSync(LedgerableEvent le) {
        try (Gateway gateway = builder.connect()) {

            Network network = gateway.getNetwork(networkChannel);
            Contract contract = network.getContract(contractName);
            Jsonb jsonb = JsonbBuilder.create();
            String json = jsonb.toJson(le);

            LOGGER.info("submitSync: " + json);

            byte[] result = contract.submitTransaction("create", json);

            return "sync";
        } catch (Throwable t) {
            LOGGER.severe("Failed to getEvents" + t.getMessage());
            processException(t);
            Quarkus.asyncExit();
            throw new RuntimeException(t);
        }

    }

    public List<LedgerableEvent> getEvents(String tradeId) {
        try (Gateway gateway = builder.connect()) {

            Network network = gateway.getNetwork(networkChannel);
            Contract contract = network.getContract(contractName);

            LOGGER.info("Getting events for trade id=" + tradeId);
            byte[] result = contract.evaluateTransaction("retrieve", tradeId);
            String json = new String(result, StandardCharsets.UTF_8);
            LOGGER.info("result:" + json);

            Type t = new ArrayList<LedgerableEvent>() {
            }.getClass().getGenericSuperclass();

            Jsonb jsonb = JsonbBuilder.create();
            List<LedgerableEvent> events;
            events = jsonb.fromJson(json, t);

            return events;
        } catch (Throwable t) {
            LOGGER.severe("Failed to getEvents" + t.getMessage());
            processException(t);
            Quarkus.asyncExit();
            throw new RuntimeException(t);
        }

    }
    public void fabricChaincodeEvents() {
        try (Gateway gateway = builder.connect()) {

            Network network = gateway.getNetwork(networkChannel);
            // Contract contract = network.getContract(contractName);
            LOGGER.info("**** Getting chaincode events from Fabric");
            Iterator<ChaincodeEvent> it = network.getChaincodeEvents(contractName);

            while (it.hasNext()) {
                ChaincodeEvent cce = it.next();
                String chaincodeName = cce.getChaincodeName();
                String eventName = cce.getEventName();
                String txId = cce.getTransactionId();
                long blockId = cce.getBlockNumber();
                String payload = new String(cce.getPayload());
                LOGGER.info("**** " + chaincodeName + " " + eventName + "[" + txId + "] " + blockId + " " + payload);
            }
            ;

            LOGGER.info("***** End of Chaincode Event processing");
        } catch (Throwable t) {
            LOGGER.severe("Failed to getEvents" + t.getMessage());
            processException(t);

        }
    }
}
