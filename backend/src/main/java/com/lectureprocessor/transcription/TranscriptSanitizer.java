package com.lectureprocessor.transcription;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.lectureprocessor.domain.Transcript;
import com.lectureprocessor.domain.TranscriptSegment;

/**
 * Removes well-known whisper.cpp hallucinations and non-speech markers that the
 * model emits on silence or background noise (e.g. the Russian subtitle-credit
 * hallucinations "Редактор субтитров … Корректор …", "[BLANK_AUDIO]",
 * "Продолжение следует"). This prevents silent/near-silent videos from being
 * reported as successfully transcribed.
 *
 * <p>The filter is intentionally conservative: only whole segments that are
 * pure credits/markers or contain no letters are dropped, so genuine speech is
 * preserved.
 */
@Component
public class TranscriptSanitizer {

    private static final List<Pattern> HALLUCINATION_PATTERNS = List.of(
            Pattern.compile("редактор субтитров"),
            Pattern.compile("корректор"),
            Pattern.compile("субтитры сделал"),
            Pattern.compile("субтитры подготовил"),
            Pattern.compile("продолжение следует"),
            Pattern.compile("спасибо за просмотр"),
            Pattern.compile("подписывайтесь на канал"),
            Pattern.compile("ставьте лайк"),
            Pattern.compile("\\[blank_audio\\]"),
            Pattern.compile("\\[\\s*(тишина|silence|music|музыка|аплодисменты)\\s*\\]"),
            Pattern.compile("thank you for watching"),
            Pattern.compile("subscribe to"),
            Pattern.compile("subtitles? by"),
            Pattern.compile("transcription by"),
            Pattern.compile("amara\\.org"));

    private static final Pattern HAS_LETTER = Pattern.compile(".*\\p{L}.*");

    /** Returns a transcript with hallucination/non-speech segments removed and renumbered. */
    public Transcript sanitize(Transcript transcript) {
        if (transcript == null) {
            return null;
        }
        List<TranscriptSegment> kept = new ArrayList<>();
        int sequence = 0;
        for (TranscriptSegment segment : transcript.segments()) {
            if (isSpeech(segment.text())) {
                kept.add(new TranscriptSegment(sequence++, segment.startMs(), segment.endMs(), segment.text()));
            }
        }
        return new Transcript(kept, transcript.rawJson());
    }

    boolean isSpeech(String rawText) {
        if (rawText == null) {
            return false;
        }
        String normalized = rawText.strip().toLowerCase().replace('ё', 'е');
        if (normalized.isEmpty()) {
            return false;
        }
        // Only punctuation/symbols/markers, no actual letters.
        if (!HAS_LETTER.matcher(normalized).matches()) {
            return false;
        }
        for (Pattern pattern : HALLUCINATION_PATTERNS) {
            if (pattern.matcher(normalized).find()) {
                return false;
            }
        }
        return true;
    }
}
