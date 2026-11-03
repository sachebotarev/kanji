-- Proposed additive schema for learning.db. Not an installed Room migration.
PRAGMA foreign_keys = ON;

-- 1. Training recipes
CREATE TABLE trainings (
  id TEXT PRIMARY KEY NOT NULL CHECK(length(id)>0),
  task_count INTEGER CHECK(task_count IS NULL OR task_count>0),
  selection_mode TEXT NOT NULL DEFAULT 'shuffled_cycles' CHECK(selection_mode IN ('shuffled_cycles','random_with_replacement')),
  seconds_per_word INTEGER CHECK(seconds_per_word IS NULL OR seconds_per_word>0),
  revision INTEGER NOT NULL DEFAULT 1 CHECK(revision>0),
  is_archived INTEGER NOT NULL DEFAULT 0 CHECK(is_archived IN (0,1)),
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL,
  archived_at INTEGER
);
CREATE INDEX idx_trainings_archived ON trainings(is_archived);

CREATE TABLE trainings_translations (
  training_id TEXT NOT NULL REFERENCES trainings(id) ON DELETE CASCADE,
  language_tag TEXT NOT NULL COLLATE NOCASE CHECK(length(trim(language_tag))>0),
  name TEXT NOT NULL CHECK(length(trim(name)) BETWEEN 1 AND 120),
  PRIMARY KEY(training_id,language_tag)
);

CREATE TABLE training_sources (
  id TEXT PRIMARY KEY NOT NULL,
  training_id TEXT NOT NULL REFERENCES trainings(id) ON DELETE CASCADE,
  position INTEGER NOT NULL CHECK(position>=0),
  source_id INTEGER REFERENCES sources(id) ON DELETE RESTRICT,
  chapter_from INTEGER CHECK(chapter_from IS NULL OR chapter_from>0),
  chapter_to INTEGER CHECK(chapter_to IS NULL OR chapter_to>0),
  word_id INTEGER REFERENCES words(id) ON DELETE RESTRICT,
  kanji_id INTEGER REFERENCES kanji(id) ON DELETE RESTRICT,
  jlpt_level INTEGER CHECK(jlpt_level IS NULL OR jlpt_level BETWEEN 1 AND 5),
  CHECK(chapter_from IS NULL OR chapter_to IS NULL OR chapter_from<=chapter_to),
  CHECK((chapter_from IS NULL AND chapter_to IS NULL) OR source_id IS NOT NULL),
  UNIQUE(training_id,position)
);
CREATE INDEX idx_training_sources_training_id ON training_sources(training_id);
CREATE INDEX idx_training_sources_source_id ON training_sources(source_id);
CREATE INDEX idx_training_sources_word_id ON training_sources(word_id);
CREATE INDEX idx_training_sources_kanji_id ON training_sources(kanji_id);

-- 2. Training sessions execution and history
CREATE TABLE training_sessions (
  id TEXT PRIMARY KEY NOT NULL CHECK(length(id)>0),
  training_id TEXT REFERENCES trainings(id) ON DELETE SET NULL,
  recipe_revision INTEGER CHECK(recipe_revision IS NULL OR recipe_revision>0),
  task_count INTEGER NOT NULL CHECK(task_count>0),
  seconds_per_word INTEGER CHECK(seconds_per_word IS NULL OR seconds_per_word>0),
  selection_mode TEXT NOT NULL CHECK(selection_mode IN ('shuffled_cycles','random_with_replacement')),
  status TEXT NOT NULL CHECK(status IN ('in_progress','completed','abandoned')),
  started_at INTEGER NOT NULL,
  completed_at INTEGER
);
CREATE INDEX idx_training_sessions_training_id ON training_sessions(training_id);
CREATE INDEX idx_training_sessions_status ON training_sessions(status);

CREATE TABLE training_session_items (
  id TEXT PRIMARY KEY NOT NULL CHECK(length(id)>0),
  session_id TEXT NOT NULL REFERENCES training_sessions(id) ON DELETE CASCADE,
  position INTEGER NOT NULL CHECK(position>=0),
  word_id INTEGER NOT NULL REFERENCES words(id) ON DELETE RESTRICT,
  status TEXT NOT NULL CHECK(status IN ('pending','correct','assisted','skipped','timed_out')),
  elapsed_ms INTEGER NOT NULL DEFAULT 0 CHECK(elapsed_ms>=0),
  hint_count INTEGER NOT NULL DEFAULT 0 CHECK(hint_count>=0),
  UNIQUE(session_id, position)
);
CREATE INDEX idx_training_session_items_session ON training_session_items(session_id);
CREATE INDEX idx_training_session_items_word ON training_session_items(word_id);
