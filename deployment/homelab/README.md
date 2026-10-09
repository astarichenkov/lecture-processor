# Homelab deployment — private AI chat (Day 30)

Target: CPU-only mini-PC/server (e.g. Intel i5-10500, 16 GB RAM).
Services: Ollama (loopback) + `private-ai-chat` Spring Boot service.

> No successful deployment on the homelab is claimed from the development
> environment. Run these steps on the server after verifying access.

## 1. Detect the OS and prerequisites

```bash
./install.sh detect
```

## 2. Install Ollama and the model

```bash
./install.sh ollama           # installs Ollama if missing
ollama pull qwen3:1.7b        # or qwen3:0.6b as fallback
```

Keep Ollama on loopback (default `127.0.0.1:11434`). Do **not** expose 11434
publicly and do not add router port-forwarding.

## 3. Build the service

On a machine with JDK 25:

```bash
./mvnw -pl private-ai-chat package -DskipTests
```

Copy `private-ai-chat/target/private-ai-chat-1.0.0-SNAPSHOT.jar` to the server
(or build there).

## 4. Run

```bash
export CHAT_API_KEY="$(openssl rand -hex 16)"
export CHAT_BIND_ADDRESS=127.0.0.1     # or a trusted LAN/VPN interface
export OLLAMA_BASE_URL=http://localhost:11434
export CHAT_MODEL=qwen3:1.7b
java -jar private-ai-chat-1.0.0-SNAPSHOT.jar
```

## 5. Optional: Docker Compose

```bash
docker compose -f deployment/homelab/compose.yaml up -d
```

Ollama stays on the internal network; only the chat service port is published,
and it should be bound to a trusted interface.

## 6. Verify

```bash
./verify.sh http://127.0.0.1:8081 "$CHAT_API_KEY"
```

Checks: health, authenticated chat, oversized request (400), rate limit (429),
and unauthenticated access (401).

## Security checklist

- [ ] Ollama listens only on loopback.
- [ ] `11434` not exposed to the internet, no public port forwarding.
- [ ] `CHAT_API_KEY` set and stored outside the repository.
- [ ] Service bound to a trusted interface or reachable only via VPN.
