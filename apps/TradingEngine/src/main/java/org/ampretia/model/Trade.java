// SPDX-License-Identifier: Apache-2.0

package org.ampretia.model;

import java.util.UUID;

/** Abstract class for the essential components of a trade */
public class Trade {

    /** UID of this trade representing all communications regarding the trade */
    public String tradeId;

    /** Original originator of the trade */
    public String tradeOriginatorId;

    /**  */
    public TradeMessage offerMsg;
    public TradeMessage responseMsg;

    public Trade() {
        this.tradeId = UUID.randomUUID().toString();
    }

    public Trade(String originator) {
        this();
        this.tradeOriginatorId = originator;

    }

    public Trade(String originator, String uid) {
        this.tradeId = uid;
        this.tradeOriginatorId = originator;

    }

    public TradeMessage createOffer() {
        this.offerMsg = new TradeMessage(this.tradeId,TradeMessage.TYPE.OFFER);
        return this.offerMsg;
    }

    public TradeMessage createReponse() {
        this.offerMsg = new TradeMessage(this.tradeId,TradeMessage.TYPE.RESPONSE);
        return this.offerMsg;
    }

    public Trade setResponse(TradeMessage response){
        this.responseMsg = response;
        return this;
    }

    public String getTradeId() {
        return this.tradeId;
    }

    public String toString(){
        return "[Trade] "+tradeId+":"+tradeOriginatorId+" [Offer] "+this.offerMsg+" [Response]"+this.responseMsg;
    }
}
