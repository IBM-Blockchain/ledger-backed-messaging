// SPDX-License-Identifier: Apache-2.0

package org.ampretia.actors;

import java.util.Random;
import java.util.function.Function;
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
public class Market implements Function<TradeMessage, TradeMessage> {

    private static final Logger LOGGER = Logger.getLogger(Market.class.getName());

    @Inject
    public Transport transport;

    public Market() {

    }

    @PostConstruct
    public void init() {
        transport.setMarketConsumer(this);
    }

    @Override
    public TradeMessage apply(TradeMessage t) {
        // Got a response message back... lets check to see what we've got
        LOGGER.info("Got response " + t);

        Random ran = new Random();

        t.setType(TradeMessage.TYPE.RESPONSE);
        if (ran.nextInt(10) < 7) {
            LOGGER.info("Offer accepted");
            t.setPrice(t.price);
        } else {
            LOGGER.info("Offer not accepted");
            t.setPrice(0);
            t.setQty(0);
        }

        if (t.description.equals("Tomatoes")) {
            LOGGER.info("Not going to do tomatoes, not *really* a fruit");
            return null;
        } else {
            return t;
        }

    }
}
