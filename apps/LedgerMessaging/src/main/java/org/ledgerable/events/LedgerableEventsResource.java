// SPDX-License-Identifier: Apache-2.0

package org.ledgerable.events;


import java.util.List;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.annotations.jaxrs.PathParam;
import org.ledgerable.adts.LedgerableEvent;
import org.ledgerable.fabric.FabricServiceFactory;

/**
 * Provides the REST endpoint for returning information on the ledgerable events
 */
@Path("/")
public class LedgerableEventsResource {
    private static final Logger LOGGER = Logger.getLogger(LedgerableEventsResource.class.getName());

    @Inject
    public FabricServiceFactory factory;

    public LedgerableEventsResource() {
      
    }

    /**
     * POST Request to add an event on direct REST request
     * Returns the TransactionID, that can be used later to check on success/failure
     */
    @Path("event")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public String addManualEvent(LedgerableEvent evt) {  
        LOGGER.info("[addManualEvent] submitEvent  >>");
        // userid in this case could come from the rest request
        // hard code for the moment
        String userid = "appuser_org1";
        
        LedgerableEventService f = factory.getLedgerableService(userid);
        String txid = f.submitEventAsync(evt);
        
        // bus.send("SUBMIT_TX", txid);

        LOGGER.info("[addManualEvent] submitEvent  << " + txid);
        return txid;
    }

    /**
     * Checks the status of a submitted transaction
     * 
     * @param txid
     * @return
     */
    @Path("status/{txid}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getStatus(@PathParam String txid){
        LOGGER.info("Checking tx "+txid);
        return factory.checkTxId(txid);
    }

    @Path("/event/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)  
    public List<LedgerableEvent> getLedgerableEvents(@PathParam String id) {
        LOGGER.info("Getting event for "+id);

        // userid in this case could come from the rest request
        // hard code for the moment
        String userid = "appuser_org1";

        LedgerableEventService f = factory.getLedgerableService(userid);
        List<LedgerableEvent> events = f.getEvents(id);
        LOGGER.info("Events :"+events);
        return events;
    }

    @Path("/metadata")
    @GET
    @Produces(MediaType.APPLICATION_JSON)  
    public String metadata() {
        LOGGER.info("Requesting metadata");

        // userid in this case could come from the rest request
        // hard code for the moment
        String userid = "appuser_org1";

        LedgerableEventService f = factory.getLedgerableService(userid);
        String r = f.metadata();
        return r;
    }
}
