"""Execute proposed additive DDL against the exported Room v4 schema (stdlib only)."""
import json
import sqlite3
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


class BuilderSchemaTest(unittest.TestCase):
    def setUp(self):
        self.db = sqlite3.connect(':memory:')
        self.db.execute('PRAGMA foreign_keys=ON')
        schema = json.loads((ROOT / 'data/catalogdb/schemas/ru.mobui.kanji.data.catalog.CatalogDatabase/4.json').read_text())
        for entity in schema['database']['entities']:
            self.db.execute(entity['createSql'].replace('${TABLE_NAME}', entity['tableName']))
            for index in entity.get('indices', []):
                self.db.execute(index['createSql'].replace('${TABLE_NAME}', entity['tableName']))
        for name in ('personal-dictionary.sql', 'recipes.sql'):
            self.db.executescript((Path(__file__).parent / name).read_text())
        self.db.executemany('INSERT INTO sources(id,source_type,original_title) VALUES(?,?,?)', [(1,'textbook','Book'),(-1,'personal','Personal')])
        self.db.execute("INSERT INTO kanji(id,symbol,stroke_count,is_kyoiku,is_joyo,is_jinmeiyo) VALUES(1,'日',4,1,1,0)")
        self.db.execute('INSERT INTO source_kanji VALUES(1,1,1,1)')
        self.db.executemany('INSERT INTO words(id,written_form,reading,is_user_created) VALUES(?,?,?,?)', [(1,'日','ひ',0),(-1,'休日','きゅうじつ',1),(-2,'日々','ひび',1)])
        self.db.executemany('INSERT INTO dictionary_tags(id,color,created_at,updated_at) VALUES(?,?,0,0)', [(1,'green'),(2,'blue')])
        self.db.execute("INSERT INTO recipes(id,is_active,created_at,updated_at) VALUES('t',1,0,0)")
        self.db.executemany('INSERT INTO recipe_materials(id,recipe_id,position,material_type,source_id) VALUES(?,?,?,?,?)', [('b','t',0,'book',1),('d','t',1,'dictionary',-1),('k','t',2,'kanji',None),('w','t',3,'words',None)])
        self.db.commit()

    def tearDown(self):
        self.db.close()

    def rejects(self, sql, args=()):
        with self.assertRaises(sqlite3.IntegrityError):
            self.db.execute(sql, args)

    def test_colors_and_personal_only(self):
        self.rejects("INSERT INTO dictionary_tags(color,created_at,updated_at) VALUES('sage',0,0)")
        self.rejects('INSERT INTO word_tags VALUES(1,1,0)')
        self.db.execute('INSERT INTO word_tags VALUES(-1,1,0)')
        self.rejects('INSERT INTO word_tags VALUES(-1,1,0)')
        self.db.execute('UPDATE words SET is_archived=1 WHERE id=-2')
        self.rejects('INSERT INTO word_tags VALUES(-2,1,0)')

    def test_translations_and_keys(self):
        self.db.executemany('INSERT INTO dictionary_tags_translations VALUES(?,?,?,?)', [(1,'ru','Повторить','повторить'),(1,'en','Review','review'),(1,'de','Wiederholen','wiederholen')])
        self.rejects("INSERT INTO dictionary_tags_translations VALUES(2,'RU','ПОВТОРИТЬ','повторить')")
        self.db.executemany('INSERT INTO recipes_translations VALUES(?,?,?)', [('t','ru','Рецепт'),('t','en','Recipe')])
        self.assertEqual(self.db.execute('SELECT count(*) FROM dictionary_tags_translations').fetchone()[0], 3)

    def test_material_types_chapters_and_fk(self):
        self.db.execute("INSERT INTO recipe_materials_chapters VALUES('b',1)")
        for source, chapter in [('b',0),('b',2),('d',1)]:
            self.rejects('INSERT INTO recipe_materials_chapters VALUES(?,?)', (source,chapter))
        self.rejects("INSERT INTO recipe_materials VALUES('bad','t',4,'dictionary',1)")
        self.rejects("INSERT INTO recipe_materials_tags VALUES('b',1)")
        self.rejects("INSERT INTO recipe_materials_words VALUES('k',1)")
        self.rejects("INSERT INTO recipe_materials_kanji VALUES('w',1)")
        self.rejects("INSERT INTO recipe_materials_kanji VALUES('k',999)")
        self.db.execute("INSERT INTO recipe_materials_words VALUES('w',-1)")
        self.db.execute("INSERT INTO recipe_materials_kanji VALUES('k',1)")
        self.assertEqual(self.db.execute('PRAGMA foreign_key_check').fetchall(), [])

    def test_active_selection_and_scope(self):
        self.rejects("INSERT INTO recipes(id,is_active,created_at,updated_at) VALUES('other',1,0,0)")
        self.rejects("UPDATE recipes SET is_archived=1 WHERE id='t'")
        self.rejects("UPDATE recipes SET selection_mode='random_with_replacement' WHERE id='t'")
        self.rejects("UPDATE recipes SET task_count=0 WHERE id='t'")
        self.rejects("UPDATE recipes SET seconds_per_word=0 WHERE id='t'")
        tables = {row[0] for row in self.db.execute("SELECT name FROM sqlite_master WHERE type='table'")}
        # Recipe DDL must not create training execution tables (see trainings.sql separately).
        self.assertFalse({'trainings', 'training_tasks', 'training_task_symbols'} & tables)

    def test_delete_cascades_without_word_loss(self):
        self.db.execute('INSERT INTO word_tags VALUES(-1,1,0)')
        self.db.execute("INSERT INTO recipe_materials_tags VALUES('d',1)")
        self.db.execute("INSERT INTO dictionary_tags_translations VALUES(1,'ru','Тег','тег')")
        self.db.execute('DELETE FROM dictionary_tags WHERE id=1')
        for table in ('word_tags','recipe_materials_tags','dictionary_tags_translations'):
            self.assertEqual(self.db.execute(f'SELECT count(*) FROM {table}').fetchone()[0], 0)
        self.assertEqual(self.db.execute('SELECT count(*) FROM words').fetchone()[0], 3)
        self.db.execute("DELETE FROM recipes WHERE id='t'")
        self.assertEqual(self.db.execute('SELECT count(*) FROM recipe_materials').fetchone()[0], 0)

    def test_draft_rollback_and_tag_or(self):
        self.db.execute('BEGIN')
        self.db.execute("INSERT INTO dictionary_tags(id,color,created_at,updated_at) VALUES(3,'gold',0,0)")
        self.db.execute('INSERT INTO word_tags VALUES(-1,3,0)')
        self.db.rollback()
        self.assertEqual(self.db.execute('SELECT count(*) FROM dictionary_tags WHERE id=3').fetchone()[0], 0)
        self.db.executemany('INSERT INTO word_tags VALUES(?,?,0)', [(-1,1),(-1,2),(-2,2)])
        query = '''SELECT id FROM words w WHERE is_user_created=1 AND is_archived=0
            AND EXISTS(SELECT 1 FROM word_tags t WHERE t.word_id=w.id AND tag_id IN (?,?))
            AND instr(written_form,?)>0 ORDER BY id'''
        self.assertEqual(self.db.execute(query, (1,2,'日')).fetchall(), [(-2,),(-1,)])
        self.assertEqual(self.db.execute(query, (1,1,'休')).fetchall(), [(-1,)])


if __name__ == '__main__':
    unittest.main(verbosity=2)
