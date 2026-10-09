package com.lectureprocessor.document;

import java.nio.file.Path;

/**
 * A slide image to embed in the DOCX, with the timestamp used for alignment.
 */
public record SlideImage(long timestampMs, Path imagePath, String caption) {
}
