#!/usr/bin/env python3
"""Local Ollama-API-compatible TEST DOUBLE (development only).

This is NOT an AI model and MUST NOT be used as evidence of a real AI
end-to-end run. It exists to verify the Lecture Processor <-> Ollama HTTP
contract (endpoints, request/response schema) and to exercise the pipeline
end-to-end when the real Ollama runtime cannot be installed (e.g. insufficient
disk space).

Endpoints implemented (schema-accurate):
  GET  /api/tags
  POST /api/show
  POST /api/chat      (stream=false and stream=true)
  POST /api/embed     (batch, Spring AI 2.0.1)

Run:
  python scripts/dev/ollama-stub.py --port 11434 --log target/ollama-stub.log
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import threading
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

DIMENSIONS = 768
MODEL = "qwen3:4b"
_requests = []
_lock = threading.Lock()
_log_path = None


def now_iso() -> str:
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.000000000Z")


def embed(text: str) -> list[float]:
    vec = [0.0] * DIMENSIONS
    for word in re.split(r"[^\w]+", text.lower()):
        if word:
            idx = int(hashlib.md5(word.encode("utf-8")).hexdigest(), 16) % DIMENSIONS
            vec[idx] += 1.0
    return vec


def make_notes(user_prompt: str) -> str:
    match = re.search(r"Фрагмент \d+ из \d+:\s*\n+(.*)", user_prompt, re.S)
    body = (match.group(1) if match else user_prompt).strip()
    first = body.split(". ")[0].strip()
    if first and not first.endswith("."):
        first += "."
    return f"## Раздел\n\n{first}\n\nКлючевые понятия и определения из этого фрагмента."


def make_reply(user_prompt: str) -> str:
    if "Резюме" in user_prompt or "резюме" in user_prompt[:200]:
        return "Краткое резюме: лекция посвящена основам векторной алгебры и её применениям."
    if "Фрагмент" in user_prompt:
        return make_notes(user_prompt)
    if "источник" in user_prompt.lower() or "Вопрос:" in user_prompt:
        return "Согласно фрагментам лекции, ответ содержится в приведённом контексте."
    return user_prompt.strip()[:400]


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def log_message(self, *args):  # silence default logging
        pass

    def _read_body(self) -> dict:
        if self.headers.get("Transfer-Encoding", "").lower() == "chunked":
            raw = self._read_chunked()
        else:
            length = int(self.headers.get("Content-Length", 0) or 0)
            raw = self.rfile.read(length) if length else b""
        try:
            return json.loads(raw.decode("utf-8")) if raw else {}
        except Exception:
            return {"_raw": raw.decode("utf-8", "replace")}

    def _read_chunked(self) -> bytes:
        data = bytearray()
        while True:
            size_line = self.rfile.readline().strip()
            if not size_line:
                break
            size = int(size_line.split(b";")[0], 16)
            if size == 0:
                self.rfile.readline()  # trailing CRLF
                break
            data += self.rfile.read(size)
            self.rfile.read(2)  # CRLF after chunk
        return bytes(data)

    def _send_json(self, payload: dict, status: int = 200):
        data = json.dumps(payload).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def _send_ndjson(self, chunks: list[dict]):
        self.send_response(200)
        self.send_header("Content-Type", "application/x-ndjson")
        self.end_headers()
        for chunk in chunks:
            self.wfile.write((json.dumps(chunk) + "\n").encode("utf-8"))
            self.wfile.flush()

    def _record(self, path: str, body: dict):
        with _lock:
            _requests.append({"path": path, "body": body})
        if _log_path:
            with open(_log_path, "a", encoding="utf-8") as fh:
                fh.write(json.dumps({"path": path, "body": body}, ensure_ascii=False) + "\n")
        print(f"[stub] {path} <- {json.dumps(body, ensure_ascii=True)[:200]}", flush=True)

    def do_GET(self):
        if self.path.startswith("/api/tags"):
            self._record("/api/tags", {})
            self._send_json({"models": [{
                "name": MODEL, "model": MODEL, "modified_at": now_iso(),
                "size": 2600000000, "digest": "stub",
                "details": {"family": "qwen3", "parameter_size": "4B",
                            "quantization_level": "Q4_K_M", "format": "gguf"},
            }]})
        else:
            self._send_json({"error": "not found"}, 404)

    def do_POST(self):
        body = self._read_body()
        path = self.path.split("?")[0]
        self._record(path, body)
        if path == "/api/chat":
            content = make_reply(_last_user(body))
            message = {"role": "assistant", "content": content}
            if body.get("stream"):
                self._send_ndjson([
                    {"model": MODEL, "created_at": now_iso(), "message": message, "done": False},
                    {"model": MODEL, "created_at": now_iso(), "message": {"role": "assistant", "content": ""},
                     "done": True, "done_reason": "stop", "total_duration": 1, "eval_count": 1,
                     "prompt_eval_count": 1},
                ])
            else:
                self._send_json({
                    "model": MODEL, "created_at": now_iso(), "message": message,
                    "done": True, "done_reason": "stop", "total_duration": 1,
                    "load_duration": 1, "prompt_eval_count": 1, "prompt_eval_duration": 1,
                    "eval_count": 1, "eval_duration": 1,
                })
        elif path == "/api/embed":
            inputs = body.get("input")
            texts = inputs if isinstance(inputs, list) else [inputs or ""]
            self._send_json({"model": MODEL, "embeddings": [embed(t) for t in texts],
                             "total_duration": 1, "load_duration": 1, "prompt_eval_count": 1})
        elif path == "/api/embeddings":
            self._send_json({"embedding": embed(body.get("prompt", ""))})
        elif path == "/api/show":
            self._send_json({"details": {"family": "qwen3", "parameter_size": "4B",
                                         "quantization_level": "Q4_K_M", "format": "gguf"},
                             "model_info": {"general.architecture": "qwen3"},
                             "capabilities": ["completion"], "modified_at": now_iso()})
        else:
            self._send_json({"error": "not found"}, 404)


def _last_user(body: dict) -> str:
    for msg in reversed(body.get("messages", [])):
        if msg.get("role") == "user":
            return msg.get("content", "")
    return body.get("prompt", "")


def main():
    global _log_path
    parser = argparse.ArgumentParser()
    parser.add_argument("--port", type=int, default=11434)
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--log", default=None)
    args = parser.parse_args()
    _log_path = args.log
    server = ThreadingHTTPServer((args.host, args.port), Handler)
    print(f"[stub] Ollama test double listening on http://{args.host}:{args.port}", flush=True)
    server.serve_forever()


if __name__ == "__main__":
    main()
