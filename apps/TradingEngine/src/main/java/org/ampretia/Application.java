// SPDX-License-Identifier: Apache-2.0

package org.ampretia;

import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.Path;

import org.ampretia.actors.Market;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped   
@Path("/v1")
public class Application  {
  
  // @Inject
  // Trader t;

  @Inject 
  Market m;


  private static final Logger LOGGER = Logger.getLogger(Application.class.getName());

  void onStart(@Observes StartupEvent ev) {
      LOGGER.info("The application is starting...");

      // // Connect and start the service going.
        
      // t.go();
      // LOGGER.info("Ledger conencted, and application has started");
  }

  void onStop(@Observes ShutdownEvent ev) {
      LOGGER.info("The application is stopping...");

    
  }


  // @Path("/trades")
  // @GET
  // @Produces(MediaType.APPLICATION_JSON)
  // public ArrayList<Trade> getTrades(){
  //   ArrayList<Trade> tradelist = this.t.getTrades();
  
  //   LOGGER.info(tradelist.toString());
  //   return tradelist;
  // }

}