"""Behavioral checks for release identities and the actual prepared SQLite schema."""
import csv
import json
from pathlib import Path
import shutil
import sqlite3
import tempfile
import unittest

import build_seed as seed


class SeedTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.inputs = Path(self.temp.name) / 'input'
        shutil.copytree(seed.ROOT / 'data/catalog', self.inputs, ignore=shutil.ignore_patterns('seed'))
        self.registry = json.loads((seed.HERE / 'identity-registry.json').read_text())

    def prepare(self, version=2):
        return seed.prepare(self.inputs, self.registry, version)

    def change_rows(self, name, change):
        path = self.inputs / name
        header, rows = seed.read_csv(path, '|')
        change(rows)
        with path.open('w', encoding='utf-8-sig', newline='') as stream:
            writer = csv.writer(stream, delimiter='|', lineterminator='\n')
            writer.writerow(header)
            writer.writerows(rows)

    def test_reordering_every_csv_preserves_all_ids_and_data(self):
        before = self.prepare()
        for path in self.inputs.glob('*.csv'):
            delimiter = '|' if path.name in {s[2] for s in seed.SOURCES} else ','
            header, rows = seed.read_csv(path, delimiter)
            with path.open('w', encoding='utf-8-sig', newline='') as stream:
                writer = csv.writer(stream, delimiter=delimiter, lineterminator='\n')
                writer.writerow(header)
                writer.writerows(reversed(rows))
        after = self.prepare()
        self.assertEqual(before[0], after[0])
        self.assertEqual(before[1], after[1])

    def test_new_word_appends_id_even_if_sorted_before_existing_words(self):
        self.change_rows('tobira-kanji.csv', lambda rows: rows.append(
            ['一あ', 'いちあ', '一', '1', 'Tobira I, глава 3', '', 'тестовое новое выражение']))
        registry, tables, _, _ = self.prepare()
        original = {r['id']: r for r in self.registry['words']}
        current = {r['id']: r for r in registry['words']}
        self.assertTrue(all(current[k] == v for k, v in original.items()))
        self.assertEqual(max(current), max(original) + 1)
        self.assertEqual(next(r[0] for r in tables['words'] if r[1] == '一あ'), max(current))

    def test_chapter_and_order_changes_keep_word_and_link_ids(self):
        before = self.prepare()
        def move(rows):
            for row in rows:
                if row[2] == '一':
                    row[3] = '10001'
                    row[4] = 'Tobira I, глава 4'
        self.change_rows('tobira-kanji.csv', move)
        after = self.prepare()
        self.assertEqual(before[0], after[0])
        self.assertEqual(before[1]['words'], after[1]['words'])
        self.assertEqual(before[1]['source_words'], after[1]['source_words'])
        self.assertNotEqual(before[1]['source_kanji'], after[1]['source_kanji'])

    def test_translation_change_does_not_change_identity(self):
        before = self.prepare()
        pair = ('一月', 'いちがつ')
        for _, _, filename in seed.SOURCES:
            def translate(rows):
                for row in rows:
                    if tuple(row[:2]) == pair:
                        row[6] = 'январь (первый месяц)'
            self.change_rows(filename, translate)
        after = self.prepare()
        self.assertEqual(before[0], after[0])
        self.assertEqual(before[1]['words'], after[1]['words'])
        self.assertNotEqual(before[1]['words_translations'], after[1]['words_translations'])

    def test_removed_word_is_archived_and_id_never_recycled(self):
        word = self.registry['words'][0]
        pair = (word['written_form'], word['reading'])
        for _, _, filename in seed.SOURCES:
            self.change_rows(filename, lambda rows: rows.__setitem__(slice(None),
                             [r for r in rows if tuple(r[:2]) != pair]))
        registry, tables, _, _ = self.prepare()
        self.assertEqual(self.registry, registry)
        self.assertEqual(next(r[3] for r in tables['words'] if r[0] == word['id']), 1)
        self.assertFalse(any(r[2] == word['id'] for r in tables['source_words']))

    def test_conflicting_translation_fails_without_mutating_registry(self):
        original = json.dumps(self.registry)
        self.change_rows('tobira-kanji.csv', lambda rows: rows.append(
            [*rows[0][:6], 'другой перевод']))
        with self.assertRaisesRegex(ValueError, 'Conflicting translation'):
            self.prepare()
        self.assertEqual(original, json.dumps(self.registry))

    def test_real_database_integrity_foreign_keys_and_personal_null_uniqueness(self):
        _, tables, _, _ = self.prepare()
        path = Path(self.temp.name) / 'test.sqlite'
        seed.make_database(path, tables)
        con = sqlite3.connect(path)
        self.addCleanup(con.close)
        con.execute('PRAGMA foreign_keys=ON')
        self.assertEqual(con.execute('PRAGMA integrity_check').fetchall(), [('ok',)])
        self.assertEqual(con.execute('PRAGMA foreign_key_check').fetchall(), [])
        con.execute("INSERT INTO sources VALUES(-1,'personal','personal',0)")
        con.execute('INSERT INTO source_words VALUES(-1,-1,1,NULL)')
        with self.assertRaises(sqlite3.IntegrityError):
            con.execute('INSERT INTO source_words VALUES(-2,-1,1,NULL)')
        with self.assertRaises(sqlite3.IntegrityError):
            con.execute('DELETE FROM words WHERE id=1')
        with self.assertRaises(sqlite3.IntegrityError):
            con.execute("INSERT INTO words_translations VALUES(1,'RU','дубль языка')")
        con.execute("INSERT INTO words_translations VALUES(1,'pt-BR','exemplo')")
        with self.assertRaises(sqlite3.IntegrityError):
            con.execute("INSERT INTO words_translations VALUES(1,'PT-br','дубль тега')")


if __name__ == '__main__':
    unittest.main()
