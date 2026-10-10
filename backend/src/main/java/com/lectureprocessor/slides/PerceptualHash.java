package com.lectureprocessor.slides;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;

/**
 * Difference hash (dHash) for near-duplicate frame detection. 64-bit hash based
 * on horizontal gradients, computed on a 9x8 grayscale thumbnail.
 *
 * <p>The thumbnail is produced with {@link Image#getScaledInstance} rather than a
 * scaled {@code Graphics2D.drawImage}: for color-managed JPEG sources (ICC
 * profile, {@code TYPE_3BYTE_BGR}) the latter rendered a flat background under
 * headless Java, yielding an all-zero hash for every real frame.
 */
public final class PerceptualHash {

    private static final int WIDTH = 9;
    private static final int HEIGHT = 8;

    private PerceptualHash() {
    }

    public static long of(BufferedImage source) {
        if (source == null) {
            return 0L;
        }
        Image scaled = source.getScaledInstance(WIDTH, HEIGHT, Image.SCALE_AREA_AVERAGING);
        BufferedImage small = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = small.createGraphics();
        graphics.drawImage(scaled, 0, 0, null);
        graphics.dispose();

        long hash = 0L;
        int bit = 0;
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH - 1; x++) {
                if (luminance(small.getRGB(x, y)) > luminance(small.getRGB(x + 1, y))) {
                    hash |= (1L << bit);
                }
                bit++;
            }
        }
        return hash;
    }

    private static int luminance(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (r * 299 + g * 587 + b * 114) / 1000;
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
