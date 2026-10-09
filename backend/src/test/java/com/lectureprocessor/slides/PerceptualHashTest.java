package com.lectureprocessor.slides;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

class PerceptualHashTest {

    @Test
    void identicalImagesProduceZeroDistance() {
        BufferedImage image = gradient();
        long hash = PerceptualHash.of(image);
        assertThat(PerceptualHash.hammingDistance(hash, hash)).isZero();
    }

    @Test
    void differentImagesHaveNonZeroDistance() {
        long a = PerceptualHash.of(stripes());
        long b = PerceptualHash.of(solid(Color.BLACK));
        assertThat(PerceptualHash.hammingDistance(a, b)).isGreaterThan(0);
    }

    private static BufferedImage stripes() {
        BufferedImage image = new BufferedImage(90, 80, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 90; x++) {
            int color = (x / 10) % 2 == 0 ? 0xFFFFFF : 0x000000;
            for (int y = 0; y < 80; y++) {
                image.setRGB(x, y, color);
            }
        }
        return image;
    }

    @Test
    void hexRoundTrip() {
        long hash = PerceptualHash.of(gradient());
        assertThat(PerceptualHash.fromHex(PerceptualHash.toHex(hash))).isEqualTo(hash);
    }

    private static BufferedImage gradient() {
        BufferedImage image = new BufferedImage(90, 80, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 90; x++) {
            for (int y = 0; y < 80; y++) {
                image.setRGB(x, y, (x * 2) << 16 | (y * 3) << 8 | 40);
            }
        }
        return image;
    }

    private static BufferedImage solid(Color color) {
        BufferedImage image = new BufferedImage(90, 80, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, 90, 80);
        g.dispose();
        return image;
    }
}
