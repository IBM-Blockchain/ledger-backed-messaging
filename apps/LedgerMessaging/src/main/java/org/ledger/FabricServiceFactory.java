// SPDX-License-Identifier: Apache-2.0

package org.ledger;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.identity.Identities;
import org.hyperledger.fabric.client.identity.Identity;
import org.hyperledger.fabric.client.identity.Signer;
import org.jboss.logging.Logger;
import org.ledger.identity.JsonIdAdapter;

import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.quarkus.runtime.Quarkus;

@ApplicationScoped
public class FabricServiceFactory {

    private static final Logger LOG = Logger.getLogger(FabricServiceFactory.class);

    @ConfigProperty(name = "fabric.wallet.dir")
    public String walletDir;

    @ConfigProperty(name = "fabric.channel")
    public String channel;

    @ConfigProperty(name = "fabric.contract")
    public String contract;

    @ConfigProperty(name = "fabric.gateway.tlsCertPath")
    public String tlsCertPath;

    @ConfigProperty(name = "fabric.gateway.hostport")
    public String hostport;

    @ConfigProperty(name = "fabric.gateway.sslHostOverride")
    public String sslHostOverride;

    private ManagedChannel grpcChannel;
    private JsonIdAdapter idWallet;

    public FabricServiceFactory() {

    }

    /**
     * Create the gRPC connection to be used by the Fabric Gateway
     */
    @PostConstruct
    public void setup() {

        try {
            Reader tlsCertReader;
            tlsCertReader = Files.newBufferedReader(Paths.get(tlsCertPath));
            X509Certificate tlsCert = Identities.readX509Certificate(tlsCertReader);

            // for non-tls use
            // NettyChannelBuilder.forTarget("digibankpeer-api.127-0-0-1.nip.io:8080").usePlaintext().build();
            this.grpcChannel = NettyChannelBuilder.forTarget(hostport)
                    .sslContext(GrpcSslContexts.forClient().trustManager(tlsCert).build())
                    .overrideAuthority(sslHostOverride).build();


            ConnectivityState state = this.grpcChannel.getState(false);
            LOG.info("grpcChannel state "+state.toString());
            this.grpcChannel.notifyWhenStateChanged(state, () -> {
                LOG.info("grpcChannel changed state");
                ConnectivityState newstate =  this.grpcChannel.getState(false);
                LOG.info(newstate.toString());
            });

            
            // setup the loading of the identities
            idWallet = new JsonIdAdapter(Paths.get(walletDir));

        } catch (IOException | CertificateException e) {
            LOG.error("Failed to establish the gRPC connection " + e.getMessage());
            Quarkus.blockingExit();
            throw new RuntimeException("Failed to establish the gRPC connection", e);
        }

    }

    @PreDestroy
    public void shutdown() {
        LOG.info("Sending request to shutdown gRPC channel");
        grpcChannel.shutdown();
        try {
            grpcChannel.awaitTermination(30, TimeUnit.SECONDS);
            LOG.info("termination completed");
        } catch (InterruptedException ie){
            LOG.error("Got interrupted exception on gRPC shutdown");
        }
    }

    

    /**
     * Get the service for ledgerable contract
     * @param userid ID to use for connecting and signing
     * @return LedgerableEventService
     */
    public LedgerableEventService getLedgerableService(String userid) {
        LOG.info("getLedgerableService "+userid+" >>");
        try {
            Identity id = idWallet.getIdentity(userid);
            Signer signer = idWallet.getSigner(userid);
            Gateway.Builder builder = Gateway.newInstance().identity(id).signer(signer).connection(grpcChannel);

            return (LedgerableEventService) new LedgerableEventService().setGatewayBuilder(builder).setContractName(contract)
                    .setNetworkChannel(channel);

        } catch (InvalidKeyException | CertificateException | IOException e) {
            LOG.error("Failed to create gateway builder " + e.getMessage());
            Quarkus.asyncExit(100);
            
            throw new RuntimeException("Failed to create gateway builder", e);
        }
    }

}
