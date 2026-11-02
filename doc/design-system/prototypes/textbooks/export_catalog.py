"""Export the project's complete read-only catalog and Android strings for the HTML baseline."""
from pathlib import Path
import hashlib
import json
import sqlite3
import xml.etree.ElementTree as ET

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[3]
SOURCE = ROOT / "data/catalog/seed/catalog.sqlite"


def export():
    database = sqlite3.connect(f"file:{SOURCE}?mode=ro", uri=True)
    database.row_factory = sqlite3.Row

    def rows(table, order="id"):
        return [dict(row) for row in database.execute(f"SELECT * FROM {table} ORDER BY {order}")]

    def translations(table, owner_column, owner_id, text_column):
        return [
            {"language_tag": row["language_tag"], "text": row[text_column]}
            for row in database.execute(f"SELECT * FROM {table} WHERE {owner_column}=? ORDER BY language_tag", (owner_id,))
        ]

    content_dir = ROOT / 'data/catalog/content'
    source_descriptions = {}
    for item in json.loads((content_dir / 'source-descriptions.json').read_text()):
        source_descriptions.setdefault(item['source_id'], []).append({
            'language_tag': item['language_tag'], 'text': item['text']})
    sources = []
    for row in rows("sources"):
        if row["source_type"] == "personal" or row["is_archived"]:
            continue
        sources.append({"id": row["id"], "original_title": row["original_title"],
                        "translations": translations("sources_translations", "source_id", row["id"], "title"),
                        "descriptions": source_descriptions.get(row["id"], [])})
    content_translations = {}
    for item in json.loads((content_dir / 'words-translations.json').read_text()):
        content_translations.setdefault(item['word_id'], []).append({
            'language_tag': item['language_tag'], 'text': item['translation'],
            'usage_note': item['usage_note']})
    classifications = {item['kanji_id']: item for item in json.loads(
        (content_dir / 'kanji-classifications.json').read_text())}
    words = []
    for row in rows("words"):
        if row["is_archived"]:
            continue
        words.append({"id": row["id"], "written": row["written_form"], "reading": row["reading"],
                      "translations": translations("words_translations", "word_id", row["id"], "translation"),
                      "content_translations": content_translations[row['id']],
                      "parts": [dict(item) for item in database.execute(
                          "SELECT written_part, reading_part FROM word_reading_parts WHERE word_id=? ORDER BY position", (row["id"],))]})
    kanji = []
    for row in rows("kanji"):
        meanings = [dict(item) for item in database.execute(
            "SELECT language_tag, position, meaning AS text FROM kanji_translations WHERE kanji_id=? ORDER BY language_tag, position", (row["id"],))]
        readings = [dict(item) for item in database.execute(
            "SELECT reading_type, position, reading AS text FROM kanji_readings WHERE kanji_id=? ORDER BY reading_type, position", (row["id"],))]
        kanji.append({**row, "translations": meanings, "readings": readings,
                      "classification": classifications[row['id']]})
    radicals = [{**row, "translations": translations("radicals_translations", "radical_id", row["id"], "name")}
                for row in rows("radicals")]

    strings, plurals = {}, {}
    for language, directory in [("ru", "values"), ("en", "values-en")]:
        strings[language], plurals[language] = {}, {}
        for module in ["feature/catalog", "core/wordui"]:
            tree = ET.parse(ROOT / module / "src/main/res" / directory / "strings.xml")
            for item in tree.getroot():
                if item.tag == "string":
                    strings[language][item.attrib["name"]] = "".join(item.itertext())
                elif item.tag == "plurals":
                    plurals[language][item.attrib["name"]] = {x.attrib["quantity"]: "".join(x.itertext()) for x in item}
    payload = {"schema_version": 1, "source_sha256": hashlib.sha256(SOURCE.read_bytes()).hexdigest(),
               "content_sha256": {p.name: hashlib.sha256(p.read_bytes()).hexdigest()
                                  for p in sorted(content_dir.glob('*.json'))},
               "sources": sources, "words": words, "kanji": kanji, "radicals": radicals,
               "placements": rows("source_kanji", "source_id, kanji_number"),
               "links": rows("source_words"), "strings": strings, "plurals": plurals}
    database.close()
    target = HERE / "assets/catalog.js"
    target.write_text("/* Generated from the project database and Android resources. See README.md. */\n"
                      + "window.TEXTBOOK_CATALOG=" + json.dumps(payload, ensure_ascii=False, separators=(",", ":")) + ";\n", encoding="utf-8")
    print({key: len(payload[key]) for key in ["sources", "words", "kanji", "radicals", "placements", "links"]})


if __name__ == "__main__":
    export()
