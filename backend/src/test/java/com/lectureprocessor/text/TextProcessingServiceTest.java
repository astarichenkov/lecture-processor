package com.lectureprocessor.text;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.lectureprocessor.domain.TranscriptSegment;
import com.lectureprocessor.llm.LlmOptions;
import com.lectureprocessor.support.FakeLlmProvider;
import com.lectureprocessor.support.TestProperties;

class TextProcessingServiceTest {

    private final FakeLlmProvider llm = new FakeLlmProvider();
    private final PromptTemplates prompts = new PromptTemplates();
    private final TextChunker chunker = new TextChunker();
    private final TextProcessingService service = new TextProcessingService(llm, chunker, prompts,
            TestProperties.defaults("./target/test-data"));

    private final List<TranscriptSegment> segments = List.of(
            new TranscriptSegment(0, 0, 1000, "Определение: вектор — это направленный отрезок."),
            new TranscriptSegment(1, 1000, 2000, "Далее рассмотрим скалярное произведение и его свойства."));

    @Test
    void cleansTranscriptUsingLlm() {
        llm.setChatFunction((system, user) -> "Исправленный фрагмент.");
        String cleaned = service.cleanTranscript(segments, LlmOptions.of("fake-model"));
        assertThat(cleaned).contains("Исправленный фрагмент.");
    }

    @Test
    void buildsStructuredNotesWithSummary() {
        llm.setChatFunction((system, user) -> "## Раздел\n\nТекст конспекта.");
        StructuredNotes notes = service.generateStructuredNotes(segments, LlmOptions.of("fake-model"));

        assertThat(notes.markdown()).contains("# Конспект лекции");
        assertThat(notes.markdown()).contains("## Резюме");
        assertThat(notes.sections()).isNotEmpty();
        assertThat(notes.sections().getFirst().startMs()).isZero();
    }

    @Test
    void rejectsEmptyLlmOutput() {
        llm.setChatFunction((system, user) -> "   ");
        assertThatThrownBy(() -> service.cleanTranscript(segments, LlmOptions.of("fake-model")))
                .isInstanceOf(ProcessingException.class);
    }
}
