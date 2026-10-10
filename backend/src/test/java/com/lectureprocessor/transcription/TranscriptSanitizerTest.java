package com.lectureprocessor.transcription;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.lectureprocessor.domain.Transcript;
import com.lectureprocessor.domain.TranscriptSegment;

class TranscriptSanitizerTest {

    private final TranscriptSanitizer sanitizer = new TranscriptSanitizer();

    @Test
    void dropsRussianSubtitleCreditHallucination() {
        // Exact text whisper.cpp produced on a silent clip in the real run.
        assertThat(sanitizer.isSpeech(
                "Редактор субтитров А.Синецкая Корректор А.Егорова")).isFalse();
    }

    @Test
    void keepsRealSpeech() {
        assertThat(sanitizer.isSpeech("Вектор — это направленный отрезок.")).isTrue();
    }

    @Test
    void keepsSpeechMentioningSubtitlesInContext() {
        assertThat(sanitizer.isSpeech("Сегодня мы обсудим, как работают субтитры в видео.")).isTrue();
    }

    @Test
    void dropsMarkersAndPunctuationOnly() {
        assertThat(sanitizer.isSpeech("[BLANK_AUDIO]")).isFalse();
        assertThat(sanitizer.isSpeech("[ Музыка ]")).isFalse();
        assertThat(sanitizer.isSpeech("... ,, --")).isFalse();
        assertThat(sanitizer.isSpeech("  ")).isFalse();
    }

    @Test
    void removesHallucinationsAndRenumbers() {
        Transcript input = new Transcript(List.of(
                new TranscriptSegment(0, 0, 1000, "Редактор субтитров А.Синецкая"),
                new TranscriptSegment(1, 1000, 2000, "Вектор — это направленный отрезок."),
                new TranscriptSegment(2, 2000, 3000, "Продолжение следует"),
                new TranscriptSegment(3, 3000, 4000, "Скалярное произведение двух векторов.")),
                "{\"raw\":true}");

        Transcript result = sanitizer.sanitize(input);

        assertThat(result.segments()).hasSize(2);
        assertThat(result.segments().get(0).sequence()).isZero();
        assertThat(result.segments().get(1).sequence()).isEqualTo(1);
        assertThat(result.segments().get(0).text()).contains("Вектор");
        assertThat(result.rawJson()).isEqualTo("{\"raw\":true}");
    }

    @Test
    void silenceOnlyTranscriptBecomesEmpty() {
        Transcript input = new Transcript(List.of(
                new TranscriptSegment(0, 0, 3000, "[BLANK_AUDIO]")), "{}");
        assertThat(sanitizer.sanitize(input).isEmpty()).isTrue();
    }
}
