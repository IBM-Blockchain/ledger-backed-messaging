// SPDX-License-Identifier: Apache-2.0

package org.ledgerable.fabric;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;

import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.Network;
import org.hyperledger.fabric.protos.gateway.ErrorDetail;

import io.grpc.protobuf.StatusProto;
/**
 * Abstract class that provides support for defining 'services' A service should
 * provide a client endpoint for a given contract
 */
public class FabricService {
    private static final Logger LOGGER = Logger.getLogger(FabricService.class.getName());
    protected Gateway.Builder builder;

    protected String networkChannel;
    protected String contractName;

    protected TxRequestStore txStore;
    protected ExecutorService executor;

    public FabricService(TxRequestStore tStore, ExecutorService executor) {
        this.txStore = tStore;
        this.executor = executor;
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
        LOGGER.info(_processException(ex, 1));
    }

    protected String _processException(final Throwable ex, int depth) {
        StringBuilder builder = new StringBuilder(System.lineSeparator());
        builder.append("=".repeat(depth)).append(" Class      ::" + ex.getClass()).append(System.lineSeparator());
        builder.append("=".repeat(depth)).append(" Message    ::" + ex.getMessage()).append(System.lineSeparator());
        builder.append("=".repeat(depth)).append(" Suppressed ::" + ex.getSuppressed().length)
                .append(System.lineSeparator());
        ;
        // builder.append("=".repeat(depth)).append("----------------------------------------")
        // .append(System.lineSeparator());
        if (ex instanceof io.grpc.StatusRuntimeException) {
            builder.append("=".repeat(depth))
                    .append("Status     ::" + ((io.grpc.StatusRuntimeException) ex).getStatus())
                    .append(System.lineSeparator());
            ;
            builder.append("=".repeat(depth))
                    .append("Metadata   ::" + ((io.grpc.StatusRuntimeException) ex).getTrailers())
                    .append(System.lineSeparator());
        }
        com.google.rpc.Status status = StatusProto.fromThrowable(ex);

        if (status != null) {
            for (Any any : status.getDetailsList()) {
                try {
                    
                    ErrorDetail ee = ErrorDetail.parseFrom(any.getValue());
                    if (ee != null) {
                        builder.append("=".repeat(depth)).append(ee.getMspId()).append(System.lineSeparator());
                        builder.append("=".repeat(depth)).append(ee.getAddress()).append(System.lineSeparator());
                        builder.append("=".repeat(depth)).append(ee.getMessage()).append(System.lineSeparator());
                    }
                } catch (InvalidProtocolBufferException e) {
                    System.out.println(e);
                }

            }
        }
        // builder.append("=".repeat(depth)).append("----------------------------------------")
        // .append(System.lineSeparator());
        Throwable cause = ex.getCause();
        if (cause == null) {
            builder.append("=".repeat(depth)).append("Cause      :: <none>").append(System.lineSeparator());
        } else {
            depth++;
            builder.append("=".repeat(depth - 1)).append("Cause      ::").append(_processException(cause, depth))
                    .append(System.lineSeparator());
        }

        return builder.toString();
    }
}
