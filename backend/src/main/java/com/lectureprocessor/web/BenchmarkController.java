package com.lectureprocessor.web;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.lectureprocessor.benchmark.BenchmarkConfig;
import com.lectureprocessor.lecture.LectureService;
import com.lectureprocessor.repository.SegmentRepository;
import com.lectureprocessor.storage.FileStorageService;
import com.lectureprocessor.benchmark.BenchmarkService;

/**
 * Benchmark API: shows the default configuration and runs benchmarks against a
 * lecture transcript (Day 29).
 */
@RestController
@RequestMapping("/api/benchmarks")
public class BenchmarkController {

    private final BenchmarkService benchmarkService;
    private final LectureService lectureService;
    private final SegmentRepository segmentRepository;
    private final FileStorageService storage;

    public BenchmarkController(BenchmarkService benchmarkService, LectureService lectureService,
            SegmentRepository segmentRepository, FileStorageService storage) {
        this.benchmarkService = benchmarkService;
        this.lectureService = lectureService;
        this.segmentRepository = segmentRepository;
        this.storage = storage;
    }

    @GetMapping("/config")
    public BenchmarkConfig config() {
        return benchmarkService.loadDefaultConfig();
    }

    @PostMapping("/run")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> run(@RequestParam("lectureId") String lectureId,
            @RequestParam(name = "configPath", required = false) String configPath) {
        lectureService.getRequired(lectureId);
        BenchmarkConfig config = configPath == null
                ? benchmarkService.loadDefaultConfig()
                : benchmarkService.loadConfig(Path.of(configPath));
        String source = segmentRepository.findByLecture(lectureId).stream()
                .map(s -> s.text())
                .reduce("", (a, b) -> a + " " + b)
                .strip();
        if (source.isEmpty()) {
            throw new IllegalArgumentException("У лекции нет транскрипта для бенчмарка");
        }
        Path dir = benchmarkService.runAndPersist(config, source);
        return Map.of("resultsDir", dir.toString());
    }
}
