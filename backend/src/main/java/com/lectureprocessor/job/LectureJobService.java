package com.lectureprocessor.job;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

/**
 * Submits lecture processing work to the bounded executor and prevents the same
 * lecture from being processed twice concurrently.
 */
@Service
public class LectureJobService {

    private static final Logger log = LoggerFactory.getLogger(LectureJobService.class);

    private final ThreadPoolTaskExecutor executor;
    private final LectureProcessingService pipeline;
    private final Set<String> running = ConcurrentHashMap.newKeySet();

    public LectureJobService(ThreadPoolTaskExecutor lectureJobExecutor, LectureProcessingService pipeline) {
        this.executor = lectureJobExecutor;
        this.pipeline = pipeline;
    }

    public boolean submit(String lectureId) {
        if (!running.add(lectureId)) {
            log.debug("Lecture {} is already being processed", lectureId);
            return false;
        }
        try {
            executor.execute(() -> {
                try {
                    pipeline.process(lectureId);
                } catch (Exception e) {
                    log.error("Lecture {} processing ended with error: {}", lectureId, e.getMessage());
                } finally {
                    running.remove(lectureId);
                }
            });
            return true;
        } catch (TaskRejectedException e) {
            running.remove(lectureId);
            throw new JobQueueFullException("Очередь обработки переполнена, попробуйте позже");
        }
    }

    public boolean isRunning(String lectureId) {
        return running.contains(lectureId);
    }
}
