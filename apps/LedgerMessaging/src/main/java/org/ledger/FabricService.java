// SPDX-License-Identifier: Apache-2.0

package org.ledger;

import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.Network;

/**
 * Abstract class that provides support for defining 'services' A service should
 * provide a client endpoint for a given contract
 */
public class FabricService {
    private static final Logger LOGGER = Logger.getLogger(FabricService.class.getName());
    protected Gateway.Builder builder;

    protected String networkChannel;
    protected String contractName;

    public FabricService() {

    }

    public FabricService setGatewayBuilder(Gateway.Builder builder) {
        this.builder = builder;
        return this;
    }

    public FabricService setNetworkChannel(String networkChannel) {
        this.networkChannel = networkChannel;
        return this;
    }

    public FabricService setContractName(String contractName) {
        this.contractName = contractName;
        return this;
    }

    public String metadata() {
        try (Gateway gateway = builder.connect()) {

            Network network = gateway.getNetwork(networkChannel);
            Contract contract = network.getContract(contractName);

            byte[] result = contract.evaluateTransaction("org.hyperledger.fabric:GetMetadata");
            String json = new String(result, StandardCharsets.UTF_8);
            LOGGER.info("No error detected: response is :" + json + ":");

            return json;
        } catch (Throwable t) {
            processException(t);
            return t.getMessage();
        }

    }

    protected void processException(final Throwable ex) {
        LOGGER.info("Class      :" + ex.getClass());
        LOGGER.info("Message    :" + ex.getMessage());
        LOGGER.info("Suppressed :" + ex.getSuppressed().length);

        Throwable cause = ex.getCause();
        if (cause == null) {
            LOGGER.info("Cause      : <none>");
        } else {
            LOGGER.info("Cause...   ");
            processException(cause);
        }

    }
}
