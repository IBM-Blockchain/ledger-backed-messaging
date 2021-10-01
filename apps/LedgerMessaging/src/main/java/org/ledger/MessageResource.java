// SPDX-License-Identifier: Apache-2.0

package org.ledger;


import java.util.List;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.jboss.resteasy.annotations.jaxrs.PathParam;

/**
 * Provides the REST endpoint for returning information on the ledgerable events
 */
@Path("/")
public class MessageResource {
    private static final Logger LOGGER = Logger.getLogger(MessageResource.class.getName());

    @Inject
    public FabricServiceFactory factory;

    public MessageResource() {
      
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
