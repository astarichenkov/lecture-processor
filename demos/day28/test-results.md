# Day 28 — test results

## Executed automated tests

| Test class | Tests | Result |
|-----------|-------|--------|
| EmbeddingIndexServiceTest | 2 | PASS |
| RetrievalServiceTest | 2 | PASS |
| RagServiceTest | 2 | PASS |
| VectorMathTest | 4 | PASS |
| EmbeddingCodecTest | 2 | PASS |

These verify chunking, embedding storage, cosine ordering, source attribution
and the no-evidence response using deterministic in-memory embeddings.

## LLM-with-RAG vs without-RAG evaluation

Status: **SKIPPED** — Ollama is not installed in this environment, so no real
answers could be generated. The evaluation protocol is fully specified in
`test-questions.json` (12 questions: direct, multi-chunk, terminology,
follow-up, unanswerable).

When Ollama is available, run each question twice:
1. Direct Qwen3 prompt (no context) — "without RAG".
2. `/api/lectures/{id}/chat` — "with RAG".

Record for each: answer, retrieved chunk ids, whether sources match the
expected segments, and whether unanswerable questions are correctly refused.

> A protocol or skipped run is never reported as a passed evaluation.
