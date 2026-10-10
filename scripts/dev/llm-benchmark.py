#!/usr/bin/env python3
"""Day 29 real local-LLM benchmark harness (no mocks).

Runs representative tasks (clean transcript, structured notes, RAG answer)
against a running Ollama, for the practical model/prompt matrix:
  A  qwen3:1.7b baseline      (think=false, v1 prompts)
  B  qwen3:1.7b optimized      (think=false, v2 prompts, smaller budget)
  C  qwen3:1.7b thinking       (think=true, v1 prompts)
  D  qwen3:4b quality baseline (think=true, v1 prompts, limited)
  E  qwen3:8b                   (SKIPPED)

Captures real Ollama timings/token counters, GPU memory via nvidia-smi and the
residency/GPU-CPU split via `ollama ps`. JSON/CSV/Markdown are written to
demos/day29/benchmark-results/. Nothing is fabricated: if a value cannot be
measured it is recorded as null/"N/A".

Usage:
  python scripts/dev/llm-benchmark.py --db <storage>/lecture-processor.db \
      --out-dir demos/day29/benchmark-results
"""
import argparse
import csv
import json
import os
import re
import subprocess
import time
import urllib.request

OLLAMA = "http://localhost:11434"
PROMPTS = "backend/src/main/resources/prompts"
FF = r"C:\tools\ffmpeg\ffmpeg-9.0.2-essentials_build\bin\ffmpeg.exe"

MATRIX = [
    {"id": "A-qwen3-1.7b-baseline", "model": "qwen3:1.7b", "think": False,
     "numCtx": 4096, "numPredict": 1536, "temperature": 0.1,
     "cleanPrompt": "clean-transcript-v1", "notesPrompt": "structured-notes-v1",
     "label": "A baseline"},
    {"id": "B-qwen3-1.7b-optimized", "model": "qwen3:1.7b", "think": False,
     "numCtx": 4096, "numPredict": 512, "temperature": 0.1,
     "cleanPrompt": "clean-transcript-v2", "notesPrompt": "structured-notes-v2",
     "label": "B optimized"},
    {"id": "C-qwen3-1.7b-thinking", "model": "qwen3:1.7b", "think": True,
     "numCtx": 4096, "numPredict": 2048, "temperature": 0.1,
     "cleanPrompt": "clean-transcript-v1", "notesPrompt": "structured-notes-v1",
     "label": "C thinking"},
    {"id": "D-qwen3-4b-verified", "model": "qwen3:4b", "think": True,
     "numCtx": 4096, "numPredict": 4096, "temperature": 0.2,
     "cleanPrompt": "clean-transcript-v1", "notesPrompt": "structured-notes-v1",
     "label": "D qwen3:4b"},
]

KEY_FACTS = ["направленн", "скалярн", "перпендикуляр", "векторн", "смешанн", "машинн", "нормализ"]


def read_prompt(name):
    with open(os.path.join(PROMPTS, name + ".txt"), encoding="utf-8") as f:
        return f.read().strip()


def ollama_chat(model, system, user, think, num_ctx, num_predict, temperature, timeout):
    payload = {
        "model": model,
        "messages": [{"role": "system", "content": system}, {"role": "user", "content": user}],
        "stream": False,
        "think": think,
        "options": {"num_ctx": num_ctx, "num_predict": num_predict, "temperature": temperature},
    }
    req = urllib.request.Request(f"{OLLAMA}/api/chat",
                                 data=json.dumps(payload, ensure_ascii=False).encode("utf-8"),
                                 headers={"Content-Type": "application/json"}, method="POST")
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return json.loads(resp.read().decode("utf-8"))


def gpu_used_mib():
    try:
        out = subprocess.run(["nvidia-smi", "--query-gpu=memory.used", "--format=csv,noheader,nounits"],
                             capture_output=True, text=True, timeout=15)
        return int(out.stdout.strip().splitlines()[0])
    except Exception:
        return None


def ollama_ps():
    try:
        out = subprocess.run([r"C:\tools\ollama\ollama.exe", "ps"], capture_output=True, text=True, timeout=15)
        return out.stdout.strip().replace("\n", " | ")
    except Exception:
        return "N/A"


def cyrillic_ratio(text):
    if not text:
        return 0.0
    letters = [c for c in text if c.isalpha()]
    cyr = [c for c in letters if "а" <= c.lower() <= "я" or c.lower() == "ё"]
    return round(len(cyr) / len(letters), 3) if letters else 0.0


