# Day 27 — test results

Environment: Windows, Java 25.0.2 (BellSoft), Maven 3.9.11, Spring Boot 4.1.1.

## Executed

Command: `./mvnw -pl backend test`

| Test class | Tests | Result |
|-----------|-------|--------|
| WhisperTranscriptParserTest | 5 | PASS |
| TextChunkerTest | 5 | PASS |
| TextProcessingServiceTest | 3 | PASS |
| DocxExporterTest | 2 | PASS |
| DefaultProcessExecutorTest | 3 | PASS |
| FfmpegServiceTest | 4 | PASS |
| LectureServiceTest | 5 | PASS |
| LectureApiControllerTest | 6 | PASS |
| (Day 28/29/30 tests in the same suite) | — | PASS |
| **Backend total** | **53** | **PASS** |

## Verification status

| Item | Status | Comment |
|------|--------|---------|
| Compile (Java 25) | PASS | `mvn verify` |
| Unit/integration tests | PASS | 53/53 |
| HTTP endpoints (MockMvc) | PASS | upload/process/list/download/errors |
| Real FFmpeg audio extraction | SKIPPED | FFmpeg not installed in this environment |
| Real whisper.cpp transcription | SKIPPED | whisper.cpp + model not installed |
| Real Ollama + Qwen3 generation | SKIPPED | Ollama not installed |
| End-to-end MP4 → DOCX | SKIPPED | depends on the three tools above |

No external-tool run is labelled as PASS. A smoke script is provided:
`scripts/smoke-transcription.sh` (executable when tools are present).

## Reproduce

```bash
./mvnw -pl backend test
./mvnw verify
```
