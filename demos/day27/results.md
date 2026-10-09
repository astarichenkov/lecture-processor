# Day 27 — results

## Artifacts produced by a real run

For each completed lecture, under `data/lectures/<id>/`:

| File | Content |
|------|---------|
| `source.*` | Original uploaded video |
| `audio.wav` | 16 kHz mono PCM extracted by FFmpeg |
| `transcript.json` | Untouched whisper.cpp JSON with timestamps |
| `transcript.md` | Human-readable timestamped transcript |
| `notes.md` | Structured Markdown notes (headings, definitions, summary) |
| `notes.docx` | DOCX with headings and rendered paragraphs |

## Pipeline behaviour

- Status is persisted at every stage; interrupted jobs are recovered as FAILED on startup.
- Long lectures are chunked (`app.processing.llm-chunk-chars`, default 6000)
  so no single LLM request is unbounded.
- LLM output is validated (non-empty, non-trivial) before documents are generated.

## Measured in this environment

No real end-to-end measurement was possible (FFmpeg/whisper/Ollama absent).
See `demos/day27/test-results.md`. All figures from real runs must be recorded
here by the operator after running the demo scenario.
