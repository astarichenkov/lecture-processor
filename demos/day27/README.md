# Day 27 — Lecture Processor MVP

Интеграция локальной LLM в реальное Java-приложение: загрузка MP4 → FFmpeg →
whisper.cpp → JSON-транскрипт → Ollama/Qwen3 → структурированный Markdown → DOCX.

## Что реализовано

- Загрузка видео с валидацией (расширение, размер, пустой файл).
- Состояния задания: `UPLOADED → EXTRACTING_AUDIO → TRANSCRIBING → PROCESSING_TEXT → EXPORTING → COMPLETED/FAILED`.
- Хранение статусов в SQLite.
- Ограниченный пул обработки (`lectureJobExecutor`) + очередь с `AbortPolicy`.
- Безопасный запуск процессов (`ProcessBuilder`, без shell, таймауты, kill process tree).
- FFmpeg: извлечение аудио 16 kHz mono WAV, определение длительности.
- whisper.cpp: `whisper-cli -oj`, парсинг JSON с временными метками.
- `LlmProvider` + реальная реализация `OllamaLlmProvider` (Spring AI 2.0.1).
- Чанкование длинных лекций, два режима: очистка транскрипта и конспект.
- Генерация Markdown и DOCX (Apache POI).
- REST API и минимальный Thymeleaf/HTMX UI.
- Тесты: парсер транскрипта, чанкер, LLM-обработка, DOCX, процессы, валидация, HTTP.

## Запуск

```bash
./mvnw -pl backend spring-boot:run -Dspring-boot.run.profiles=local
```

Открыть http://localhost:8080, загрузить видео и дождаться статуса `COMPLETED`.
Результаты — `data/lectures/<id>/` (transcript.json, transcript.md, notes.md, notes.docx).

## Требования

- FFmpeg, whisper.cpp + модель (`WHISPER_MODEL_PATH`), Ollama + `qwen3:4b`.
- Без них приложение собирается и тесты проходят, но реальная обработка недоступна.
