// SPDX-License-Identifier: Apache-2.0
package org.ampretia.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

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

    /** 
     * Create a hash to confirm the contents of this trade
     * @throws NoSuchAlgorithmException
     */
    public String getHash() throws NoSuchAlgorithmException{
        StringBuilder str = new StringBuilder("TradeMessage:");
        str = str.append(tradeId).append(":");
        str = str.append(msgId).append(":");
        str = str.append(qty).append(":");
        str = str.append(price).append(":");

        String payload = str.toString();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String dataHash =  Base64.getEncoder().encodeToString(digest.digest(payload.getBytes(StandardCharsets.UTF_8)));

        return dataHash;
    }
}
