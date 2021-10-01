// SPDX-License-Identifier: Apache-2.0

package org.ampretia.actors;

import java.util.List;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.ampretia.model.Trade;
import org.ampretia.model.TradeMessage;
import org.ampretia.model.Trader;
import org.jboss.resteasy.annotations.jaxrs.PathParam;

@Path("/trader/")
public class TraderResource {

    private static final Logger LOGGER = Logger.getLogger(Trader.class.getName());

    @Inject
    public TraderService traderService;

    @Path("{traderId}/trade")
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public TradeMessage submitTrade(@PathParam String traderId, TradeMessage newOffer) {
        LOGGER.info(newOffer.toString());
        return traderService.submitTrade(traderId, newOffer);
    }

    @Path("{traderId}/trade/{tradeId}")
    @GET
    @Produces("application/json")
    public Trade getTrade(@PathParam String traderId, @PathParam String tradeId) {
        return traderService.getTrade(traderId,tradeId);
    }

    @Path("{traderId}/trade")
    @GET
    @Produces("application/json")
    public List<Trade> getTrades(@PathParam String traderId) {
        return traderService.getTrades(traderId);
    }
}