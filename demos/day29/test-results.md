# Day 29 — test results (real)

Machine: Ryzen 7 5800H / RTX 3050 Laptop 4 GB / 32 GB RAM, Windows, Java 25.
Ollama 0.40.2: `qwen3:1.7b`, `qwen3:4b`, `nomic-embed-text`.

## Automated tests

```
./mvnw verify    # → 88 tests PASS (68 backend + 20 private-ai-chat)
```

Day 29 relevant classes:

| Test class | Tests | Result |
|---|---|---|
| BenchmarkRunnerTest | 3 | PASS |
| BenchmarkServiceTest | 2 | PASS |
| PerceptualHashTest | 4 | PASS (incl. new `realJpegHashIsNotZero`) |
| SlideExtractionServiceTest | 1 | PASS (non-adjacent dedup) |
| DocxExporterTest (image insertion) | 2 | PASS |

## Real benchmark runs

Status: **DONE** — see `benchmark-results/results.json`,
`benchmark-results/results.csv`, `benchmark-results/report.md`,
`benchmark-results/chunking-results.json`.

- Matrix A (qwen3:1.7b baseline), B (v2 prompts), C (thinking), D (qwen3:4b).
- E (qwen3:8b): **SKIPPED** — 4 GB VRAM + overnight time budget.
- Representative tasks: clean transcript, structured notes, RAG answer.
- Real metrics captured from Ollama (`prompt_eval_count`, `eval_count`,
  `eval_duration`, `done_reason`), `nvidia-smi` and `ollama ps`.

## Real slide extraction

Status: **DONE** on `demos/fixtures/synthetic-slides-with-audio.mp4`
(5 patterned slides + 2 near-duplicates):

| Check | Result |
|---|---|
| candidates → slides | 14 → 5 |
| duplicates removed | PASS |
| timestamps | 0, 4000, 8000, 12000, 16000 ms |
| chronological | PASS |
| max-images=3 | PASS (3 slides) |
| DOCX images | PASS (5 media entries) |

Two real defects were found and fixed: real-JPEG hashing (`PerceptualHash`)
and non-adjacent duplicate handling (`SlideExtractionService`).
