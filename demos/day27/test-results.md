# Day 27 — test results

Environment: Windows, Java 25.0.2 (BellSoft), Maven 3.9.11, Spring Boot 4.1.1.

## Executed

Command: `./mvnw -pl backend test`

| Test class | Tests | Result |
|-----------|-------|--------|
| WhisperTranscriptParserTest | 5 | PASS |
| TranscriptSanitizerTest | 6 | PASS |
| OllamaLlmProviderTest | 2 | PASS |
| TextChunkerTest | 5 | PASS |
| TextProcessingServiceTest | 3 | PASS |
| DocxExporterTest | 2 | PASS |
| DefaultProcessExecutorTest | 3 | PASS |
| FfmpegServiceTest | 4 | PASS |
| LectureServiceTest | 5 | PASS |
| LectureApiControllerTest | 6 | PASS |
| (Day 28/29 tests in the same suite) | | PASS |
| **Backend total** | **61** | **PASS** |

## Real integration run (2026-10-10)

Tools installed locally: FFmpeg 9.0.2, whisper.cpp b5454 (CPU/BLAS), model
`ggml-small.bin`. Fixture: original Russian speech (Windows SAPI "Microsoft
Irina") wrapped into a 45 s MP4 — no copyrighted/user media.

| Step | Tool | Result |
|------|------|--------|
| MP4 → WAV | FFmpeg 9.0.2 | **PASS** (16 kHz mono) |
| WAV → transcript.json | whisper.cpp real, `-l ru` | **PASS** (6 segments, 0–38.6 s) |
| JSON parsing | app parser | **PASS** |
| LLM (Ollama/Qwen3) | Ollama | **BLOCKED — not installed (disk)** |
| notes.md / notes.docx | Apache POI pipeline | **PASS** (LLM via documented test double) |

### Verified end-to-end through the running app

```
POST /api/lectures (sample-lecture-ru.mp4) → UPLOADED
POST /api/lectures/{id}/process
  EXTRACTING_AUDIO   → real ffmpeg       ✅
  TRANSCRIBING       → real whisper.cpp  ✅ transcript.json preserved
  PROCESSING_TEXT    → LLM stage
  EXPORTING          → notes.md + notes.docx ✅
  COMPLETED
```

Artifacts under `target/e2e-data/lectures/<id>/`:
`audio.wav`, `transcript.json` (raw whisper output), `transcript.md`,
`notes.md`, `notes.docx`; DB has 6 segments and 1 chunk (768-dim embedding).

## Regression found and fixed during the real run

| Issue | Before | After |
|-------|--------|-------|
| Ollama unreachable | job stuck in `PROCESSING_TEXT` >190 s, no error | `FAILED` in ~14 s with a clear message |
| Silent audio | hallucinated subtitle credits accepted → `COMPLETED` | sanitized → `FAILED: нет распознанной речи` |

## Status summary

| Item | Status |
|------|--------|
| Compile / tests | PASS (61) |
| Real FFmpeg audio extraction | PASS |
| Real whisper.cpp Russian transcription | PASS |
| Real Ollama/Qwen3 generation | BLOCKED (insufficient disk) |
| Full MP4→DOCX pipeline wiring | PASS (LLM via test double, labelled) |

Details: `docs/day27-runtime-audit.md`. A real AI E2E must be run on a host
with enough free disk; it is **not** claimed here.
