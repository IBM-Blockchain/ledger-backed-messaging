// SPDX-License-Identifier: Apache-2.0

package org.ledger;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallets;
import org.jboss.logging.Logger;

@ApplicationScoped
public class FabricFactory {
    private static final Logger LOG = Logger.getLogger(FabricFactory.class);

    @ConfigProperty(name = "fabric.wallet.dir")
    public String walletDir;

    @ConfigProperty(name = "fabric.channel")
    public String channel;

    @ConfigProperty(name = "fabric.connection.profile")
    public String profilePath;

    @ConfigProperty(name = "fabric.user")
    public String user;

    @ConfigProperty(name = "fabric.contract")
    public String contract;

    public FabricFactory() {

    }

    public Fabric getFabric() {
        try {
            Fabric f = new Fabric();
            LOG.info("Getting Fabric");
                   
            // Load an existing wallet holding identities used to access the network.
            Path walletDirectory = Paths.get(walletDir);
            LOG.info("Wallet directory is "+walletDirectory);
            Wallet wallet = Wallets.newFileSystemWallet(walletDirectory);

            // Path to a common connection profile describing the network.
            Path networkConfigFile = Paths.get(profilePath);
            LOG.info("Gateway profile file is "+networkConfigFile);
            // Configure the gateway connection used to access the network.
            Gateway.Builder builder;

            builder = Gateway.createBuilder().identity(wallet, user).networkConfig(networkConfigFile);

            // Create a gateway connection
            f.gateway = builder.connect();

            // Obtain a smart contract deployed on the network.
            Network network = f.gateway.getNetwork(channel);
            f.contract = network.getContract(contract);

            return f;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
