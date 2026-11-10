-- Reference DDL for Training execution tables.
-- Apply after recipes.sql (and catalog tables words/kanji).
-- Documentation contract mirrored by Room v9 CatalogDatabase entities/DAO.
-- Not a migration: the unreleased Android database is replaced.
PRAGMA foreign_keys = ON;

CREATE TABLE trainings (
    id TEXT PRIMARY KEY NOT NULL CHECK(length(trim(id)) > 0),
    recipe_id TEXT REFERENCES recipes(id) ON DELETE SET NULL,
    recipe_revision INTEGER CHECK(recipe_revision IS NULL OR recipe_revision > 0),
    task_count INTEGER CHECK(task_count IS NULL OR task_count > 0),
    seconds_per_symbol INTEGER CHECK(seconds_per_symbol IS NULL OR seconds_per_symbol > 0),
    hint_mode TEXT NOT NULL DEFAULT 'reading' CHECK(hint_mode IN ('translation', 'reading', 'none')),
    stroke_hint_mode TEXT NOT NULL DEFAULT 'next_stroke' CHECK(stroke_hint_mode IN ('next_stroke', 'full_character')),
    status TEXT NOT NULL CHECK(status IN ('in_progress', 'completed', 'interrupted')),
    started_at INTEGER NOT NULL,
    ended_at INTEGER,
    CHECK(
        (status = 'in_progress' AND ended_at IS NULL)
        OR (status IN ('completed', 'interrupted') AND ended_at IS NOT NULL)
    )
);
CREATE INDEX idx_trainings_started_at ON trainings(started_at, id);
CREATE INDEX idx_trainings_recipe_id ON trainings(recipe_id);
CREATE UNIQUE INDEX idx_trainings_one_in_progress ON trainings(status)
    WHERE status = 'in_progress';

CREATE TABLE trainings_recipe_translations (
    training_id TEXT NOT NULL REFERENCES trainings(id) ON DELETE CASCADE,
    language_tag TEXT NOT NULL COLLATE NOCASE CHECK(length(trim(language_tag)) > 0),
    name TEXT NOT NULL CHECK(length(trim(name)) BETWEEN 1 AND 120),
    PRIMARY KEY(training_id, language_tag)
);

CREATE TABLE training_tasks (
    id TEXT PRIMARY KEY NOT NULL CHECK(length(trim(id)) > 0),
    training_id TEXT NOT NULL REFERENCES trainings(id) ON DELETE CASCADE,
    position INTEGER NOT NULL CHECK(position >= 0),
    word_id INTEGER REFERENCES words(id) ON DELETE SET NULL,
    written_form TEXT NOT NULL CHECK(length(trim(written_form)) > 0),
    reading TEXT NOT NULL CHECK(length(trim(reading)) > 0),
    status TEXT NOT NULL CHECK(status IN (
        'pending', 'in_progress', 'correct', 'assisted', 'skipped', 'timed_out'
    )),
    started_at INTEGER,
    ended_at INTEGER,
    elapsed_ms INTEGER NOT NULL DEFAULT 0 CHECK(elapsed_ms >= 0),
    reading_hint_used INTEGER NOT NULL DEFAULT 0 CHECK(reading_hint_used IN (0, 1)),
    translation_hint_used INTEGER NOT NULL DEFAULT 0 CHECK(translation_hint_used IN (0, 1)),
    UNIQUE(training_id, position)
);
CREATE INDEX idx_training_tasks_word_id ON training_tasks(word_id);
CREATE INDEX idx_training_tasks_training_status ON training_tasks(training_id, status);

CREATE TABLE training_task_translations (
    task_id TEXT NOT NULL REFERENCES training_tasks(id) ON DELETE CASCADE,
    language_tag TEXT NOT NULL COLLATE NOCASE CHECK(length(trim(language_tag)) > 0),
    translation TEXT NOT NULL CHECK(length(trim(translation)) > 0),
    PRIMARY KEY(task_id, language_tag)
);

CREATE TABLE training_task_symbols (
    id TEXT PRIMARY KEY NOT NULL CHECK(length(trim(id)) > 0),
    task_id TEXT NOT NULL REFERENCES training_tasks(id) ON DELETE CASCADE,
    position INTEGER NOT NULL CHECK(position >= 0),
    occurrence_id TEXT NOT NULL CHECK(length(trim(occurrence_id)) > 0),
    role TEXT NOT NULL CHECK(role IN ('practice', 'display')),
    script TEXT NOT NULL CHECK(script IN ('kanji', 'hiragana', 'katakana', 'other')),
    glyph TEXT NOT NULL CHECK(length(trim(glyph)) > 0),
    kanji_id INTEGER REFERENCES kanji(id) ON DELETE SET NULL,
    kanji_key TEXT CHECK(kanji_key IS NULL OR length(trim(kanji_key)) > 0),
    status TEXT NOT NULL CHECK(status IN (
        'empty', 'editing', 'correct', 'incorrect', 'prefilled'
    )),
    hint_request_count INTEGER NOT NULL DEFAULT 0 CHECK(hint_request_count >= 0),
    elapsed_ms INTEGER NOT NULL DEFAULT 0 CHECK(elapsed_ms >= 0),
    readability_verdict TEXT CHECK(
        readability_verdict IS NULL
        OR readability_verdict IN ('correct', 'incorrect', 'incomplete', 'unavailable')
    ),
    readability_glyph TEXT,
    shape_verdict TEXT CHECK(
        shape_verdict IS NULL
        OR shape_verdict IN ('correct', 'incorrect', 'incomplete', 'unavailable')
    ),
    shape_similarity_percent REAL CHECK(
        shape_similarity_percent IS NULL
        OR (shape_similarity_percent >= 0 AND shape_similarity_percent <= 100)
    ),
    shape_budget_percent REAL CHECK(
        shape_budget_percent IS NULL OR shape_budget_percent >= 0
    ),
    stroke_writing_verdict TEXT CHECK(
        stroke_writing_verdict IS NULL
        OR stroke_writing_verdict IN ('correct', 'incorrect', 'incomplete', 'unavailable')
    ),
    stroke_expected INTEGER CHECK(stroke_expected IS NULL OR stroke_expected >= 0),
    stroke_actual INTEGER CHECK(stroke_actual IS NULL OR stroke_actual >= 0),
    writing_sample_json TEXT,
    check_report_json TEXT,
    UNIQUE(task_id, position),
    UNIQUE(task_id, occurrence_id),
    CHECK(
        (role = 'display' AND status = 'prefilled')
        OR role = 'practice'
    )
);
CREATE INDEX idx_training_task_symbols_kanji_id ON training_task_symbols(kanji_id);
CREATE INDEX idx_training_task_symbols_kanji_key ON training_task_symbols(kanji_key);
