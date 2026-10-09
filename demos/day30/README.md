# Day 30 — Private AI chat service

Отдельный минимальный сервис приватного локального чата. Не зависит от
конвейера обработки видео (без Whisper/FFmpeg/DOCX/RAG).

```
Browser / Remote Client → Spring Boot API → Auth + Rate limit → ChatService → Ollama → Qwen3 1.7B (CPU)
```

## Что реализовано

- Отдельный Maven-модуль `private-ai-chat` (Java 25, Spring Boot 4.1.1).
- Веб-чат на Thymeleaf/HTMX (`/`) и HTTP API (`/api/health`, `/api/chat`).
- История диалога на сессию с ограничением (`ChatMemoryStore`).
- Простая аутентификация по API-ключу из `CHAT_API_KEY` (`X-API-Key` или
  `Authorization: Bearer`).
- Rate limit 5 запросов/мин на клиента → HTTP 429.
- Лимит сообщения 2000 символов → HTTP 400.
- Контроль контекста и max output tokens (независимо от лимита символов).
- Один активный воркер генерации + ограниченная очередь → 429 при перегрузке.
- Таймаут генерации; понятные ошибки без stack trace.
- Структурированное логирование.

## Конфигурация (env)

| Переменная | По умолчанию | Назначение |
|-----------|--------------|-----------|
| `CHAT_PORT` | 8081 | порт |
| `CHAT_BIND_ADDRESS` | 127.0.0.1 | интерфейс (только доверенная сеть) |
| `OLLAMA_BASE_URL` | http://localhost:11434 | локальная Ollama |
| `CHAT_MODEL` | qwen3:1.7b | модель |
| `CHAT_API_KEY` | (пусто) | API-ключ; пусто = без аутентификации |
