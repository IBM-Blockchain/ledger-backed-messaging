package org.ledgerable.fabric;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;

import org.hyperledger.fabric.protos.peer.TransactionPackage.TxValidationCode;

@Singleton
public class TxRequestStore {
    private ReentrantLock mutex = new ReentrantLock();

    public static class Entry {
        byte[] finalResult;

        Map<String, Optional<TxValidationCode>> txAttempts;

        public Entry() {
            this.txAttempts = new HashMap<String, Optional<TxValidationCode>>();
        }

        public String toString() {
            StringBuilder builder = new StringBuilder("[TxRequest:Entry]").append(System.lineSeparator());
            builder.append("FinalResult::").append(finalResult == null ? "<none>" : new String(finalResult)).append(System.lineSeparator());

            String mapAsString = txAttempts.keySet().stream().map(key -> key + "=" + txAttempts.get(key))
                    .collect(Collectors.joining(", ", "{", "}"));

            builder.append("TxAttempts::").append(mapAsString).append(System.lineSeparator());

            return builder.toString();
        }
    }

    Map<String, Entry> dataStore;

    @PostConstruct
    public void init() {
        dataStore = new HashMap<String, Entry>();
    }

    public void addTx(String id) {
        Entry e = new Entry();

        try {
            mutex.lock();
            dataStore.put(id, e);
        } finally {
            mutex.unlock();
        }
    }

    public void updateFinalResult(String id, byte[] result) {

        try {
            mutex.lock();
            Entry e = this.dataStore.get(id);
            e.finalResult = result;
            dataStore.put(id, e);

        } finally {
            mutex.unlock();
        }
    }

    public void updateTxValidationCode(String id, String txId, Optional<TxValidationCode> code) {
        try {
            mutex.lock();
            Entry e = this.dataStore.get(id);

            e.txAttempts.put(txId,code);

            this.dataStore.put(txId, e);
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

}
