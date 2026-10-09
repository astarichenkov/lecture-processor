package com.lectureprocessor.slides;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Difference hash (dHash) for near-duplicate frame detection. 64-bit hash based
 * on horizontal gradients, computed on a 9x8 grayscale thumbnail.
 */
public final class PerceptualHash {

    private static final int WIDTH = 9;
    private static final int HEIGHT = 8;

    private PerceptualHash() {
    }

    public static long of(BufferedImage source) {
        BufferedImage gray = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = gray.createGraphics();
        graphics.drawImage(source, 0, 0, WIDTH, HEIGHT, null);
        graphics.dispose();

        long hash = 0L;
        int bit = 0;
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH - 1; x++) {
                int left = gray.getRaster().getSample(x, y, 0);
                int right = gray.getRaster().getSample(x + 1, y, 0);
                if (left > right) {
                    hash |= (1L << bit);
                }
                bit++;
            }
        }
        return hash;
    }

    public static int hammingDistance(long a, long b) {
        return Long.bitCount(a ^ b);
    }

    public static String toHex(long hash) {
        return "%016x".formatted(hash);
    }

    public static long fromHex(String hex) {
        return Long.parseUnsignedLong(hex, 16);
    }
}
