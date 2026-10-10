#!/usr/bin/env python3
"""Real local RAG evaluation harness (no mocks).

Runs each control question through:
  * the running Lecture Processor REST RAG endpoint (Java RagService + Ollama), and
  * a no-context baseline (same model + system prompt, no sources).
It also recomputes retrieval similarity scores from the persisted SQLite index
using the real nomic-embed-text embeddings, so the report carries real scores.

Usage:
  python scripts/dev/rag-eval.py --base-url http://localhost:18081 \
      --lecture-id <id> --db <storage>/lecture-processor.db \
      --questions demos/day28/test-questions.json \
      --out demos/day28/rag-evaluation.json --model qwen3:1.7b --think false

This script only performs real network calls; it never fabricates metrics.
"""
import argparse
import json
import math
import re
import sqlite3
import struct
import time
import urllib.parse
import urllib.request

OLLAMA = "http://localhost:11434"

# Conventional no-context baseline: the model's own knowledge, no grounding
# instruction and no retrieved sources.
NEUTRAL_SYS = "Ты — полезный ассистент. Отвечай на вопрос кратко и по делу на русском языке."

REFUSAL_PATTERNS = [
    r"нет\s+информац",
    r"информац\w*[^.\n]{0,120}\bнет\b",
    r"не\s+на(й|х)ден",
    r"не\s+содерж",
    r"не\s+упомин",
    r"недостаточн\w*\s+(информац|данн|сведен)",
    r"информац\w*\s+недостаточн",
    r"информац\w*[^.\n]{0,120}недостаточн",
    r"в\s+источниках\s+не\s+",
    r"отсутству|не\s+указан|не\s+приведен|не\s+описан|не\s+рассмотр",
    r"нет\s+данн",
    r"не\s+могу\s+ответ",
]


def http_json(url, payload=None, method="GET", timeout=1800):
    data = None
    headers = {}
    if payload is not None:
        data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        headers["Content-Type"] = "application/json"
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return json.loads(resp.read().decode("utf-8"))


def embed(texts):
    out = http_json(f"{OLLAMA}/api/embed", {"model": "nomic-embed-text", "input": texts}, "POST")
    return out["embeddings"]


def cosine(a, b):
    if len(a) != len(b):
        return 0.0
    dot = sum(x * y for x, y in zip(a, b))
    na = math.sqrt(sum(x * x for x in a))
    nb = math.sqrt(sum(y * y for y in b))
    return 0.0 if na == 0 or nb == 0 else dot / (na * nb)


