// SPDX-License-Identifier: Apache-2.0

package org.ledgerable.fabric;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.identity.Identities;
import org.hyperledger.fabric.client.identity.Identity;
import org.hyperledger.fabric.client.identity.Signer;
import org.ledgerable.events.LedgerableEventService;
import org.ledgerable.fabric.identity.JsonIdAdapter;

import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.quarkus.runtime.Quarkus;

@Singleton
public class FabricServiceFactory {

    private static final Logger LOG = Logger.getLogger(FabricServiceFactory.class.getName());

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
    private ExecutorService executor;
    private Runnable grpcStateChange;
    @Inject
    TxRequestStore txStore;

    // is this accepting requests?
    private boolean running = false;

    public FabricServiceFactory() {

    }

    public String checkTxId(String appRequestId) {
        TxRequestStore.Entry e = this.txStore.getEntry(appRequestId);
        if (e == null) {
            return "<No Record of that App Request Id>";
        } else {
            return e.toString();
        }
    }

    public String getAllRequests() {
        return this.txStore.getAllEntries();
    }

    /**
     * Create the gRPC connection to be used by the Fabric Gateway
     */
    @PostConstruct
    public void setup() {
        this.grpcChannel = createGrpcChannel();
    }

    private ManagedChannel createGrpcChannel() {
        ManagedChannel grpcChannel;
        try {
            LOG.info("Reading the tlsCert");
            Reader tlsCertReader;
            tlsCertReader = Files.newBufferedReader(Paths.get(tlsCertPath));
            X509Certificate tlsCert = Identities.readX509Certificate(tlsCertReader);

            // for non-tls use
            // NettyChannelBuilder.forTarget("digibankpeer-api.127-0-0-1.nip.io:8080").usePlaintext().build();
            LOG.info("Using hostport " + hostport + " sslHostOverride=" + sslHostOverride);
            grpcChannel = NettyChannelBuilder.forTarget(hostport)
                    .sslContext(GrpcSslContexts.forClient().trustManager(tlsCert).build())
                    .overrideAuthority(sslHostOverride).build();

            grpcStateChange = () -> {
                ConnectivityState newstate = grpcChannel.getState(false);
                if (newstate == ConnectivityState.READY || newstate == ConnectivityState.IDLE) {
                    this.running = true;
                } else {
                    this.running = false;
                }
                LOG.info(() -> "grpcChannel changed state " + newstate.toString());
                LOG.info(() -> "grpcChannel authority " + grpcChannel.authority());
                LOG.info(() -> "grpcChannel " + grpcChannel.toString());
                grpcChannel.notifyWhenStateChanged(newstate, grpcStateChange);
            };

            // bootstrap the grpcStateChange
            new Thread(grpcStateChange, "gRPC-state-watchdog").start();

            // setup the loading of the identities
            idWallet = new JsonIdAdapter(Paths.get(walletDir));

            // create the executor thread pool for the async handling of tx submissions
            executor = Executors.newFixedThreadPool(10);
        } catch (IOException | CertificateException e) {
            LOG.severe("Failed to establish the gRPC connection " + e.getMessage());
            Quarkus.blockingExit();
            throw new RuntimeException("Failed to establish the gRPC connection", e);
        }
        return grpcChannel;

    }

    @PreDestroy
    public void shutdown() {
        LOG.info("Sending request to shutdown gRPC channel");
        grpcChannel.shutdown();
        try {
            grpcChannel.awaitTermination(30, TimeUnit.SECONDS);
            LOG.info("termination completed");
        } catch (InterruptedException ie) {
            LOG.severe("Got interrupted exception on gRPC shutdown");
        }
    }

    /**
     * Get the service for ledgerable contract
     * 
     * @param userid ID to use for connecting and signing
     * @return LedgerableEventService
     * @throws FabricServiceException
     */
    public LedgerableEventService getLedgerableService(String userid) throws FabricServiceException {
        LOG.info("getLedgerableService " + userid + " >>");

        if (!this.running) {
            throw new FabricServiceException("Unable to accept requests yet", true);
        }

        try {
            Identity id = idWallet.getIdentity(userid);
            Signer signer = idWallet.getSigner(userid);
            Gateway.Builder builder = Gateway.newInstance().identity(id).signer(signer).connection(grpcChannel);

            LedgerableEventService les = (LedgerableEventService) new LedgerableEventService(txStore, executor)
                    .setGatewayBuilder(builder).setContractName(contract).setNetworkChannel(channel);
            les.startEvents();
            return les;
        } catch (InvalidKeyException | CertificateException e) {
            LOG.severe("Failed to create gateway builder " + e.getMessage());
            throw (FabricServiceException) new FabricServiceException("Failed to create gateway builder", false)
                    .initCause(e);
        } catch (IOException e) {
            LOG.severe("Failed with error" + e.getMessage());
            throw (FabricServiceException) new FabricServiceException("Unclear what to do", true).initCause(e);
        }
    }

}
