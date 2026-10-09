# Day 29 — LLM optimization + presentation slides

## Part A — benchmark module

- `BenchmarkRunner` executes configurations and records measured metrics:
  model, quantization, prompt version, context length, temperature, estimated
  input/output tokens, total time, tokens/s, peak JVM heap delta, error count,
  output validity, key-fact preservation.
- VRAM is reported as `unknown (Ollama runs out-of-process)` — never invented.
- Configurations A–D in `benchmark-config.json`.
- Results written to `<storage>/benchmarks/<timestamp>/` as JSON, CSV and Markdown.
- Deterministic checks: output length validity and key-fact containment.
  Subjective quality is explicitly out of scope for automation.

Versioned prompts: `basic-summary-v1`, `structured-notes-v1`, `clean-transcript-v1`,
`summary-v1`, `rag-answer-v1`.

## Part B — slides

- `FfmpegService.extractFrames` samples frames every `app.slides.interval-seconds`.
- `PerceptualHash` (dHash, 64-bit) + Hamming distance removes near-duplicates.
- `max-images` limit prevents hundreds of identical images.
- Slides are associated with transcript time spans; `DocxExporter` inserts each
  image into the notes section whose time span contains the slide timestamp,
  remaining slides go to a chronological appendix.

## UI

- `/benchmarks` lists the configured benchmark variants.
- Slide images are downloadable via `/api/lectures/{id}/slides/{index}/image`.

## Run benchmark

```bash
curl -X POST "http://localhost:8080/api/benchmarks/run?lectureId=<id>"
```
