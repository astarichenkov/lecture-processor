package com.lectureprocessor.transcription;

import java.nio.file.Path;

import com.lectureprocessor.domain.Transcript;

/**
 * Converts extracted audio into a timestamped transcript.
 */
public interface TranscriptionService {

    Transcript transcribe(Path audio);
}
