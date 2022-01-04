package org.ledgerable.adts;

//  { appReqId, eventId: event.eventId, subId: event.subId, fabricTxId: txId };
public class LedgerableChaincodeEvent {
    public String _id;
    public String appReqId;
    public String eventId;

    public String fabricTxId;

    public String eventName;
}
