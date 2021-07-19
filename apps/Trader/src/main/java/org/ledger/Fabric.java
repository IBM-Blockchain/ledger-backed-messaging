// SPDX-License-Identifier: Apache-2.0

package org.ledger;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.ContractException;
import org.hyperledger.fabric.gateway.Gateway;
import org.jboss.logging.Logger;
    

public class Fabric {
    private static final Logger LOGGER = Logger.getLogger(Fabric.class);

    public Gateway gateway;
    public Contract contract;

    public Fabric(){

    }

    public void submitEvent(String tradeId, String msgId, String event) {
        try {
            

            Jsonb jsonb = JsonbBuilder.create();
            LedgerableEvent le = new LedgerableEvent();
            le.dataHash="0xCAFEBABE";
            le.eventId = tradeId;
            le.subId = msgId;
            le.type = event;

            String json = jsonb.toJson(le);

            LOGGER.info("submit:"+json);
            contract.createTransaction("create").submit(json);
        } catch (ContractException | TimeoutException | InterruptedException e) {
            this.gateway.close();
            throw new RuntimeException(e);
        }
    }

    public List<LedgerableEvent> getEvents(String tradeId){
     try {
        byte[] result = contract.createTransaction("retrieve").submit(tradeId);
        String json = new String(result, StandardCharsets.UTF_8);
        List<LedgerableEvent> events;
        LOGGER.info("get:"+json);
        Jsonb jsonb = JsonbBuilder.create();
        events = jsonb.fromJson(json,new ArrayList<LedgerableEvent>(){}.getClass().getGenericSuperclass());

        return events;

    } catch (ContractException | TimeoutException | InterruptedException e) {
        this.gateway.close();
        throw new RuntimeException(e);
    }

    }

}
