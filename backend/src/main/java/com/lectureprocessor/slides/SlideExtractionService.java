package com.lectureprocessor.slides;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.lectureprocessor.config.AppProperties;
import com.lectureprocessor.domain.Slide;
import com.lectureprocessor.repository.SlideRepository;
import com.lectureprocessor.storage.FileStorageService;
import com.lectureprocessor.video.FfmpegService;
import com.lectureprocessor.video.FfmpegService.ExtractedFrame;

/**
 * Extracts presentation frames from a video, removes near-duplicates using a
 * perceptual hash and persists the selected slides with their timestamps.
 */
@Service
public class SlideExtractionService {

    private static final Logger log = LoggerFactory.getLogger(SlideExtractionService.class);

    private final FfmpegService ffmpeg;
    private final FileStorageService storage;
    private final SlideRepository slideRepository;
    private final AppProperties properties;

    public SlideExtractionService(FfmpegService ffmpeg, FileStorageService storage,
            SlideRepository slideRepository, AppProperties properties) {
        this.ffmpeg = ffmpeg;
        this.storage = storage;
        this.slideRepository = slideRepository;
        this.properties = properties;
    }

    public List<Slide> extract(String lectureId, Path video) {
        AppProperties.Slides config = properties.slides();
        Path outputDir = storage.slidesDir(lectureId);
        // Extract more candidates than the final limit, then deduplicate.
        int candidateLimit = Math.max(config.maxImages() * 2, config.maxImages());
        List<ExtractedFrame> frames = ffmpeg.extractFrames(video, outputDir,
                config.intervalSeconds(), candidateLimit);

        List<Slide> slides = new ArrayList<>();
        // Compare against ALL kept hashes, not just the previous frame: a slide
        // can reappear later in the lecture (e.g. a repeated title) and must
        // still be deduplicated.
        List<Long> keptHashes = new ArrayList<>();
        for (ExtractedFrame frame : frames) {
            if (slides.size() >= config.maxImages()) {
                deleteQuietly(frame.path());
                continue;
            }
            long hash = hashOf(frame.path());
            boolean duplicate = keptHashes.stream()
                    .anyMatch(kept -> PerceptualHash.hammingDistance(kept, hash)
                            < config.hashDistanceThreshold());
            if (duplicate) {
                deleteQuietly(frame.path());
                continue;
            }
            keptHashes.add(hash);
            slides.add(new Slide(0, lectureId, slides.size(), frame.timestampMs(),
                    frame.path().toAbsolutePath().toString(), PerceptualHash.toHex(hash)));
        }
        slideRepository.replaceAll(lectureId, slides);
        log.info("Extracted {} unique slides from {} candidates", slides.size(), frames.size());
        return slideRepository.findByLecture(lectureId);
    }

    private static long hashOf(Path frame) {
        try {
            BufferedImage image = ImageIO.read(frame.toFile());
            if (image == null) {
                return 0;
            }
            return PerceptualHash.of(image);
        } catch (IOException e) {
            log.warn("Cannot read frame {}: {}", frame, e.getMessage());
            return 0;
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // best effort
        }
    }
}
