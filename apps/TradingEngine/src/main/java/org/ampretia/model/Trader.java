package org.ampretia.model;

import java.util.HashMap;

public class Trader {
    
    public String name;

    public HashMap<String,Trade> trades = new HashMap<String,Trade>();

    public Trader(){

    }

    public Trader(String name){
        this.name=name;
    }

    public void addTrade(Trade trade){
        trades.put(trade.getTradeId(),trade);
    }

    public Trade createTrade(){
        return new Trade(this.name);
    }
}
