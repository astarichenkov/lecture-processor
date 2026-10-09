package com.lectureprocessor.text;

/**
 * A chunk of plain text with its character offsets in the source text.
 */
public record TextChunk(int index, String text, int charStart, int charEnd) {
}
