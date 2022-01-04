package org.ledgerable.fabric;

public class FabricChaincodeEvent {
    String chaincodeName;
    String eventName;
    String fabricTxId;
    long blockId;
    byte[] payload;
}
