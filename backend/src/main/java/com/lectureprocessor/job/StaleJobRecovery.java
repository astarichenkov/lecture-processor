package com.lectureprocessor.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

import com.lectureprocessor.domain.JobStatus;
import com.lectureprocessor.repository.LectureRepository;

/**
 * On startup, marks lectures left in a non-terminal state by a previous run as
 * FAILED, so the UI never shows a job stuck "in progress" forever.
 */
@Component
public class StaleJobRecovery implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StaleJobRecovery.class);

    private final LectureRepository lectureRepository;

    public StaleJobRecovery(LectureRepository lectureRepository) {
        this.lectureRepository = lectureRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        lectureRepository.findAll().stream()
                .filter(lecture -> !lecture.status().isTerminal())
                .forEach(lecture -> {
                    log.warn("Recovering stale lecture {} in status {}", lecture.id(), lecture.status());
                    lectureRepository.save(lecture.withError(
                            "Обработка была прервана (перезапуск приложения). Запустите обработку заново."));
                });
    }
}