def fact_hits(text):
    low = (text or "").lower().replace("ё", "е")
    return sum(1 for f in KEY_FACTS if f in low)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--db", required=True)
    ap.add_argument("--out-dir", default="demos/day29/benchmark-results")
    ap.add_argument("--timeout", type=int, default=900)
    args = ap.parse_args()
    os.makedirs(args.out_dir, exist_ok=True)

    # real source material from the verified Day 28 lecture DB
    import sqlite3
    con = sqlite3.connect(args.db)
    lecture_id = con.execute("select id from lecture where status='COMPLETED' order by updated_at desc limit 1").fetchone()[0]
    segments = [r[0] for r in con.execute(
        "select text from transcript_segment where lecture_id=? order by sequence", (lecture_id,))]
    transcript = " ".join(segments)
    chunks = con.execute("select sequence,start_ms,end_ms,text from lecture_chunk where lecture_id=? order by sequence",
                         (lecture_id,)).fetchall()
    context = "\n\n".join(f"[{i+1}] ({c[1]}–{c[2]}) {c[3]}" for i, c in enumerate(chunks))
    rag_question = "Как векторы используются в машинном обучении и что такое косинусная близость?"
    rag_prompt = read_prompt("rag-answer-v1")
    rag_system = rag_prompt
    rag_user = f"Источники из лекции:\n\n{context}\n\nВопрос: {rag_question}"

    tasks = {
        "clean": {"system": None, "user": transcript, "promptKind": "cleanPrompt"},
        "notes": {"system": None, "user": "Фрагмент 1 из 1:\n\n" + transcript, "promptKind": "notesPrompt"},
        "rag": {"system": rag_system, "user": rag_user, "promptKind": "rag"},
    }

    results = []
    for cfg in MATRIX:
        for task, spec in tasks.items():
            if spec["promptKind"] == "rag":
                system = spec["system"]
                prompt_version = "rag-answer-v1"
            else:
                prompt_version = cfg[spec["promptKind"]]
                system = read_prompt(cfg[spec["promptKind"]])
            print(f"running {cfg['id']} / {task} ...", flush=True)
            ram_before, gpu_before = None, gpu_used_mib()
            error = None
            response = None
            t0 = time.time()
            try:
                response = ollama_chat(cfg["model"], system, spec["user"], cfg["think"],
                                       cfg["numCtx"], cfg["numPredict"], cfg["temperature"], args.timeout)
            except Exception as e:  # includes read timeout
                error = f"{type(e).__name__}: {e}"
            wall_ms = int((time.time() - t0) * 1000)
            gpu_after = gpu_used_mib()

            if response is None:
                results.append({
                    "configuration": cfg["id"], "label": cfg["label"], "model": cfg["model"],
                    "think": cfg["think"], "promptVersion": prompt_version, "task": task,
                    "numCtx": cfg["numCtx"], "numPredict": cfg["numPredict"], "temperature": cfg["temperature"],
                    "status": "TIMEOUT" if "timeout" in (error or "").lower() or "timed out" in (error or "").lower() else "FAIL",
                    "error": error, "wallMs": wall_ms, "inputTokens": None, "outputTokens": None,
                    "thinkingTokensEst": None, "evalTokensPerSec": None, "totalOllamaMs": None,
                    "loadMs": None, "gpuUsedMiB": gpu_after, "ollamaPs": ollama_ps(),
                    "content": None, "doneReason": None,
                })
                print("  ->", results[-1]["status"], error, flush=True)
                continue

            content = response.get("message", {}).get("content") or ""
            thinking = response.get("message", {}).get("thinking") or ""
            eval_count = response.get("eval_count")
            eval_dur = response.get("eval_duration") or 0
            results.append({
                "configuration": cfg["id"], "label": cfg["label"], "model": cfg["model"],
                "think": cfg["think"], "promptVersion": prompt_version, "task": task,
                "numCtx": cfg["numCtx"], "numPredict": cfg["numPredict"], "temperature": cfg["temperature"],
                "status": "PASS",
                "wallMs": wall_ms,
                "inputTokens": response.get("prompt_eval_count"),
                "outputTokens": eval_count,
                "contentTokensEst": max(1, len(content) // 4),
                "thinkingChars": len(thinking),
                "thinkingTokensEst": max(0, len(thinking) // 4),
                "evalTokensPerSec": round(eval_count / (eval_dur / 1e9), 2) if eval_dur else None,
                "totalOllamaMs": round((response.get("total_duration") or 0) / 1e6),
                "loadMs": round((response.get("load_duration") or 0) / 1e6),
                "gpuUsedMiB": gpu_after,
                "ollamaPs": ollama_ps(),
                "doneReason": response.get("done_reason"),
                "contentChars": len(content),
                "cyrillicRatio": cyrillic_ratio(content),
                "headings": len(re.findall(r"^##", content, re.M)),
                "keyFactHits": fact_hits(content),
                "keyFactTotal": len(KEY_FACTS),
                "truncated": response.get("done_reason") == "length",
                "reasoningLeak": ("think" in content.lower() and "…" in content) or "思考" in content,
                "content": content,
            })
            r = results[-1]
            print(f"  -> PASS wall={wall_ms}ms outTok={r['outputTokens']} tok/s={r['evalTokensPerSec']} "
                  f"done={r['doneReason']} facts={r['keyFactHits']}/{r['keyFactTotal']}", flush=True)

    json.dump({"lectureId": lecture_id, "matrix": [c["id"] for c in MATRIX],
               "skipped": [{"id": "E-qwen3-8b", "reason": "RTX 3050 4GB + overnight time budget"}],
               "sourceChars": len(transcript), "results": results},
              open(os.path.join(args.out_dir, "results.json"), "w", encoding="utf-8"),
              ensure_ascii=False, indent=2)

    cols = ["configuration", "task", "model", "think", "promptVersion", "numCtx", "numPredict",
            "temperature", "status", "wallMs", "inputTokens", "outputTokens", "thinkingTokensEst",
            "evalTokensPerSec", "totalOllamaMs", "loadMs", "gpuUsedMiB", "doneReason", "truncated",
            "keyFactHits", "keyFactTotal", "contentChars", "cyrillicRatio", "headings"]
    with open(os.path.join(args.out_dir, "results.csv"), "w", encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, fieldnames=cols, extrasaction="ignore")
        w.writeheader()
        w.writerows(results)
    print("wrote results.json and results.csv", flush=True)


if __name__ == "__main__":
    main()
