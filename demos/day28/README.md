# Day 28 — Local LLM + RAG

Локальный retrieval + генерация поверх транскрипта лекции.

## Что реализовано

- `LectureChunk` — доменная модель чанка с временными метками.
- Чанкование с перекрытием (`TextChunker.chunkSegments`).
- Локальные эмбеддинги через Ollama (`LlmProvider.embed`, модель
  `nomic-embed-text`), нормализованные L2.
- Персистентный индекс в SQLite (`lecture_chunk.embedding` BLOB),
  точный косинусный поиск в Java (`RetrievalService`).
- `RagService`: сборка контекста, ответ Qwen3, атрибуция источников.
- Источники формируются из результатов поиска (lectureId, filename, chunkId,
  start/end, excerpt) — LLM не может их выдумать.
- Ответ «недостаточно информации», если ничего не найдено.
- Чат по лекции с историей (`/lectures/{id}/chat`, API `/api/lectures/{id}/chat`).
- 12 контрольных вопросов и сравнение LLM без RAG / с RAG.

## Архитектура

```
Transcript → chunking → Ollama embeddings → SQLite index
                                              ↓
User question → embedding → cosine top-K → context assembly → Qwen3 → grounded answer + sources
```

Индекс заменяем: `RetrievalService` инкапсулирует поиск, так что его можно
заменить на выделенное векторное хранилище.

## Ограничения

- Эмбеддинги требуют доступной Ollama и embedding-модели.
- Поиск точный (полный перебор) — подходит для небольших локальных коллекций.
