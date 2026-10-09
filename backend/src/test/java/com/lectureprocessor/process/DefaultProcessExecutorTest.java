package com.lectureprocessor.process;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

class DefaultProcessExecutorTest {

    private final DefaultProcessExecutor executor = new DefaultProcessExecutor();

    @Test
    void executesProcessAndCapturesOutput() {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        ProcessResult result = executor.execute(List.of(java, "-version"), Duration.ofSeconds(30), null);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.stderr()).contains("version");
        assertThat(result.exitCode()).isZero();
    }

    @Test
    void throwsForMissingBinary() {
        assertThatThrownBy(() -> executor.execute(
                List.of("definitely-not-a-real-binary-12345"), Duration.ofSeconds(5), null))
                .isInstanceOf(ProcessExecutionException.class);
    }

    @Test
    void capturesNonZeroExitCode() {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        ProcessResult result = executor.execute(List.of(java, "-bad-flag-xyz"),
                Duration.ofSeconds(30), null);
        assertThat(result.isSuccess()).isFalse();
    }
}
