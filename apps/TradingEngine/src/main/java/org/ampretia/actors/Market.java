// SPDX-License-Identifier: Apache-2.0

package org.ampretia.actors;

import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.ampretia.model.TradeMessage;
import org.ampretia.transport.Transport;

/**
 *
 */
@Dependent
public class Market implements Consumer<TradeMessage> {

	private static final Logger LOGGER = Logger.getLogger(Market.class.getName());

    @Inject
    public Transport transport;

    public Market(){

    }

    @PostConstruct
    public void init(){
        transport.setOfferQueueConsumer(this);
    }

    @Override
    public void accept(TradeMessage t) {
        // Got a response message back... lets check to see what we've got
        LOGGER.info("Got response "+t);

        Random ran = new Random();
        boolean ok = ran.nextInt(10) < 7;

        if (ran.nextInt(10) < 7){
            LOGGER.info("Offer accpted");
            // knock 10% off... 
            float price = t.price;
            t.setPrice(price*0.9f);
        } else {
            LOGGER.info("Offer not accepted");
            t.setPrice(0.0f);
            t.setQty(0);
        }

        transport.sendResponse(t);
        LOGGER.info("Sent response");

    }
}
