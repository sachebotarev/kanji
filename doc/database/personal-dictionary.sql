-- Reference DDL for the feature tables implemented by Room v5 entities/DAO.
-- SQL contract test applies this over exported v4, before recipes.sql.
-- Not a migration: the unreleased Android database is replaced. No training execution/statistics.
PRAGMA foreign_keys = ON;

CREATE TABLE dictionary_tags (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    color TEXT NOT NULL DEFAULT 'neutral'
        CHECK(color IN ('neutral','green','blue','violet','pink','orange','gold')),
    position INTEGER NOT NULL DEFAULT 0 CHECK(position >= 0),
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
CREATE INDEX idx_dictionary_tags_position ON dictionary_tags(position, id);

CREATE TABLE dictionary_tags_translations (
    tag_id INTEGER NOT NULL REFERENCES dictionary_tags(id) ON DELETE CASCADE,
    language_tag TEXT NOT NULL COLLATE NOCASE CHECK(length(trim(language_tag)) > 0),
    name TEXT NOT NULL CHECK(length(trim(name)) BETWEEN 1 AND 60),
    -- Repository supplies NFKC + trimmed/case-folded key; SQLite NOCASE is ASCII-only.
    name_key TEXT NOT NULL CHECK(length(trim(name_key)) > 0),
    PRIMARY KEY(tag_id, language_tag),
    UNIQUE(language_tag, name_key)
);
CREATE INDEX idx_dictionary_tags_translations_name ON dictionary_tags_translations(name_key);

CREATE TABLE word_tags (
    word_id INTEGER NOT NULL REFERENCES words(id) ON DELETE CASCADE,
    tag_id INTEGER NOT NULL REFERENCES dictionary_tags(id) ON DELETE CASCADE,
    created_at INTEGER NOT NULL,
    PRIMARY KEY(word_id, tag_id)
);
CREATE INDEX idx_word_tags_tag ON word_tags(tag_id, word_id);

CREATE TRIGGER word_tags_personal_insert BEFORE INSERT ON word_tags
WHEN NOT EXISTS (SELECT 1 FROM words WHERE id=NEW.word_id AND is_user_created=1 AND is_archived=0)
BEGIN SELECT RAISE(ABORT, 'Tags require an active personal word'); END;
CREATE TRIGGER word_tags_personal_update BEFORE UPDATE ON word_tags
WHEN NOT EXISTS (SELECT 1 FROM words WHERE id=NEW.word_id AND is_user_created=1 AND is_archived=0)
BEGIN SELECT RAISE(ABORT, 'Tags require an active personal word'); END;