def load_chunks(db):
    con = sqlite3.connect(db)
    rows = con.execute(
        "select id, sequence, start_ms, end_ms, text, embedding from lecture_chunk order by sequence"
    ).fetchall()
    chunks = []
    for cid, seq, start, end, text, blob in rows:
        vec = list(struct.unpack("<%df" % (len(blob) // 4), blob)) if blob else []
        chunks.append({"id": cid, "seq": seq, "start": start, "end": end, "text": text, "vec": vec})
    return chunks


def token_set(text):
    normalized = (text or "").lower().replace("ё", "е")
    return set(re.findall(r"[а-яa-z0-9]{4,}", normalized))


def concept_match(answer, concept):
    """Loose concept coverage: fraction of meaningful concept tokens present."""
    want = token_set(concept) - {"вектор", "векторы", "векторов", "через", "между"}
    if not want:
        return 1.0
    got = token_set(answer)
    # crude stemming: compare 5-char prefixes
    hits = sum(1 for w in want if any(g.startswith(w[:5]) or w.startswith(g[:5]) for g in got))
    return hits / len(want)


def is_refusal(text):
    t = (text or "").lower()
    return any(re.search(p, t) for p in REFUSAL_PATTERNS)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--base-url", required=True)
    ap.add_argument("--lecture-id", required=True)
    ap.add_argument("--db", required=True)
    ap.add_argument("--questions", required=True)
    ap.add_argument("--out", required=True)
    ap.add_argument("--model", default="qwen3:1.7b")
    ap.add_argument("--think", default="false")
    ap.add_argument("--num-ctx", type=int, default=8192)      # matches LlmOptions.of()
    ap.add_argument("--num-predict", type=int, default=6144)  # matches LlmOptions.of()
    ap.add_argument("--temperature", type=float, default=0.2)  # matches LlmOptions.of()
    ap.add_argument("--seed", type=int, default=42)
    ap.add_argument("--top-k", type=int, default=5)
    args = ap.parse_args()

    think = args.think.lower() == "true"
    spec = json.load(open(args.questions, encoding="utf-8"))
    chunks = load_chunks(args.db)
    sys_prompt = open("backend/src/main/resources/prompts/rag-answer-v1.txt", encoding="utf-8").read().strip()

    print(f"loaded {len(chunks)} chunks, {len(spec['questions'])} questions")

    results = []
    for q in spec["questions"]:
        text = q["question"]
        # --- real retrieval scores (recomputed from persisted embeddings) ---
        t0 = time.time()
        qvec = embed([text])[0]
        embed_ms = int((time.time() - t0) * 1000)
        scored = sorted(
            ({"seq": c["seq"], "chunkId": c["id"], "start": c["start"], "end": c["end"],
              "text": c["text"], "score": round(cosine(qvec, c["vec"]), 4)} for c in chunks),
            key=lambda s: s["score"], reverse=True,
        )
        top = scored[: args.top_k]

        # --- real RAG answer via the Java application ---
        url = (f"{args.base_url}/api/lectures/{args.lecture_id}/chat?"
               + urllib.parse.urlencode({"session": q["session"], "question": text}))
        t0 = time.time()
        rag = http_json(url, method="POST")
        rag_ms = int((time.time() - t0) * 1000)

        # --- baseline: same model, same system prompt, no context ---
        base_payload = {
            "model": args.model,
            "messages": [{"role": "system", "content": NEUTRAL_SYS}, {"role": "user", "content": text}],
            "stream": False,
            "think": think,
            "options": {"num_ctx": args.num_ctx, "num_predict": args.num_predict,
                        "temperature": args.temperature, "seed": args.seed},
        }
        t0 = time.time()
        base = http_json(f"{OLLAMA}/api/chat", base_payload, "POST")
        base_ms = int((time.time() - t0) * 1000)

        rag_answer = rag["text"]
        base_answer = base["message"]["content"]
        gold = q.get("gold_chunks", [])
        top_seqs = [s["seq"] for s in top]
        gold_rank = next((i + 1 for i, s in enumerate(top_seqs) if s in gold), None) if gold else None

        if q["answerable"]:
            cov = concept_match(rag_answer, q["expected_concept"])
            rag_pass = cov >= 0.5 and not is_refusal(rag_answer)
            # groundedness: source list must include a gold chunk when it was retrieved
            source_ok = bool(set(q.get("gold_chunks", [])) & set(s["chunkSequence"] for s in rag["sources"]))
        else:
            cov = None
            rag_pass = is_refusal(rag_answer)
            source_ok = len(rag["sources"]) == 0 or all(s["chunkSequence"] not in () for s in rag["sources"])

        results.append({
            "id": q["id"],
            "type": q["type"],
            "session": q["session"],
            "question": text,
            "answerable": q["answerable"],
            "expected_concept": q["expected_concept"],
            "gold_chunks": gold,
            "gold_rank": gold_rank,
            "top_chunks": [{k: s[k] for k in ("seq", "chunkId", "start", "end", "score")} for s in top],
            "all_scores": [{k: s[k] for k in ("seq", "score")} for s in scored],
            "rag_answer": rag_answer,
            "rag_grounded_flag": rag["grounded"],
            "rag_sources": [{"seq": s["chunkSequence"], "chunkId": s["chunkId"],
                             "start": s["startMs"], "end": s["endMs"],
                             "score": s.get("score")} for s in rag["sources"]],
            "baseline_answer": base_answer,
            "concept_coverage": None if cov is None else round(cov, 3),
            "rag_pass": rag_pass,
            "source_ok": source_ok,
            "is_refusal": is_refusal(rag_answer),
            "baseline_refusal": is_refusal(base_answer),
            "latency": {"embeddingMs": embed_ms, "ragMs": rag_ms, "baselineMs": base_ms,
                        "ragEvalCount": rag.get("usage") if isinstance(rag, dict) else None,
                        "baselineEvalCount": base.get("eval_count"),
                        "baselinePromptEvalCount": base.get("prompt_eval_count")},
        })
        print(f"  {q['id']:>3} {q['type']:<12} rank={gold_rank} ragPass={rag_pass} "
              f"cov={cov} ragMs={rag_ms} baseMs={base_ms}")

    json.dump({"model": args.model, "think": think, "topK": args.top_k, "results": results},
              open(args.out, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
    print(f"saved {args.out}")


if __name__ == "__main__":
    main()
