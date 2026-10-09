# Demo scenario — Day 30

## 1. Requirements

- Homelab mini-PC (Intel i5-10500, 16 GB RAM, CPU only) или любой хост с Ollama.
- Ollama с `qwen3:1.7b` (fallback `qwen3:0.6b`), слушающая только loopback.

## 2. Startup

```bash
export CHAT_API_KEY=$(openssl rand -hex 16)
export CHAT_BIND_ADDRESS=0.0.0.0    # only inside a trusted LAN/VPN
./mvnw -pl private-ai-chat spring-boot:run
```

## 3. What to show

1. Locally: open `http://localhost:8081/`, send a message, show the answer.
2. From another machine on the trusted network:
   `curl -H "X-API-Key: $CHAT_API_KEY" http://<server-ip>:8081/api/health`.
3. Chat over the network:
   `curl -H "X-API-Key: $CHAT_API_KEY" -H "Content-Type: application/json" \
        -d '{"message":"Привет"}' http://<server-ip>:8081/api/chat`.
4. Show rate limiting: send 6 requests quickly → 6th returns 429.
5. Show oversized input: a 3000-char message → 400.
6. Show unauthenticated request → 401.
7. `ollama ps` on the server shows the local model.

## 4. Expected result

- Model answers, follow-ups preserve context.
- 429/400/401 behave as specified.
- No stack traces leak.

## 5. Local proof

- `curl http://localhost:11434/api/tags` works only on the server; the model is
  local. No cloud endpoints are configured.

## 6. Known limitations

- Real network verification requires access to the homelab; if unavailable it
  is reported as SKIPPED (see `test-results.md`).
- In-memory sessions; restart clears history.
