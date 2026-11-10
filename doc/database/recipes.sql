-- Reference DDL for Recipe Builder tables implemented by Room CatalogDatabase entities/DAO.
-- SQL contract test applies this after personal-dictionary.sql over exported v4.
-- Not a migration: the unreleased Android database is replaced.
-- Training execution lives in trainings.sql / Room v9 CatalogDatabase.
PRAGMA foreign_keys = ON;

CREATE TABLE recipes (
    id TEXT PRIMARY KEY NOT NULL CHECK(length(trim(id)) > 0),
    position INTEGER NOT NULL DEFAULT 0 CHECK(position >= 0),
    is_active INTEGER NOT NULL DEFAULT 0 CHECK(is_active IN (0, 1)),
    task_count INTEGER CHECK(task_count IS NULL OR task_count > 0),
    selection_mode TEXT NOT NULL DEFAULT 'shuffled_cycles' CHECK(selection_mode = 'shuffled_cycles'),
    seconds_per_symbol INTEGER CHECK(seconds_per_symbol IS NULL OR seconds_per_symbol > 0),
    hint_mode TEXT NOT NULL DEFAULT 'reading' CHECK(hint_mode IN ('translation', 'reading', 'none')),
    stroke_hint_mode TEXT NOT NULL DEFAULT 'next_stroke' CHECK(stroke_hint_mode IN ('next_stroke', 'full_character')),
    revision INTEGER NOT NULL DEFAULT 1 CHECK(revision > 0),
    is_archived INTEGER NOT NULL DEFAULT 0 CHECK(is_archived IN (0, 1)),
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    archived_at INTEGER,
    CHECK(is_archived = 0 OR is_active = 0)
);
CREATE INDEX idx_recipes_position ON recipes(is_archived, position, id);
CREATE UNIQUE INDEX idx_recipes_active ON recipes(is_active) WHERE is_active = 1 AND is_archived = 0;

CREATE TABLE recipes_translations (
    recipe_id TEXT NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    language_tag TEXT NOT NULL COLLATE NOCASE CHECK(length(trim(language_tag)) > 0),
    name TEXT NOT NULL CHECK(length(trim(name)) BETWEEN 1 AND 120),
    PRIMARY KEY(recipe_id, language_tag)
);

CREATE TABLE recipe_materials (
    id TEXT PRIMARY KEY NOT NULL CHECK(length(trim(id)) > 0),
    recipe_id TEXT NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    position INTEGER NOT NULL CHECK(position >= 0),
    material_type TEXT NOT NULL CHECK(material_type IN ('book','dictionary','kanji','words')),
    source_id INTEGER REFERENCES sources(id) ON DELETE RESTRICT,
    UNIQUE(recipe_id, position),
    CHECK((material_type IN ('book','dictionary') AND source_id IS NOT NULL)
       OR (material_type IN ('kanji','words') AND source_id IS NULL))
);
CREATE INDEX idx_recipe_materials_source_id ON recipe_materials(source_id);
CREATE TRIGGER recipe_materials_scope_insert BEFORE INSERT ON recipe_materials
WHEN NEW.source_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM sources WHERE id=NEW.source_id AND is_archived=0
    AND ((NEW.material_type='dictionary' AND source_type='personal')
      OR (NEW.material_type='book' AND source_type IN ('textbook','book'))))
BEGIN SELECT RAISE(ABORT, 'Invalid training source scope'); END;
CREATE TRIGGER recipe_materials_scope_update BEFORE UPDATE ON recipe_materials
WHEN NEW.source_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM sources WHERE id=NEW.source_id AND is_archived=0
    AND ((NEW.material_type='dictionary' AND source_type='personal')
      OR (NEW.material_type='book' AND source_type IN ('textbook','book'))))
BEGIN SELECT RAISE(ABORT, 'Invalid training source scope'); END;

CREATE TABLE recipe_materials_chapters (
    material_id TEXT NOT NULL REFERENCES recipe_materials(id) ON DELETE CASCADE,
    chapter_number INTEGER NOT NULL CHECK(chapter_number > 0),
    PRIMARY KEY(material_id, chapter_number)
);
CREATE TRIGGER recipe_materials_chapters_insert BEFORE INSERT ON recipe_materials_chapters
WHEN NOT EXISTS (SELECT 1 FROM recipe_materials s JOIN source_kanji k ON k.source_id=s.source_id
    WHERE s.id=NEW.material_id AND s.material_type='book' AND k.chapter_number=NEW.chapter_number)
