package com.lectureprocessor.process;

import java.time.Duration;

/**
 * Result of an external process invocation.
 */
public record ProcessResult(
        int exitCode,
        String stdout,
        String stderr,
        Duration duration,
        boolean timedOut) {

    public boolean isSuccess() {
        return !timedOut && exitCode == 0;
    }

    public String outputForError() {
        String err = stderr == null ? "" : stderr.strip();
        if (!err.isEmpty()) {
            return err.length() > 2000 ? err.substring(err.length() - 2000) : err;
        }
        String out = stdout == null ? "" : stdout.strip();
        return out.length() > 2000 ? out.substring(out.length() - 2000) : out;
    }
}
