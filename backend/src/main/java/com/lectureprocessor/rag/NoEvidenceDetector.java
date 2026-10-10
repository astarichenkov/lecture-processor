package com.lectureprocessor.rag;

import java.util.regex.Pattern;

/**
 * Detects whether a generated answer is an honest refusal (no evidence in the
 * retrieved context). Used so that no-answer responses never carry fabricated
 * sources or timestamps: source attribution stays fully controlled by code.
 *
 * <p>Pure heuristics over Russian refusal phrasings; intentionally conservative
 * and unit-tested. {@code UNICODE_CHARACTER_CLASS} is required because Java's
 * {@code \w} is ASCII-only by default and would not match Cyrillic.
 */
public final class NoEvidenceDetector {

    private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
            | Pattern.UNICODE_CHARACTER_CLASS;

    private static final Pattern[] PATTERNS = {
            Pattern.compile("нет\\s+информац", FLAGS),
            Pattern.compile("информац\\w*[^.!?\\n]{0,120}\\bнет\\b", FLAGS),
            Pattern.compile("не\\s+на(й|х)ден", FLAGS),
            Pattern.compile("не\\s+содерж", FLAGS),
            Pattern.compile("не\\s+упомин", FLAGS),
            Pattern.compile("недостаточн\\w*\\s+(информац|данн|сведен)", FLAGS),
            Pattern.compile("информац\\w*\\s+недостаточн", FLAGS),
            Pattern.compile("информац\\w*[^.!?\\n]{0,120}недостаточн", FLAGS),
            Pattern.compile("в\\s+источниках\\s+не\\s+", FLAGS),
            Pattern.compile("источник\\w*\\s+не\\s+(содерж|предостав|дают|даёт)", FLAGS),
            Pattern.compile("(отсутству|не\\s+указан|не\\s+приведен|не\\s+описан|не\\s+рассмотр)\\w*", FLAGS),
            Pattern.compile("нет\\s+данн", FLAGS),
            Pattern.compile("не\\s+могу\\s+ответ", FLAGS),
    };

    private NoEvidenceDetector() {
    }

    /** Returns true only when the text is clearly a refusal based on missing evidence. */
    public static boolean isRefusal(String answer) {
        if (answer == null || answer.isBlank()) {
            return true;
        }
        for (Pattern pattern : PATTERNS) {
            if (pattern.matcher(answer).find()) {
                return true;
            }
        }
        return false;
    }
}
