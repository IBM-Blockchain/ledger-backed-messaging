// SPDX-License-Identifier: Apache-2.0

package org.ampretia.actors;

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

	private static final Logger LOGGER = Logger.getLogger(Trader.class.getName());

    @Inject
    private Transport transport;

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
    }
}
