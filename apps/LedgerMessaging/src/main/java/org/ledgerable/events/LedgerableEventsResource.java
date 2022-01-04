// SPDX-License-Identifier: Apache-2.0

package org.ledgerable.events;


import java.util.List;
import java.util.Random;
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
import org.ledgerable.fabric.FabricServiceException;
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
    public String addManualEvent(LedgerableEvent evt) throws FabricServiceException{  
        LOGGER.info("[addManualEvent] submitEvent  >>");
        // userid in this case could come from the rest request
        // hard code for the moment
        String userid = "appuser_org1";
        String appRequestId;
        try{
            LedgerableEventService f = factory.getLedgerableService(userid);
            appRequestId = f.submitEventAsync(evt);
           // bus.send("SUBMIT_TX", txid);

        } catch (FabricServiceException fse){
            LOGGER.info("Trying again");
            LedgerableEventService f = factory.getLedgerableService(userid);
             // String txid = f.submitEventAsync(evt);
            appRequestId = f.submitEventSync(evt);

        }
        // bus.send("SUBMIT_TX", txid);

        LOGGER.info("[addManualEvent] submitEvent  << " + appRequestId);
        return appRequestId;
    }

    /**
     * Checks the status of a submitted request
     * 
     * @param apprequestid
     * @return
     */
    @Path("status/{apprequestid}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getStatus(@PathParam String apprequestid){
        LOGGER.info("Checking tx "+apprequestid);
        return factory.checkTxId(apprequestid);
    }

    @Path("event/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)  
    public List<LedgerableEvent> getLedgerableEvents(@PathParam String id)throws FabricServiceException{
        LOGGER.info("Getting event for "+id);

        // userid in this case could come from the rest request
        // hard code for the moment
        String userid = "appuser_org1";
        LedgerableEventService f = factory.getLedgerableService(userid);
        List<LedgerableEvent> events = f.getEvents(id);
        LOGGER.info("Events :"+events);
        return events;
    }


    @Path("txstate")
    @GET
    @Produces(MediaType.APPLICATION_JSON)  
    public String getAllTxState()throws FabricServiceException{
        LOGGER.info("Getting all the cached tx state");

        String allAppRequests = factory.getAllRequests();
        
        return allAppRequests;
    }

    @Path("metadata")
    @GET
    @Produces(MediaType.APPLICATION_JSON)  
    public String metadata() throws FabricServiceException {
        LOGGER.info("Requesting metadata");

        // userid in this case could come from the rest request
        // hard code for the moment
        String userid = "appuser_org1";

        LedgerableEventService f = factory.getLedgerableService(userid);
        String r = f.metadata();
        return r;
    }

    @Path("system/ping")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String ping() throws FabricServiceException {
        LOGGER.info("system::ping");
                // userid in this case could come from the rest request
        // hard code for the moment
        String userid = "appuser_org1";

        LedgerableEventService f = factory.getLedgerableService(userid);
        String r =  f.evaluate("ContractSupport:ping", "Hello World");
        return r;
    }

    @Path("system/efail")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String efail() throws FabricServiceException {
        LOGGER.info("> system::fail");
                // userid in this case could come from the rest request
        // hard code for the moment
        String userid = "appuser_org1";

        LedgerableEventService f = factory.getLedgerableService(userid);
        String r =  f.evaluate("ContractSupport:fail", "aaaarg");
        LOGGER.info("< system::fail "+r);
        return r;
    }

    @Path("system/sfail")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String sfail() throws FabricServiceException {
        LOGGER.info("> system::fail");
                // userid in this case could come from the rest request
        // hard code for the moment
        String userid = "appuser_org1";

        LedgerableEventService f = factory.getLedgerableService(userid);
        String r =  f.submit("ContractSupport:fail", new String[]{"aaaarg"});
        LOGGER.info("< system::fail "+r);
        return r;
    }

    @Path("system/mvcc")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String mvcc() throws FabricServiceException {
        LOGGER.info("> system::mvcc");
                // userid in this case could come from the rest request
        // hard code for the moment
        String userid = "appuser_org1";

        LedgerableEventService f = factory.getLedgerableService(userid);
        String r1 =  f.submit("ContractSupport:set", new String[]{ "thekey",generateRandom()});
        String r2 =  f.submit("ContractSupport:set", new String[]{"thekey",generateRandom()});
        LOGGER.info("< system::mvcc "+r1);
        LOGGER.info("< system::mvcc "+r2);
        return r1+"::"+r2;
    }

    private static String AtoZ="abcdefghijklmnopqrstuvz";

    private static String generateRandom() {
        Random rand=new Random();
        StringBuilder res=new StringBuilder();
        for (int i = 0; i < 17; i++) {
           int randIndex=rand.nextInt(AtoZ.length()); 
           res.append(AtoZ.charAt(randIndex));            
        }
        return res.toString();
    }

}

