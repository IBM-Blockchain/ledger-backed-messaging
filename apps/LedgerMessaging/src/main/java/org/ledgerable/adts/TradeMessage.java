// SPDX-License-Identifier: Apache-2.0
package org.ledgerable.adts;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

import javax.json.bind.annotation.JsonbTransient;


public class TradeMessage {
    
    public static enum TYPE {
        OFFER, RESPONSE
    };

    private static final Logger LOGGER = Logger.getLogger(TradeMessage.class.getName());
    /** Trade this message is part of */
    public String tradeId;
    
    /** UID for this specific message */
    public String msgId;

    /** Unit Price */
    public int price;

    /** Unit Qty */
    public int qty;

    /** Asset description */
    public String description;

    /** Is this a request or a response */
    public TYPE type;


    public TradeMessage(){

    }

    public TradeMessage(String tradeId, TYPE type) {
        this.tradeId = tradeId;
        this.type = type;
    }
    
    public TradeMessage setMsgId(String msgId){
        this.msgId=msgId;
        return this;
    }

    public TradeMessage setPrice(int price){
        this.price = price;
        return this;
    }

    public TradeMessage setQty(int qty){
        this.qty = qty;
        return this;
    }

    public TradeMessage setDescription(String description){
        this.description = description;
        return this;
    }

    public String toString() {
        return "[TradeMessage] " + type + ":" + tradeId + ":" + msgId + ":" + price + ":" + description;
    }
    /** 
     * Create a hash to confirm the contents of this trade
     * @throws NoSuchAlgorithmException
     */
    @JsonbTransient
    public String getHash() throws NoSuchAlgorithmException{
        StringBuilder str = new StringBuilder("TradeMessage:");
        str = str.append(tradeId).append(":");
        str = str.append(type).append(":");
        str = str.append(qty).append(":");
        str = str.append(price).append(":");
        str = str.append(description).append(":");
       
        String payload = str.toString();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String dataHash = TradeMessage.bytesToHex(digest.digest(payload.getBytes(StandardCharsets.UTF_8)));
        LOGGER.info("Hashing "+str+" to  "+dataHash);
        return dataHash;
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