BEGIN SELECT RAISE(ABORT, 'Chapter must belong to the selected textbook'); END;
CREATE TRIGGER recipe_materials_chapters_update BEFORE UPDATE ON recipe_materials_chapters
WHEN NOT EXISTS (SELECT 1 FROM recipe_materials s JOIN source_kanji k ON k.source_id=s.source_id
    WHERE s.id=NEW.material_id AND s.material_type='book' AND k.chapter_number=NEW.chapter_number)
BEGIN SELECT RAISE(ABORT, 'Chapter must belong to the selected textbook'); END;

CREATE TABLE recipe_materials_kanji (
    material_id TEXT NOT NULL REFERENCES recipe_materials(id) ON DELETE CASCADE,
    kanji_id INTEGER NOT NULL REFERENCES kanji(id) ON DELETE RESTRICT,
    PRIMARY KEY(material_id, kanji_id)
);
CREATE INDEX idx_recipe_materials_kanji_kanji ON recipe_materials_kanji(kanji_id);
CREATE TRIGGER recipe_materials_kanji_insert BEFORE INSERT ON recipe_materials_kanji
WHEN NOT EXISTS (SELECT 1 FROM recipe_materials WHERE id=NEW.material_id AND material_type IN ('book','dictionary','kanji'))
BEGIN SELECT RAISE(ABORT, 'Invalid kanji material type'); END;
CREATE TRIGGER recipe_materials_kanji_update BEFORE UPDATE ON recipe_materials_kanji
WHEN NOT EXISTS (SELECT 1 FROM recipe_materials WHERE id=NEW.material_id AND material_type IN ('book','dictionary','kanji'))
BEGIN SELECT RAISE(ABORT, 'Invalid kanji material type'); END;

CREATE TABLE recipe_materials_words (
    material_id TEXT NOT NULL REFERENCES recipe_materials(id) ON DELETE CASCADE,
    word_id INTEGER NOT NULL REFERENCES words(id) ON DELETE RESTRICT,
    PRIMARY KEY(material_id, word_id)
);
CREATE INDEX idx_recipe_materials_words_word ON recipe_materials_words(word_id);
CREATE TRIGGER recipe_materials_words_insert BEFORE INSERT ON recipe_materials_words
WHEN NOT EXISTS (SELECT 1 FROM recipe_materials WHERE id=NEW.material_id AND material_type='words')
BEGIN SELECT RAISE(ABORT, 'Explicit words require words material'); END;
CREATE TRIGGER recipe_materials_words_update BEFORE UPDATE ON recipe_materials_words
WHEN NOT EXISTS (SELECT 1 FROM recipe_materials WHERE id=NEW.material_id AND material_type='words')
BEGIN SELECT RAISE(ABORT, 'Explicit words require words material'); END;

CREATE TABLE recipe_materials_tags (
    material_id TEXT NOT NULL REFERENCES recipe_materials(id) ON DELETE CASCADE,
    tag_id INTEGER NOT NULL REFERENCES dictionary_tags(id) ON DELETE CASCADE,
    PRIMARY KEY(material_id, tag_id)
);
CREATE INDEX idx_recipe_materials_tags_tag ON recipe_materials_tags(tag_id);
CREATE TRIGGER recipe_materials_tags_insert BEFORE INSERT ON recipe_materials_tags
WHEN NOT EXISTS (SELECT 1 FROM recipe_materials WHERE id=NEW.material_id AND material_type='dictionary')
BEGIN SELECT RAISE(ABORT, 'Tags require dictionary material'); END;
CREATE TRIGGER recipe_materials_tags_update BEFORE UPDATE ON recipe_materials_tags
WHEN NOT EXISTS (SELECT 1 FROM recipe_materials WHERE id=NEW.material_id AND material_type='dictionary')
BEGIN SELECT RAISE(ABORT, 'Tags require dictionary material'); END;

CREATE TABLE recipe_materials_jlpt (
    material_id TEXT NOT NULL REFERENCES recipe_materials(id) ON DELETE CASCADE,
    jlpt_level INTEGER NOT NULL,
    PRIMARY KEY(material_id, jlpt_level),
    CHECK (jlpt_level = -1 OR jlpt_level BETWEEN 1 AND 5)
);
CREATE TRIGGER recipe_materials_jlpt_insert BEFORE INSERT ON recipe_materials_jlpt
WHEN NOT EXISTS (SELECT 1 FROM recipe_materials WHERE id=NEW.material_id AND material_type='kanji')
BEGIN SELECT RAISE(ABORT, 'JLPT requires kanji material'); END;
CREATE TRIGGER recipe_materials_jlpt_update BEFORE UPDATE ON recipe_materials_jlpt
WHEN NOT EXISTS (SELECT 1 FROM recipe_materials WHERE id=NEW.material_id AND material_type='kanji')
BEGIN SELECT RAISE(ABORT, 'JLPT requires kanji material'); END;
