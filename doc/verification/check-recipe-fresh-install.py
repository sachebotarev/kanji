#!/usr/bin/env python3
"""Read-only Room check after a separate fresh installation on a dedicated emulator."""
import argparse
import hashlib
import json
from pathlib import Path
import sqlite3
import subprocess
import tempfile

parser = argparse.ArgumentParser()
parser.add_argument('--serial', default='emulator-5554')
parser.add_argument('--output', type=Path, default=Path('doc/verification/recipe-terminology-fresh-install.json'))
args = parser.parse_args()
if not args.serial.startswith('emulator-'):
    parser.error('Use an isolated emulator, never a user phone')
adb = str(Path.home() / 'Library/Android/sdk/platform-tools/adb')
with tempfile.TemporaryDirectory(prefix='recipe-fresh-room-') as directory:
    database = Path(directory) / 'kanji.db'
    for suffix in ('', '-wal', '-shm'):
        result = subprocess.run([adb, '-s', args.serial, 'exec-out', 'run-as', 'ru.mobui.kanji',
                                 'cat', 'databases/kanji.db' + suffix], capture_output=True, check=False)
        if not suffix:
            assert result.stdout.startswith(b'SQLite format 3\0'), result.stdout[:200]
        if result.returncode == 0 and not result.stdout.startswith(b'cat:'):
            Path(str(database) + suffix).write_bytes(result.stdout)
    connection = sqlite3.connect(database)
    version = connection.execute('PRAGMA user_version').fetchone()[0]
    tables = sorted(row[0] for row in connection.execute("SELECT name FROM sqlite_master WHERE type='table'")
                    if not row[0].startswith('sqlite_') and row[0] not in ('room_master_table', 'android_metadata'))
    assert version == 6
    assert len(tables) == 24
    assert all(table in tables for table in ('recipes', 'recipes_translations', 'recipe_materials',
               'recipe_materials_chapters', 'recipe_materials_kanji', 'recipe_materials_words', 'recipe_materials_tags'))
    assert not any(table == 'trainings' or table.startswith('training_') for table in tables)
    views = connection.execute("SELECT name FROM sqlite_master WHERE type='view'").fetchall()
    assert views == []
    assert connection.execute('PRAGMA quick_check').fetchone()[0] == 'ok'
    assert connection.execute('PRAGMA foreign_key_check').fetchall() == []
    recipe_count = connection.execute('SELECT COUNT(*) FROM recipes').fetchone()[0]
    source_count = connection.execute('SELECT COUNT(*) FROM sources').fetchone()[0]
    assert recipe_count == 0
    assert source_count > 0
    report = dict(installation=f'fresh on isolated {args.serial}; phone untouched', database_file='kanji.db',
                  database_version=version, application_tables=tables, recipes=recipe_count, sources=source_count,
                  quick_check='ok', foreign_key_check=[], compatibility_views=[],
                  apk_sha256=hashlib.sha256(Path('app/build/outputs/apk/debug/app-debug.apk').read_bytes()).hexdigest())
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    print(f'OK: fresh Room v{version}, {len(tables)} canonical tables, {source_count} sources, no recipes/FK violations')
