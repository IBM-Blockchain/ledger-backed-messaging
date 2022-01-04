package org.ledgerable.fabric.mongo;

import java.util.UUID;

import com.mongodb.MongoClientSettings;

import org.bson.BsonReader;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.CollectibleCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.ledgerable.adts.LedgerableChaincodeEvent;

public class LedgerableChaincodeEventCodec implements CollectibleCodec<LedgerableChaincodeEvent> {

    private final Codec<Document> documentCodec;

    public LedgerableChaincodeEventCodec() {
        this.documentCodec = MongoClientSettings.getDefaultCodecRegistry().get(Document.class);
    }

    @Override
    public void encode(BsonWriter writer, LedgerableChaincodeEvent evt, EncoderContext encoderContext) {
        Document doc = new Document();
        doc.put("appReqId", evt.appReqId);
        doc.put("eventId", evt.eventId);
        doc.put("eventName", evt.eventName);
        doc.put("fabricTxId", evt.fabricTxId);
        documentCodec.encode(writer, doc, encoderContext);
    }

    @Override
    public Class<LedgerableChaincodeEvent> getEncoderClass() {
        return LedgerableChaincodeEvent.class;
    }

    @Override
    public LedgerableChaincodeEvent generateIdIfAbsentFromDocument(LedgerableChaincodeEvent document) {
        if (!documentHasId(document)) {
            document._id = (UUID.randomUUID().toString());
        }
        return document;
    }

    @Override
    public boolean documentHasId(LedgerableChaincodeEvent document) {
        return document._id != null;
    }

    @Override
    public BsonValue getDocumentId(LedgerableChaincodeEvent document) {
        return new BsonString(document._id);
    }

    @Override
    public LedgerableChaincodeEvent decode(BsonReader reader, DecoderContext decoderContext) {
        Document document = documentCodec.decode(reader, decoderContext);
        LedgerableChaincodeEvent evt = new LedgerableChaincodeEvent();
        if (document.getString("id") != null) {
            evt._id = document.getString("id");
        }
        evt.appReqId = document.getString("appReqId");
        evt.eventId = document.getString("eventId");
        evt.eventName = document.getString("eventName");
        evt.fabricTxId = document.getString("fabricTxId");
        return evt;
    }
}
