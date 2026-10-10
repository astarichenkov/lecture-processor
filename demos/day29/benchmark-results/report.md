# Day 29 — Real local-LLM optimization benchmark

Date: 2026-10-11. Machine: Ryzen 7 5800H / RTX 3050 Laptop 4 GB / 32 GB RAM, Windows, Java 25. 
Ollama 0.40.2; chat `qwen3:1.7b` (primary) and `qwen3:4b` (limited comparison); embeddings `nomic-embed-text`.

All numbers below are measured from real Ollama calls (token counters, `eval_duration`, `nvidia-smi`, `ollama ps`). 
`qwen3:8b` was intentionally **SKIPPED** (4 GB VRAM + overnight budget).

## Residency / hardware

| Observation | Value |
|---|---|
| `ollama ps` during run | qwen3:1.7b 1.7 GB **100% GPU** ctx=4096; nomic-embed-text 323 MB 100% GPU |
| GPU memory used | 2200 MiB / 4096 MiB |
| Model load time (first call) | 1824 ms |
| No reload between chunks | confirmed (single resident model) |

## Benchmark matrix (real results)

| Config | Task | Model | think | Prompt | numCtx | numPred | in tok | out tok | think tok | wall ms | tok/s | done | facts | truncated |

|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| A-qwen3-1.7b-baseline | clean | qwen3:1.7b | False | clean-transcript-v1 | 4096 | 1536 | 494 | 303 | 0 | 6812 | 112.09 | stop | 7/7 | False |
| A-qwen3-1.7b-baseline | notes | qwen3:1.7b | False | structured-notes-v1 | 4096 | 1536 | 503 | 387 | 0 | 5819 | 113.15 | stop | 7/7 | False |
| A-qwen3-1.7b-baseline | rag | qwen3:1.7b | False | rag-answer-v1 | 4096 | 1536 | 638 | 120 | 0 | 3450 | 114.03 | stop | 2/7 | False |
| B-qwen3-1.7b-optimized | clean | qwen3:1.7b | False | clean-transcript-v2 | 4096 | 512 | 472 | 304 | 0 | 5070 | 112.1 | stop | 7/7 | False |
| B-qwen3-1.7b-optimized | notes | qwen3:1.7b | False | structured-notes-v2 | 4096 | 512 | 500 | 391 | 0 | 5838 | 113.17 | stop | 7/7 | False |
| B-qwen3-1.7b-optimized | rag | qwen3:1.7b | False | rag-answer-v1 | 4096 | 512 | 638 | 120 | 0 | 3323 | 106.28 | stop | 2/7 | False |
| C-qwen3-1.7b-thinking | clean | qwen3:1.7b | True | clean-transcript-v1 | 4096 | 2048 | 488 | 840 | 515 | 9906 | 108.77 | stop | 7/7 | False |
| C-qwen3-1.7b-thinking | notes | qwen3:1.7b | True | structured-notes-v1 | 4096 | 2048 | 497 | 784 | 410 | 9407 | 109.0 | stop | 7/7 | False |
| C-qwen3-1.7b-thinking | rag | qwen3:1.7b | True | rag-answer-v1 | 4096 | 2048 | 632 | 350 | 272 | 5514 | 105.54 | stop | 2/7 | False |
| D-qwen3-4b-verified | clean | qwen3:4b | True | clean-transcript-v1 | 4096 | 4096 | 488 | 4096 | 2983 | 224608 | 18.91 | length | 0/7 | True |
| D-qwen3-4b-verified | notes | qwen3:4b | True | structured-notes-v1 | 4096 | 4096 | 497 | 3556 | 2402 | 187460 | 19.24 | stop | 7/7 | False |
| D-qwen3-4b-verified | rag | qwen3:4b | True | rag-answer-v1 | 4096 | 4096 | 632 | 1141 | 878 | 47885 | 25.34 | stop | 1/7 | False |

> `out tok` is Ollama `eval_count` (includes thinking tokens). `tok/s` is `eval_count / eval_duration`.

## Quality rubric (agent-as-judge, 0–5)

Criteria order: factual preservation, completeness, grammar, terminology, structure, no hallucination, no truncation.

