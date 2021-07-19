// SPDX-License-Identifier: Apache-2.0

package org.ampretia.actors;

import java.util.HashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.ampretia.model.Trade;
import org.ampretia.model.TradeMessage;
import org.ampretia.transport.Transport;

@Dependent
public class Trader implements Consumer<TradeMessage> {

    private static final Logger LOGGER = Logger.getLogger(Trader.class.getName());

    @Inject
    private Transport transport;

    private String name;

    private HashMap<String,Trade> trades = new HashMap();

    public Trader() {
    }

    @PostConstruct
    public void init() {
        this.name = "Trader#2";
        // transport.setConsumer(this);
    }

    public void go() {
        for (int i = 0; i < 10; i++) {

            LOGGER.info("Set a message");
            Trade t = new Trade(this.name);
            TradeMessage offer = t.createOffer().setPrice(34.2f).setQty(100);
            transport.send(offer);

            trades.put(t.tradeId,t);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        System.out.println(trades);

        
    }

    @Override
    public void accept(TradeMessage t) {
        // Got a response message back... lets check to see what we've got
        LOGGER.info("Got response " + t);
    }

}