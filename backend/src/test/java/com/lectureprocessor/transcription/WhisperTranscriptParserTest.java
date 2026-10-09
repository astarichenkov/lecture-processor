package com.lectureprocessor.transcription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;
import com.lectureprocessor.domain.Transcript;

class WhisperTranscriptParserTest {

    private final WhisperTranscriptParser parser = new WhisperTranscriptParser(new ObjectMapper());

    @Test
    void parsesOffsetsInMilliseconds() {
        String json = """
                {
                  "transcription": [
                    {"offsets": {"from": 10500, "to": 16200}, "text": " Сегодня мы рассмотрим..."},
                    {"offsets": {"from": 16200, "to": 20000}, "text": " Тема лекции."}
                  ]
                }
                """;

        Transcript transcript = parser.parse(json);

        assertThat(transcript.segments()).hasSize(2);
        assertThat(transcript.segments().getFirst().startMs()).isEqualTo(10500);
        assertThat(transcript.segments().getFirst().endMs()).isEqualTo(16200);
        assertThat(transcript.segments().getFirst().sequence()).isZero();
        assertThat(transcript.segments().get(1).sequence()).isEqualTo(1);
    }

    @Test
    void parsesTimestampStringsWhenOffsetsMissing() {
        String json = """
                {
                  "segments": [
                    {"timestamps": {"from": "00:00:10,500", "to": "00:00:16,200"}, "text": "Привет"}
                  ]
                }
                """;

        Transcript transcript = parser.parse(json);

        assertThat(transcript.segments()).hasSize(1);
        assertThat(transcript.segments().getFirst().startMs()).isEqualTo(10500);
        assertThat(transcript.segments().getFirst().endMs()).isEqualTo(16200);
    }

    @Test
    void skipsBlankSegments() {
        String json = """
                {"transcription": [{"offsets":{"from":0,"to":1},"text":"  "},
                                   {"offsets":{"from":1,"to":2},"text":"ok"}]}
                """;
        assertThat(parser.parse(json).segments()).hasSize(1);
    }

    @Test
    void rejectsEmptyJson() {
        assertThatThrownBy(() -> parser.parse("{}")).isInstanceOf(TranscriptionException.class);
        assertThatThrownBy(() -> parser.parse("")).isInstanceOf(TranscriptionException.class);
    }

    @Test
    void preservesRawJson() {
        String json = "{\"transcription\":[{\"offsets\":{\"from\":0,\"to\":1},\"text\":\"a\"}]}";
        assertThat(parser.parse(json).rawJson()).isEqualTo(json);
    }
}
