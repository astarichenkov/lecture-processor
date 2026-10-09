# Day 29 — test results

## Executed automated tests

| Test class | Tests | Result |
|-----------|-------|--------|
| BenchmarkRunnerTest | 3 | PASS |
| BenchmarkServiceTest | 2 | PASS |
| PerceptualHashTest | 3 | PASS |
| DocxExporterTest (image insertion) | 2 | PASS |

These verify benchmark metric recording, SKIPPED/FAIL handling, result
serialization (CSV/Markdown), perceptual-hash dedup logic, and DOCX image
insertion.

## Benchmark runs

Status: **SKIPPED** — Ollama/Qwen3 models unavailable in this environment.
`BenchmarkRunner` correctly produces `SKIPPED` entries with the reason
"Ollama недоступна — замер не выполнялся" (verified by `BenchmarkRunnerTest`).

Real measured results must be saved under `demos/day29/benchmark-results/`
after running the benchmark with models present. No figures are fabricated here.

## Slide extraction

Status: **SKIPPED** for real video (FFmpeg absent). Unit tests verify the
deduplication and DOCX-insertion logic with synthetic images.
