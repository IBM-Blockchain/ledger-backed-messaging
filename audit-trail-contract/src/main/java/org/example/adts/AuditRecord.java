/*
 * Copyright 2020 Hyperledger Fabric Contributors. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.example.adts;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ledger.CompositeKey;
import static java.nio.charset.StandardCharsets.UTF_8;
import org.json.JSONObject;

@DataType()
public class AuditRecord {

    @Property()
    private String originator;

    @Property()
    private String description;

    @Property()
    private String uid;

    @Property()
    private String businessEvent;

    public AuditRecord setUid(String uid){
        this.uid = uid;
        return this;
    }

    public String getUid()  {
        return this.uid;
    }
    
    public AuditRecord setBusinessEvent(String evt){
        this.businessEvent = evt;
        return this;
    }

    public String getBusinessEvent(){
        return this.businessEvent;
    }

    public CompositeKey getKey() {
        return new CompositeKey(AuditRecord.class.getSimpleName(), new String[]{ this.businessEvent, this.uid });
    }

    public static CompositeKey createKey(String[] attributes) {
        return new CompositeKey(AuditRecord.class.getSimpleName(), attributes);
    }

    public static CompositeKey createKey(String attribute) {
        return new CompositeKey(AuditRecord.class.getSimpleName(), new String[] {attribute});
    }
    /**
     * Constructor - creates empty asset.
     */
    public AuditRecord() {
    }

    /**
     *
     * @return String value
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param value String description
     */
    public AuditRecord setDescription(final String value) {
        this.description = value;
        return this;
    }

    public AuditRecord setOriginator(final String name){
        this.originator = name;
        return this;
    }

    public String getOriginator(){
        return this.originator;
    }

    public byte[] toBytes() {
        String jsonStr = new JSONObject(this).toString();
        return jsonStr.getBytes(UTF_8);
    }

    /**
     * Constructs new asset from JSON String.
     * @param json Asset format.
     * @return MyAsset
     */
    public static AuditRecord fromBytes(final byte[] bytes) {
        return fromJSON(new String(bytes, UTF_8));
    }

    public static AuditRecord fromJSON(final String assetJSON) {
        try {
            JSONObject json = new JSONObject(assetJSON);
            final String id = json.getString("uid");
            final String value = json.getString("value");
            final String owner = json.getString("owner");      
            return new AuditRecord().setUid(id).setDescription(value).setOriginator(owner);
        } catch (Exception e) {
            throw new ChaincodeException("Deserialize error: " + e.getMessage(), "DATA_ERROR");
        }
    }

}
