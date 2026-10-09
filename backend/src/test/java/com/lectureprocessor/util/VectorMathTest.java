package com.lectureprocessor.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VectorMathTest {

    @Test
    void cosineOfIdenticalVectorsIsOne() {
        float[] v = { 1, 2, 3 };
        assertThat(VectorMath.cosine(v, v)).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-6));
    }

    @Test
    void cosineOfOrthogonalVectorsIsZero() {
        assertThat(VectorMath.cosine(new float[] { 1, 0 }, new float[] { 0, 1 })).isZero();
    }

    @Test
    void normalizeProducesUnitLength() {
        float[] normalized = VectorMath.normalize(new float[] { 3, 4 });
        assertThat(Math.hypot(normalized[0], normalized[1])).isCloseTo(1.0,
                org.assertj.core.data.Offset.offset(1e-6));
    }

    @Test
    void mismatchedDimensionsReturnZero() {
        assertThat(VectorMath.cosine(new float[] { 1, 2 }, new float[] { 1 })).isZero();
    }
}
