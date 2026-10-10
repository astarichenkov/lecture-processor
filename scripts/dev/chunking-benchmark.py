#!/usr/bin/env python3
"""Day 29 chunking + concurrency measurement (real Ollama calls).

Compares the number of LLM calls and total generation time for structured-notes
generation at different chunk sizes, and optionally sequential vs 2-way
concurrency. Mirrors the Java TextChunker.chunkSegments grouping (whole
segments, char budget, whole-segment overlap).

Usage: python scripts/dev/chunking-benchmark.py --db <db> --model qwen3:1.7b
"""
import argparse
import json
import sqlite3
import time
import urllib.request
from concurrent.futures import ThreadPoolExecutor

OLLAMA = "http://localhost:11434"


def chunk_segments(segments, max_chars, overlap_chars):
    chunks, current, current_len = [], [], 0
    for text in segments:
        seg_len = len(text) + 1
        if current and current_len + seg_len > max_chars:
            chunks.append(" ".join(current))
            carried, acc = [], 0
            for s in reversed(current):
                if acc + len(s) > overlap_chars and carried:
                    break
                carried.insert(0, s)
                acc += len(s) + 1
            current, current_len = list(carried), sum(len(s) + 1 for s in carried)
        current.append(text)
        current_len += seg_len
        if len(current) == 1 and current_len > max_chars:
            chunks.append(current[0])
            current, current_len = [], 0
    if current:
        chunks.append(" ".join(current))
    return chunks


def chat(prompt, user, model, num_predict, num_ctx, think):
    payload = {"model": model, "stream": False, "think": think,
               "messages": [{"role": "system", "content": prompt}, {"role": "user", "content": user}],
               "options": {"num_ctx": num_ctx, "num_predict": num_predict, "temperature": 0.1}}
    req = urllib.request.Request(f"{OLLAMA}/api/chat",
                                 data=json.dumps(payload, ensure_ascii=False).encode("utf-8"),
                                 headers={"Content-Type": "application/json"}, method="POST")
    with urllib.request.urlopen(req, timeout=600) as r:
        return json.loads(r.read().decode("utf-8"))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--db", required=True)
    ap.add_argument("--model", default="qwen3:1.7b")
    ap.add_argument("--out", default="demos/day29/benchmark-results/chunking-results.json")
    args = ap.parse_args()

    con = sqlite3.connect(args.db)
    lid = con.execute("select id from lecture where status='COMPLETED' order by updated_at desc limit 1").fetchone()[0]
    segments = [r[0] for r in con.execute(
        "select text from transcript_segment where lecture_id=? order by sequence", (lid,))]
    prompt = open("backend/src/main/resources/prompts/structured-notes-v1.txt", encoding="utf-8").read().strip()

    results = []
    for max_chars in (300, 1000, 3000, 6000):
        chunks = chunk_segments(segments, max_chars, 60)
        t0 = time.time()
        tokens = 0
        for i, c in enumerate(chunks):
            resp = chat(prompt, f"Фрагмент {i+1} из {len(chunks)}:\n\n{c}", args.model, 1536, 4096, False)
            tokens += resp.get("eval_count") or 0
        seq_ms = int((time.time() - t0) * 1000)
        # concurrency=2 for the same chunk set
        t0 = time.time()
        with ThreadPoolExecutor(max_workers=2) as ex:
            list(ex.map(lambda ci: chat(prompt, f"Фрагмент {ci[0]+1} из {len(chunks)}:\n\n{ci[1]}",
                                        args.model, 1536, 4096, False), list(enumerate(chunks))))
        par_ms = int((time.time() - t0) * 1000)
        results.append({"maxChars": max_chars, "calls": len(chunks), "outputTokens": tokens,
                        "sequentialMs": seq_ms, "concurrency2Ms": par_ms,
                        "chunkChars": [len(c) for c in chunks]})
        print(f"maxChars={max_chars}: calls={len(chunks)} tokens={tokens} seq={seq_ms}ms par2={par_ms}ms", flush=True)

    json.dump({"lectureId": lid, "model": args.model, "overlapChars": 60, "results": results},
              open(args.out, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
    print("wrote", args.out)


if __name__ == "__main__":
    main()
