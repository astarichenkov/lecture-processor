package com.lectureprocessor.text;

/**
 * One generated section of structured notes with the time span of its source
 * transcript chunk. Used to align extracted slides with notes.
 */
public record NoteSection(int index, long startMs, long endMs, String markdown) {
}
