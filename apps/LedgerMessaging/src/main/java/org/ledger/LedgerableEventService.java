// SPDX-License-Identifier: Apache-2.0

package org.ledger;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.hyperledger.fabric.client.CommitException;
import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.Network;

import io.quarkus.runtime.Quarkus;

import java.util.logging.Logger;

/**
 * Service for handling Ledgerable Events. 
 * This provides the client side view of the LedgerableEventContract
 */
public class LedgerableEventService extends FabricService {
    private static final Logger LOGGER = Logger.getLogger(LedgerableEventService.class.getName());

    public LedgerableEventService() {
        super();
    }

    public void submitEvent(LedgerableEvent le) {
        try (Gateway gateway = builder.connect()) {

            Network network = gateway.getNetwork(networkChannel);
            Contract contract = network.getContract(contractName);

            Jsonb jsonb = JsonbBuilder.create();
            String json = jsonb.toJson(le);

            LOGGER.info("submit:" + json);
            contract.submitTransaction("create", json);
        } catch (Throwable e) {
            LOGGER.severe("Failed to submit transaction " + e.getMessage());
            Quarkus.asyncExit();
            processException(e);
            throw new RuntimeException(e);
        }
    }

    public List<LedgerableEvent> getEvents(String tradeId) {
        try (Gateway gateway = builder.connect()) {

            Network network = gateway.getNetwork(networkChannel);
            Contract contract = network.getContract(contractName);

            byte[] result = contract.evaluateTransaction("retrieve", tradeId);
            String json = new String(result, StandardCharsets.UTF_8);           
            LOGGER.info("get:" + json);

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

}
