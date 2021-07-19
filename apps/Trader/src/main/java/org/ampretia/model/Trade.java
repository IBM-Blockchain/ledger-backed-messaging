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

    public Trade(String originator){
        this.tradeOriginatorId = originator;
        this.tradeId = UUID.randomUUID().toString();
    }

    public TradeMessage createOffer(){
        this.offerMsg =  new TradeMessage(this.tradeId);
        return this.offerMsg;
    }
}
