package com.lectureprocessor.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Converts embedding vectors to/from a compact little-endian byte
 * representation so they can be stored in SQLite BLOBs.
 */
public final class EmbeddingCodec {

    private EmbeddingCodec() {
    }

    public static byte[] encode(float[] vector) {
        if (vector == null) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.allocate(vector.length * Float.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        for (float value : vector) {
            buffer.putFloat(value);
        }
        return buffer.array();
    }

    public static float[] decode(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        float[] vector = new float[bytes.length / Float.BYTES];
        for (int i = 0; i < vector.length; i++) {
            vector[i] = buffer.getFloat();
        }
        return vector;
    }
}
