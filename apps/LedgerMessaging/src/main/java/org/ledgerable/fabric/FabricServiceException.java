package org.ledgerable.fabric;

public class FabricServiceException extends Exception{
    
    public static enum Reason { APPLIACTION, FABRIC, GRPC, SELF }

    boolean retry;
    Reason reason;

    public FabricServiceException(String msg,  boolean retry){
        super(msg);
        this.reason = Reason.SELF;
        this.retry = retry;
    }

    public FabricServiceException(String msg, Reason reason, boolean retry){
        super(msg);
        this.reason = reason;
        this.retry = retry;
    }

    public FabricServiceException(String msg, Throwable cause, Reason reason, boolean retry){
        super(msg,cause);
        this.reason = reason;
        this.retry = retry;
    }

    public boolean isTryable(){
        return this.retry;
    }

    public Reason getReason(){
        return this.reason;
    }

}
