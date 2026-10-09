package com.lectureprocessor.domain;

/**
 * Lifecycle of a lecture processing job. Persisted so that progress survives
 * application restarts and can be observed from the UI.
 */
public enum JobStatus {
    UPLOADED,
    EXTRACTING_AUDIO,
    TRANSCRIBING,
    PROCESSING_TEXT,
    EXPORTING,
    COMPLETED,
    FAILED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED;
    }
}
