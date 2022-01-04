package org.example.contracts;

import java.util.ArrayList;
import java.util.logging.Logger;

import org.example.adts.AuditRecord;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.ContractRuntimeException;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ledger.CompositeKey;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;


// Debug mode doesn't practically work with vscode
// confusing with relation to the normal route


@Contract(name="AuditTrail")
public class AuditTrail implements ContractInterface {
    
    private static Logger logger = Logger.getLogger(AuditTrail.class.getName());

        /**
     * Required Default Constructor.
     */
    public AuditTrail() {
        logger.info(()->"AuditTrail:<init>");
    }

    private boolean exists(Context ctx, CompositeKey key){
        byte[] bytes = ctx.getStub().getState(key.toString());
        logger.info("bytes"+bytes+" "+key.toString());
        return bytes!=null;
    }

    @Transaction()
    public AuditRecord addRecord(Context ctx,String businessEvent, String uid, String originator, String description){
        AuditRecord ar = new AuditRecord().setBusinessEvent(businessEvent).setUid(uid).setOriginator(originator).setDescription(description);

        if (exists(ctx,ar.getKey())){
            throw new ContractRuntimeException("AuditRecord UID already assigned");
        }

        ctx.getStub().putState(ar.getKey().toString(), ar.toBytes());

        return ar;
    }

    @Transaction()
    public AuditRecord[] getRecordsforEvent(Context ctx,String businessEvent){
     
        CompositeKey searchKey = AuditRecord.createKey(businessEvent);
        QueryResultsIterator<KeyValue> iterator = ctx.getStub().getStateByPartialCompositeKey(searchKey);

        ArrayList<AuditRecord> records = new ArrayList<AuditRecord>();
        iterator.forEach((keyvalue)->{
            records.add(AuditRecord.fromBytes(keyvalue.getValue()));
        });

        AuditRecord[] retArray = records.toArray(new AuditRecord[0]);
        return retArray;
    }
}
