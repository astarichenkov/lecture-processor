# Day 28 — test results (real)

Machine: Ryzen 7 5800H / RTX 3050 Laptop 4 GB / 32 GB RAM, Windows, Java 25.
Ollama 0.40.2, chat `qwen3:1.7b` (thinking disabled), embeddings `nomic-embed-text`.

## Automated tests

```
./mvnw -pl backend test           # → 66 tests PASS (was 63)
./mvnw verify                     # full run below (Day 28 gate)
```

New tests:
- `NoEvidenceDetectorTest` (2) — refusal detection, no false positives on grounded answers.
- `RagServiceTest.clearsSourcesAndGroundedFlagWhenModelRefuses` — no-answer clears sources.
- `RagServiceTest` also asserts `SourceRef.score` propagation.

## Real RAG evaluation (15 questions)

- 12 answerable (5 direct, 2 multi-chunk, 3 terminology, 2 follow-up)
- 3 unanswerable

| Metric | Result |
|---|---|
| Recall@1 | 0.75 |
| Recall@3 | 0.917 |
| Recall@5 | 1.0 (trivial: only 5 chunks) |
| No-answer rejection accuracy | 1.0 (3/3) |
| Source correctness (topK=5) | 1.0 |
| Grounded answer pass rate | 1.0 (12/12) |
| RAG latency avg / median | 722 / 591 ms |
| Baseline latency avg (no context) | 2679 ms |
| Baseline answerable pass rate | 0.833 |
| Baseline no-answer refusal rate | 0.0 (it answered "weather in Moscow" from parametric memory) |

Full data: `rag-evaluation.json`, summary: `rag-metrics.json`, human report: `rag-evaluation.md`.

## Persistence / idempotency checks

| Check | Result |
|---|---|
| 14 transcript segments persisted | PASS |
| 5 chunks created, embedding_dim=768 | PASS |
| Vectors stored in SQLite BLOB | PASS |
| App restart keeps index usable | PASS (multiple restarts, same chunk ids) |
| Reprocess does not duplicate chunks | PASS (5 before → 5 after) |

## Acceptance

Day 28 acceptance criteria are met with a real local stack.
