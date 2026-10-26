PRAGMA foreign_keys = ON;
CREATE TABLE catalog_metadata (
 singleton INTEGER PRIMARY KEY CHECK(singleton = 1),
 schema_version INTEGER NOT NULL CHECK(schema_version > 0),
 content_version INTEGER NOT NULL CHECK(content_version > 0)
);
CREATE TABLE radicals (
 id INTEGER PRIMARY KEY CHECK(id BETWEEN 1 AND 214), symbol TEXT NOT NULL UNIQUE CHECK(length(symbol)=1),
 stroke_count INTEGER NOT NULL CHECK(stroke_count BETWEEN 1 AND 17)
);
CREATE TABLE radicals_translations (
 radical_id INTEGER NOT NULL REFERENCES radicals(id) ON DELETE CASCADE ON UPDATE RESTRICT,
 language_tag TEXT NOT NULL COLLATE NOCASE CHECK(length(trim(language_tag))>0),
 name TEXT NOT NULL CHECK(length(trim(name))>0), PRIMARY KEY(radical_id,language_tag)
);
CREATE TABLE kanji (
 id INTEGER PRIMARY KEY, symbol TEXT NOT NULL UNIQUE CHECK(length(symbol)=1),
 stroke_count INTEGER NOT NULL CHECK(stroke_count>0),
 radical_id INTEGER REFERENCES radicals(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 is_kyoiku INTEGER NOT NULL CHECK(is_kyoiku IN (0,1)),
 is_joyo INTEGER NOT NULL CHECK(is_joyo IN (0,1)),
 is_jinmeiyo INTEGER NOT NULL CHECK(is_jinmeiyo IN (0,1)),
 CHECK(is_kyoiku=0 OR is_joyo=1),
 CHECK((id=12293 AND radical_id IS NULL) OR (id<>12293 AND radical_id IS NOT NULL))
);
CREATE INDEX kanji_radical_id ON kanji(radical_id);
CREATE TABLE kanji_translations (
 kanji_id INTEGER NOT NULL REFERENCES kanji(id) ON DELETE CASCADE ON UPDATE RESTRICT,
 language_tag TEXT NOT NULL COLLATE NOCASE CHECK(length(trim(language_tag))>0),
 position INTEGER NOT NULL CHECK(position>0), meaning TEXT NOT NULL CHECK(length(trim(meaning))>0),
 PRIMARY KEY(kanji_id,language_tag,position)
);
CREATE TABLE kanji_readings (
 kanji_id INTEGER NOT NULL REFERENCES kanji(id) ON DELETE CASCADE ON UPDATE RESTRICT,
 reading_type TEXT NOT NULL CHECK(reading_type IN ('on','kun')),
 position INTEGER NOT NULL CHECK(position>0), reading TEXT NOT NULL CHECK(length(trim(reading))>0),
 PRIMARY KEY(kanji_id,reading_type,position)
);
CREATE INDEX kanji_readings_reading ON kanji_readings(reading);
CREATE TABLE words (
 id INTEGER PRIMARY KEY CHECK(id<>0), written_form TEXT NOT NULL CHECK(length(trim(written_form))>0),
 reading TEXT NOT NULL CHECK(length(trim(reading))>0),
 is_archived INTEGER NOT NULL DEFAULT 0 CHECK(is_archived IN (0,1)), UNIQUE(written_form,reading)
);
CREATE TABLE words_translations (
 word_id INTEGER NOT NULL REFERENCES words(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 language_tag TEXT NOT NULL COLLATE NOCASE CHECK(length(trim(language_tag))>0),
 translation TEXT NOT NULL CHECK(length(trim(translation))>0), PRIMARY KEY(word_id,language_tag)
);
CREATE TABLE word_reading_parts (
 word_id INTEGER NOT NULL REFERENCES words(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 position INTEGER NOT NULL CHECK(position>0), written_part TEXT NOT NULL CHECK(length(written_part)>0),
 reading_part TEXT NOT NULL CHECK(length(reading_part)>0), PRIMARY KEY(word_id,position)
);
CREATE TABLE sources (
 id INTEGER PRIMARY KEY CHECK(id<>0), source_type TEXT NOT NULL CHECK(source_type IN ('textbook','book','personal')),
 original_title TEXT NOT NULL CHECK(length(trim(original_title))>0),
 is_archived INTEGER NOT NULL DEFAULT 0 CHECK(is_archived IN (0,1))
);
CREATE TABLE sources_translations (
 source_id INTEGER NOT NULL REFERENCES sources(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 language_tag TEXT NOT NULL COLLATE NOCASE CHECK(length(trim(language_tag))>0),
 title TEXT NOT NULL CHECK(length(trim(title))>0), PRIMARY KEY(source_id,language_tag)
);
CREATE TABLE source_kanji (
 source_id INTEGER NOT NULL REFERENCES sources(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 kanji_id INTEGER NOT NULL REFERENCES kanji(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 kanji_number INTEGER NOT NULL CHECK(kanji_number>0), chapter_number INTEGER NOT NULL CHECK(chapter_number>0),
 PRIMARY KEY(source_id,kanji_id), UNIQUE(source_id,kanji_number)
);
CREATE INDEX source_kanji_chapter ON source_kanji(source_id,chapter_number,kanji_number);
CREATE TABLE source_words (
 id INTEGER PRIMARY KEY CHECK(id<>0),
 source_id INTEGER NOT NULL REFERENCES sources(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 word_id INTEGER NOT NULL REFERENCES words(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 kanji_id INTEGER,
 FOREIGN KEY(source_id,kanji_id) REFERENCES source_kanji(source_id,kanji_id) ON DELETE RESTRICT ON UPDATE RESTRICT
);
CREATE UNIQUE INDEX source_words_with_kanji ON source_words(source_id,word_id,kanji_id) WHERE kanji_id IS NOT NULL;
CREATE UNIQUE INDEX source_words_without_kanji ON source_words(source_id,word_id) WHERE kanji_id IS NULL;
CREATE INDEX source_words_word ON source_words(word_id);
CREATE INDEX source_words_kanji ON source_words(source_id,kanji_id);
