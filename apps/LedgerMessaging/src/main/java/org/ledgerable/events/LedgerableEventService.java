// SPDX-License-Identifier: Apache-2.0

package org.ledgerable.events;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
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
import org.ledgerable.adts.LedgerableChaincodeEvent;
import org.ledgerable.adts.LedgerableEvent;
import org.ledgerable.fabric.FabricService;
import org.ledgerable.fabric.FabricServiceException;
import org.ledgerable.fabric.FabricServiceException.Reason;
import org.ledgerable.fabric.TxRequestStore;

import io.quarkus.runtime.Quarkus;


/**
 * Service for handling Ledgerable Events. This provides the client side view of
 * the LedgerableEventContract
 */
public class LedgerableEventService extends FabricService implements Consumer<ChaincodeEvent> {
    private static final Logger LOGGER = Logger.getLogger(LedgerableEventService.class.getName());

    public LedgerableEventService(TxRequestStore txStore, ExecutorService executor) {
        super(txStore, executor);
    }
    /** Submit via async means, the transaction */
    public void _submit(Supplier<Proposal> pbSupplier, String txRequestId) {
        try {
            LOGGER.info("Submitting=" + txRequestId);
            // get the proposal needed
            Proposal proposal = pbSupplier.get();
            // endorse from the Peers
            Transaction tx = proposal.endorse();

            LOGGER.info("Endorsed=" + txRequestId + " FabricTxId=" + proposal.getTransactionId());

            // keep track of the transaction id, indexed under the applications request id
            txStore.updateTxValidationCode(txRequestId, tx.getTransactionId(), Optional.empty());

            CompletableFuture.supplyAsync(() -> {
                
                SubmittedTransaction submittedTx = tx.submitAsync();
                LOGGER.info("[AsyncSubmit] submitted for FabricTxId=" + tx.getTransactionId());
                Status s1 = submittedTx.getStatus();
                LOGGER.info("[AsyncSubmit] getStatus returned with "+s1.toString());
                return submittedTx;
            }, this.executor).thenApply(submittedTx -> {
                LOGGER.info("[AsyncSubmit] GetStatus for " + tx.getTransactionId());
                String tx_id = submittedTx.getTransactionId();
                Status status = submittedTx.getStatus();
                TxValidationCode txcode = status.getCode();
                this.txStore.updateTxValidationCode(txRequestId, tx_id, Optional.of(txcode));

                if (status.isSuccessful()) {
                    byte[] result = submittedTx.getResult();
                    this.txStore.updateFinalResult(txRequestId, result);
                    LOGGER.info("[AsyncSubmit] FinalResult for " + txRequestId + " FabricTxId="
                            + tx_id);
                    return false;
                } else {
                    // Set to resubmit the tx if there's a MVCC conflict
                    if (txcode.equals(TxValidationCode.MVCC_READ_CONFLICT)) {
                        LOGGER.warning("[AsyncSubmit] Got a MVCC Conflict on txid, resubmitting=" + tx_id);
                        return true;

                    } else {
                        LOGGER.warning("[AsyncSubmit] Something else failed " + status.toString());
                        return false; // need to abort
                    }

                }

            }).thenAccept(action -> {
                // if the action is to resubmit, call _submit
                if (action) {
                    LOGGER.info("[AsyncSubmit] Resubmititng " + txRequestId + " FabricTxId=" + proposal.getTransactionId());
                    _submit(pbSupplier, txRequestId);
                }
            });
        } catch (Throwable t) {
            LOGGER.severe(t.toString());
            processException(t);
        }
    }

    public String submitEventAsync(LedgerableEvent le) {
        final String txRequestId = "AppReqId::"+UUID.randomUUID().toString();
        try (Gateway gateway = builder.connect()) {

            Network network = gateway.getNetwork(networkChannel);
            Contract contract = network.getContract(contractName);

            Jsonb jsonb = JsonbBuilder.create();
            String json = jsonb.toJson(le);

            LOGGER.info("submit: " + json);
            this.txStore.addTx(txRequestId);

            Supplier<Proposal> pbSupplier = () -> contract.newProposal("create").addArguments(json).putTransient("AppReqId",txRequestId).build();
            this._submit(pbSupplier, txRequestId);

        } catch (Throwable e) {
            LOGGER.severe("Failed to submit transaction " + e.getMessage());
            Quarkus.asyncExit();
            processException(e);
            throw new RuntimeException(e);
        }
        return txRequestId;
    }


    public String submitEventSync(LedgerableEvent le) throws FabricServiceException{
        try (Gateway gateway = builder.connect()) {

            Network network = gateway.getNetwork(networkChannel);
            Contract contract = network.getContract(contractName);
            Jsonb jsonb = JsonbBuilder.create();
            String json = jsonb.toJson(le);

            LOGGER.info("submitSync: " + json);

            byte[] result = contract.submitTransaction("create", json);

            return new String(result);
        } catch ( io.grpc.StatusRuntimeException grpc){
            LOGGER.severe("Failed to getEvents" + grpc.getMessage());
            processException(grpc);
            throw (FabricServiceException) new FabricServiceException("Submit failed",Reason.GRPC,false).initCause(grpc);
        
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
    @Override
    public void accept(ChaincodeEvent t) {
       Jsonb jsonb= JsonbBuilder.create();
       LedgerableChaincodeEvent lce = jsonb.fromJson(new String(t.getPayload()),LedgerableChaincodeEvent.class);
       this.txStore.addEvent(lce);
       LOGGER.info("Added event for "+lce.appReqId);
    }


    public void startEvents(){
        super.startEvents(this);
    }
}
