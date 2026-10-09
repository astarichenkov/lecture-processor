# Demo scenario — Day 29

## Part A — benchmarks

1. Открыть `http://localhost:8080/benchmarks`, показать конфигурации A–D.
2. Запустить: `curl -X POST "http://localhost:8080/api/benchmarks/run?lectureId=<id>"`.
3. Открыть `<storage>/benchmarks/<timestamp>/results.md` и `results.csv`.
4. Показать метрики: время, токены, токены/с, сохранение ключевых фактов.
5. Показать строку VRAM `unknown` — честная пометка вместо выдуманных данных.

## Part B — slides

1. Обработать лекцию с флагом «извлечь слайды».
2. Показать каталог `data/lectures/<id>/slides/` — только уникальные кадры.
3. Открыть `notes.docx` — изображения вставлены в соответствующие разделы
   конспекта с подписью таймкода; не привязанные — в приложении.

## Expected result

- JSON/CSV/Markdown файлы бенчмарка созданы.
- Число изображений ≤ `app.slides.max-images`, дубликаты удалены.
- DOCX содержит изображения (`document.getAllPictures()` не пуст).

## Local proof

- Все модели запускаются через Ollama на loopback.

## Known limitations

- Benchmarks зависят от доступности моделей; недоступные помечаются SKIPPED.
- Выравнивание слайдов с текстом — по временным меткам (консервативно).
- Оценка «качества» ограничена детерминированными проверками.
