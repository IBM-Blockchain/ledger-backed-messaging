package org.ledgerable.fabric;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import org.bson.Document;
import org.hyperledger.fabric.protos.peer.TransactionPackage.TxValidationCode;
import org.ledgerable.adts.LedgerableChaincodeEvent;
@Singleton
public class TxRequestStore {
    private ReentrantLock mutex = new ReentrantLock();


    private static final Logger LOG = Logger.getLogger(TxRequestStore.class.getName());

    @Inject
    MongoClient mongoClient;

    public static class Entry {
        public String appRequestId;

        public String finalResult;

        public Map<String, Optional<TxValidationCode>> txAttempts;

        public List<LedgerableChaincodeEvent> events;

        public Entry() {
            this.txAttempts = new HashMap<String, Optional<TxValidationCode>>();
            this.events = new ArrayList<LedgerableChaincodeEvent>();
        }

        public String toString() {

            Jsonb jsonb = JsonbBuilder.create();
            return jsonb.toJson(this);

        }
    }

    Map<String, Entry> dataStore;

    @PostConstruct
    public void init() {
        dataStore = new HashMap<String, Entry>();
    }

    public void addTx(String id) {
        Entry e = new Entry();
        e.appRequestId = id;

        try {
            mutex.lock();
            dataStore.put(id, e);

            Document document = new Document()
                .append("appRequestId", e.appRequestId);
            getCollection().insertOne(document);

            LOG.info("Added to mongo");
        } finally {
            mutex.unlock();
        }
    }

    public void addEvent(LedgerableChaincodeEvent evt) {

        try {
            mutex.lock();
            if (this.dataStore.containsKey(evt.appReqId)) {
                // if the request didn't come from here don't store the events
                Entry e = this.dataStore.get(evt.appReqId);
                e.events.add(evt);
                dataStore.put(evt.appReqId, e);

                getCollection().updateMany(
                    Filters.eq("appRequestId", evt.appReqId),
                    Updates.combine(
                        Updates.push("events",evt)
                    ));

            }
        } finally {
            mutex.unlock();
        }
    }

    public void updateFinalResult(String id, byte[] result) {

        try {
            mutex.lock();
            Entry e = this.dataStore.get(id);
            e.finalResult = result.toString();
            dataStore.put(id, e);

            getCollection().updateMany(
                Filters.eq("appRequestId", id),
                Updates.combine(
                    Updates.set("result", new String(result))
                ));

        } finally {
            mutex.unlock();
        }
    }

    public void updateTxValidationCode(String id, String txId, Optional<TxValidationCode> code) {
        try {
            mutex.lock();
            Entry e = this.dataStore.get(id);

            e.txAttempts.put(txId, code);



            this.dataStore.put(txId, e);
            
            String strCode = code.isPresent() ? code.get().toString(): "<none>";
            getCollection().updateMany(
                Filters.eq("appRequestId", id),
                Updates.combine(
                    Updates.set(txId, strCode)
                ));

        } finally {
            mutex.unlock();
        }
    }

    public Entry getEntry(String txId) {
        if (this.dataStore.containsKey(txId)) {
            return this.dataStore.get(txId);
        } else {
            return null;
        }
    }

    public String getAllEntries() {
        Jsonb jsonb = JsonbBuilder.create();
        String json;
        try {
            mutex.lock();
            List all = new ArrayList(this.dataStore.values());
            json = jsonb.toJson(all);
        } finally {
            mutex.unlock();
        }
        return json;
    }

    private MongoCollection getCollection(){
        return mongoClient.getDatabase("lm").getCollection("apprequest");
    }
}
