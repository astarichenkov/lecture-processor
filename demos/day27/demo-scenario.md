# Demo scenario — Day 27

## 1. Initial requirements

- Java 25, FFmpeg, whisper.cpp + multilingual model, Ollama with `qwen3:4b`.
- Short lecture video (use `demos/fixtures/generate-fixture.ps1`).

## 2. Startup commands

```bash
export JAVA_HOME=<path-to-jdk-25>
export WHISPER_MODEL_PATH=<path-to-ggml-model.bin>
./mvnw -pl backend spring-boot:run
```

## 3. What to show on screen

1. Browser at `http://localhost:8080`.
2. Upload `sample-lecture.mp4` with "очистка транскрипта" checked.
3. Status polling updates automatically (HTMX).
4. Download links: raw transcript, Markdown, DOCX.
5. Terminal: `ollama ps` / `ollama list` showing the local model running.

## 4. API calls

```bash
curl -F "file=@sample-lecture.mp4" -F "clean=true" http://localhost:8080/api/lectures
curl -X POST http://localhost:8080/api/lectures/<id>/process
curl http://localhost:8080/api/lectures/<id>
curl -OJ http://localhost:8080/api/lectures/<id>/download/notes.docx
```

## 5. Expected observable result

- Status transitions to `COMPLETED`.
- `notes.md` contains a title, a "Резюме" section and structured headings.
- `notes.docx` opens in Word/LibreOffice and is non-empty.
- `transcript.json` preserves original whisper.cpp output with timestamps.

## 6. Proof that the LLM is local

- Ollama listens on `http://localhost:11434`; no cloud API key is configured.
- `OLLAMA_BASE_URL` and `OLLAMA_CHAT_MODEL` are the only LLM settings.
- Turning Ollama off makes processing fail with a clear error (no fallback to cloud).

## 7. Known limitations

- Requires external tools; not available in the current verification environment
  (marked SKIPPED in `test-results.md`).
- Only one language is configured at a time (`app.tools.whisper-language`).