| Config | Task | F | C | G | T | S | H | X | Avg | Notes |
|---|---|---|---|---|---|---|---|---|---|---|
| A-qwen3-1.7b-baseline | clean | 5 | 5 | 5 | 5 | 4 | 5 | 5 | **4.86** | Clean, corrects ASR errors; all facts kept. |
| A-qwen3-1.7b-baseline | notes | 5 | 5 | 5 | 4 | 5 | 5 | 5 | **4.86** | Well-sectioned; 'калинерными' typo. |
| A-qwen3-1.7b-baseline | rag | 5 | 4 | 5 | 5 | 5 | 5 | 5 | **4.86** | Grounded, concise. |
| B-qwen3-1.7b-optimized | clean | 4 | 5 | 4 | 4 | 4 | 5 | 5 | **4.43** | Kept ASR errors ('длины','вектор и перпендикулярны'). |
| B-qwen3-1.7b-optimized | notes | 4 | 5 | 5 | 4 | 5 | 4 | 5 | **4.57** | Similar; adds a small unsupported sentence. |
| B-qwen3-1.7b-optimized | rag | 5 | 4 | 5 | 5 | 5 | 5 | 5 | **4.86** | Identical to A. |
| C-qwen3-1.7b-thinking | clean | 3 | 5 | 5 | 3 | 4 | 4 | 5 | **4.14** | Factual error: коллинеарные -> перпендикулярные. |
| C-qwen3-1.7b-thinking | notes | 5 | 5 | 5 | 5 | 5 | 5 | 5 | **5.0** | Correct formulas; cleanest structure (but slower). |
| C-qwen3-1.7b-thinking | rag | 4 | 4 | 5 | 4 | 4 | 5 | 5 | **4.43** | Imprecise definition of cosine closeness. |
| D-qwen3-4b-verified | clean | 1 | 1 | 1 | 1 | 1 | 1 | 0 | **0.86** | Truncated (done=length) with empty content. |
| D-qwen3-4b-verified | notes | 5 | 5 | 5 | 4 | 4 | 5 | 5 | **4.71** | Good, 187 s. |
| D-qwen3-4b-verified | rag | 5 | 3 | 5 | 5 | 5 | 5 | 5 | **4.71** | Correct but only one sentence; 48 s. |

## Findings

1. **Model choice dominates latency.** qwen3:1.7b notes ~5–6 s vs qwen3:4b notes ~187 s (~30x). qwen3:4b `clean` also **truncated** at 4096 tokens with empty content, so it is not usable as the default on this hardware.
2. **Thinking disabled is the right mode.** A (think=false) is ~2x faster than C (think=true) with equal or better quality; C even introduced a factual error in `clean`.
3. **Prompt v2 did not reduce output tokens** on this single-chunk corpus (303→304 clean, 387→391 notes) and slightly reduced `clean` quality. The `v2-conservative-clean` / `v3-optimized-notes` candidates are kept for longer, more repetitive inputs but are **not** recommended as defaults based on this measurement.
4. **Chunking is the real lever.** Fewer, larger chunks cut both calls and tokens:


| maxChars | LLM calls | output tokens | sequential ms | concurrency=2 ms |
|---|---|---|---|---|
| 300 | 5 | 841 | 19958 | 9187 |
| 1000 | 1 | 313 | 5038 | 5078 |
| 3000 | 1 | 318 | 5069 | 4972 |
| 6000 | 1 | 313 | 4987 | 5044 |

   Concurrency=2 helps only when many chunks exist (5 calls: 20.0 s → 9.2 s), but is still slower than a single large chunk (5.0 s). Default stays **sequential**.
5. **Decoupled chunk sizes implemented.** RAG indexing now uses `rag-chunk-chars` (default 400/60) while text generation keeps `llm-chunk-chars` (6000/400), so retrieval keeps granular chunks without multiplying generation calls.

## Recommended configuration

| Setting | Value |
|---|---|
| Model | `qwen3:1.7b` |
| Thinking | disabled (`OLLAMA_DISABLE_THINKING=true`) |
| numCtx | 4096 |
| numPredict | 1536 |
| temperature | 0.1 |
| Generation chunk | `llm-chunk-chars=6000` / overlap 400 |
| RAG chunk | `rag-chunk-chars=400` / overlap 60 |
| Concurrency | sequential |
| Prompts | `clean-transcript-v1`, `structured-notes-v1`, `rag-answer-v1` (v2 variants available but not default) |

**Before/after (notes task):** qwen3:4b thinking ≈ 187 000 ms → qwen3:1.7b thinking-disabled ≈ 5 800 ms (**~32x faster**) with equal key-fact preservation.
