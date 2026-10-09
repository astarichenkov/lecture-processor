# Demo fixtures

Fixtures used by Days 27–29 demos. Large video files are **not** committed.

## Synthetic video

`generate-fixture.ps1` (Windows) and `generate-fixture.sh` (bash) create a short
synthetic lecture video with FFmpeg: a coloured background, a title and a few
slides with visible text, plus a silent/sine audio track. The content is
original and contains no copyrighted material.

```powershell
./demos/fixtures/generate-fixture.ps1 -Output demos/fixtures/sample-lecture.mp4
```

```bash
./demos/fixtures/generate-fixture.sh demos/fixtures/sample-lecture.mp4
```

Requires FFmpeg on `PATH` (or use `--ffmpeg <path>`).

## Text/transcript fixtures (committed, no tools required)

| File | Purpose |
|------|---------|
| `sample-transcript.json` | A hand-written whisper.cpp-style transcript used by tests and demos when real transcription is unavailable. |
| `sample-lecture-text.txt` | Plain lecture text used for benchmark and RAG demos. |

These are original synthetic texts about linear algebra, not real lecture
recordings.
