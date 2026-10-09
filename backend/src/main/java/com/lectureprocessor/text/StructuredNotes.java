package com.lectureprocessor.text;

import java.util.List;

/**
 * Structured notes plus the metadata needed to place slides at relevant
 * positions in the exported document.
 */
public record StructuredNotes(String markdown, String summary, List<NoteSection> sections) {

    public StructuredNotes {
        sections = List.copyOf(sections);
    }
}
