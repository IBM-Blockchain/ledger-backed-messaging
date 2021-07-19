// SPDX-License-Identifier: Apache-2.0

package org.ledger;


import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.logging.Logger;
@Path("/trade")
public class MessageResource {
    private static final Logger LOGGER = Logger.getLogger(MessageResource.class.getName());
    public MessageResource() {
      
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)  
    public String send() {
        LOGGER.info("Trade got");
        // MessagingService.getService().send(msg)
        return "hello";
    }

}
