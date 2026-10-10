package com.lectureprocessor.slides;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

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
    void realJpegHashIsNotZero() throws Exception {
        // Regression: color-managed JPEG sources (ICC, TYPE_3BYTE_BGR) used to
        // scale down to a flat background via Graphics2D.drawImage, yielding an
        // all-zero hash for every real frame.
        Path file = Path.of("target/test-data/phash-" + System.nanoTime() + ".jpg");
        Files.createDirectories(file.getParent());
        BufferedImage source = new BufferedImage(320, 180, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = source.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 320, 180);
        g.setColor(Color.BLACK);
        g.fillRect(40, 40, 120, 80);
        g.dispose();
        ImageIO.write(source, "jpg", file.toFile());

        BufferedImage jpeg = ImageIO.read(file.toFile());
        long hash = PerceptualHash.of(jpeg);

        assertThat(PerceptualHash.hammingDistance(hash, 0L)).isGreaterThan(0);
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
