CREATE TABLE IF NOT EXISTS lecture (
    id                   TEXT PRIMARY KEY,
    original_filename    TEXT NOT NULL,
    stored_filename      TEXT NOT NULL,
    size_bytes           INTEGER NOT NULL,
    content_type         TEXT,
    status               TEXT NOT NULL,
    error_message        TEXT,
    duration_ms          INTEGER,
    clean_transcript     INTEGER NOT NULL DEFAULT 0,
    extract_slides       INTEGER NOT NULL DEFAULT 0,
    raw_transcript_path  TEXT,
    cleaned_text         TEXT,
    notes_markdown       TEXT,
    docx_path            TEXT,
    created_at           TEXT NOT NULL,
    updated_at           TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS transcript_segment (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    lecture_id   TEXT NOT NULL,
    sequence     INTEGER NOT NULL,
    start_ms     INTEGER NOT NULL,
    end_ms       INTEGER NOT NULL,
    text         TEXT NOT NULL,
    FOREIGN KEY (lecture_id) REFERENCES lecture(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_segment_lecture ON transcript_segment(lecture_id, sequence);

CREATE TABLE IF NOT EXISTS lecture_chunk (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    lecture_id    TEXT NOT NULL,
    sequence      INTEGER NOT NULL,
    start_ms      INTEGER NOT NULL,
    end_ms        INTEGER NOT NULL,
    text          TEXT NOT NULL,
    embedding     BLOB,
    embedding_dim INTEGER,
    FOREIGN KEY (lecture_id) REFERENCES lecture(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_chunk_lecture ON lecture_chunk(lecture_id, sequence);

CREATE TABLE IF NOT EXISTS chat_message (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    lecture_id   TEXT NOT NULL,
    session_id   TEXT NOT NULL,
    role         TEXT NOT NULL,
    content      TEXT NOT NULL,
    sources_json TEXT,
    created_at   TEXT NOT NULL,
    FOREIGN KEY (lecture_id) REFERENCES lecture(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_chat_session ON chat_message(lecture_id, session_id, id);

CREATE TABLE IF NOT EXISTS slide (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    lecture_id   TEXT NOT NULL,
    sequence     INTEGER NOT NULL,
    timestamp_ms INTEGER NOT NULL,
    image_path   TEXT NOT NULL,
    phash        TEXT,
    FOREIGN KEY (lecture_id) REFERENCES lecture(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_slide_lecture ON slide(lecture_id, sequence);
