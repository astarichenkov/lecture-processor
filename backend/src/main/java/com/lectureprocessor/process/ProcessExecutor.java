package com.lectureprocessor.process;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * Abstraction over external process execution. Implementations must be safe
 * with user-supplied paths (no shell interpolation) and must enforce timeouts.
 */
public interface ProcessExecutor {

    ProcessResult execute(List<String> command, Duration timeout, Path workingDirectory);
}
