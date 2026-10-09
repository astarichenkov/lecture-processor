package com.lectureprocessor.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmbeddingCodecTest {

    @Test
    void encodesAndDecodesRoundTrip() {
        float[] vector = { 0.1f, -2.5f, 3.75f, 0f };
        float[] decoded = EmbeddingCodec.decode(EmbeddingCodec.encode(vector));
        assertThat(decoded).containsExactly(vector);
    }

    @Test
    void nullVectorRoundTripsToNull() {
        assertThat(EmbeddingCodec.encode(null)).isNull();
        assertThat(EmbeddingCodec.decode(null)).isNull();
    }
}
