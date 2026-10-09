package com.lectureprocessor.document;

import org.springframework.stereotype.Component;

import com.lectureprocessor.domain.Lecture;
import com.lectureprocessor.domain.Transcript;

/**
 * Builds human-readable Markdown documents from transcript and notes.
 */
@Component
public class MarkdownBuilder {

    public String rawTranscriptMarkdown(Lecture lecture, Transcript transcript) {
        StringBuilder md = new StringBuilder();
        md.append("# Расшифровка лекции\n\n");
        md.append("**Файл:** ").append(lecture.originalFilename()).append("\n\n");
        md.append("## Расшифровка\n\n");
        for (var segment : transcript.segments()) {
            md.append("**[").append(Transcript.formatTimestamp(segment.startMs())).append("]** ")
                    .append(segment.text())
                    .append("\n\n");
        }
        return md.toString().strip();
    }
}
