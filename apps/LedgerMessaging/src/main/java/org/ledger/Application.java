// SPDX-License-Identifier: Apache-2.0

package org.ledger;

import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
class Application {

    @Inject
    private LedgerStore ledger;

    private static final Logger LOGGER = Logger.getLogger(Application.class.getName());

    void onStart(@Observes StartupEvent ev) {
        LOGGER.info("The application is starting...");

        // Connect and start the service going.
        ledger.connect();

        LOGGER.info("Ledger conencted, and application has started");
    }

    void onStop(@Observes ShutdownEvent ev) {
        LOGGER.info("The application is stopping...");
        
        // disconnect the ledger
        ledger.disconnect();


        LOGGER.info("Ledger disconnected, application stopping");
    }
}