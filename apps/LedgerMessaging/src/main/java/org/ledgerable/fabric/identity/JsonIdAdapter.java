package org.ledgerable.fabric.identity;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;

import org.hyperledger.fabric.client.identity.Identities;
import org.hyperledger.fabric.client.identity.Identity;
import org.hyperledger.fabric.client.identity.Signer;
import org.hyperledger.fabric.client.identity.Signers;
import org.hyperledger.fabric.client.identity.X509Identity;

public class JsonIdAdapter {
    private static final Logger LOGGER = Logger.getLogger(JsonIdAdapter.class.getName());
    public static final String JSON_VERSION = "version";
    public static final String JSON_TYPE = "type";
    public static final String JSON_MSP_ID = "mspId";

    private static final String JSON_CREDENTIALS="credentials";
    private static final String JSON_CERTIFICATE = "certificate";
    private static final String JSON_PRIVATE_KEY = "privateKey";
    private static final String DATA_FILE_EXTENTION = ".id";

    private final Path storePath;

    public JsonIdAdapter(final Path storePath) {
        this.storePath = storePath;

        if (!Files.exists(storePath)) {
            throw new RuntimeException("Directory "+storePath+" does not exist");
        }

        LOGGER.info("Reading for "+storePath);

    }

    private Path getPathForLabel(final String label) {
        return storePath.resolve(label + DATA_FILE_EXTENTION);
    }

    public Identity getIdentity(String id) throws CertificateException, IOException {
        JsonObject json = readFile(id);
        String mspId = json.getString(JSON_MSP_ID);
        JsonObject credentials = json.getJsonObject(JSON_CREDENTIALS);
        String certificatePem = credentials.getString(JSON_CERTIFICATE);

        X509Certificate certificate = Identities.readX509Certificate(new StringReader(certificatePem));
        return new X509Identity(mspId, certificate);
    }

    public Signer getSigner(String id) throws InvalidKeyException, IOException {
        JsonObject json = readFile(id);
        JsonObject credentials = json.getJsonObject(JSON_CREDENTIALS);
        String privateKeyPem = credentials.getString(JSON_PRIVATE_KEY);
        
        PrivateKey privateKey = Identities.readPrivateKey(new StringReader(privateKeyPem));

        return Signers.newPrivateKeySigner(privateKey);
    }

    private JsonObject readFile(final String label) throws IOException {
        InputStream identityData = Files.newInputStream(getPathForLabel(label));
        JsonObject identityJson = Json.createReader(identityData).readObject();
        return identityJson;
    }
}
