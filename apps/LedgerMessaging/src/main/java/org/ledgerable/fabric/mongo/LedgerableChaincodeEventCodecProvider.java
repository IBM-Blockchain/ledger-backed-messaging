package org.ledgerable.fabric.mongo;

import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.ledgerable.adts.LedgerableChaincodeEvent;

public class LedgerableChaincodeEventCodecProvider implements CodecProvider {
    @Override
    public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
        if (clazz == LedgerableChaincodeEvent.class) {
            return (Codec<T>) new LedgerableChaincodeEventCodec();
        }
        return null;
    }

}