// SPDX-License-Identifier: Apache-2.0
package org.ampretia.model;

public class TradeMessage {
    /** Trade this message is part of */
    public String tradeId;
    
    /** UID for this specific message */
    public String msgId;

    /** Unit Price */
    public float price;

    /** Unit Qty */
    public int qty;

    public TradeMessage(){

    }

    public TradeMessage(String tradeId){
        this.tradeId = tradeId;
    }
    
    public TradeMessage setMsgId(String msgId){
        this.msgId=msgId;
        return this;
    }

    public TradeMessage setPrice(float price){
        this.price = price;
        return this;
    }

    public TradeMessage setQty(int qty){
        this.qty = qty;
        return this;
    }

    public String toString(){
        return tradeId+":"+msgId+":"+price;
    }
}
