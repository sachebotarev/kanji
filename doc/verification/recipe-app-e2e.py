#!/usr/bin/env python3
"""Real installed debug app + Room smoke. Requires an otherwise empty recipe library.

Uses Android input/uiautomator/run-as; no mocks, SQL writes or dependency changes.
Existing non-test recipes cause an early abort. UI deletion archives only test recipes.
"""
import argparse
import json
import pathlib
import re
import sqlite3
import subprocess
import tempfile
import time
import xml.etree.ElementTree as ET

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--adb', default='adb')
parser.add_argument('--serial', required=True)
parser.add_argument('--prefix', required=True, help='ASCII prefix for isolated test records')
parser.add_argument('--output', required=True, type=pathlib.Path)
args = parser.parse_args()
adb = [args.adb, '-s', args.serial]
package = 'ru.mobui.kanji'
steps = []


def call(*command):
    return subprocess.check_output(adb + list(command), stderr=subprocess.STDOUT)


def shell(*command):
    return call('shell', *map(str, command))


def nodes():
    shell('uiautomator', 'dump', '/sdcard/kanji-recipe-e2e.xml')
    return list(ET.fromstring(call('exec-out', 'cat', '/sdcard/kanji-recipe-e2e.xml')).iter('node'))


def find(label=None, cls=None):
    matches = [n for n in nodes() if
               (label is None or n.get('text') == label or n.get('content-desc') == label) and
               (cls is None or n.get('class') == cls)]
    assert len(matches) == 1, (label, cls, [(n.get('text'), n.get('content-desc')) for n in matches])
    return matches[0]


def center(node):
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', node.get('bounds')))
    return (x1 + x2) // 2, (y1 + y2) // 2


def tap(label):
    shell('input', 'tap', *center(find(label)))
    time.sleep(.5)


def field(text):
    node = find(cls='android.widget.EditText')
    shell('input', 'tap', *center(node))
    old = node.get('text', '')
    if old:
        shell('input', 'keyevent', 'KEYCODE_MOVE_END')
        shell('input', 'keyevent', *(['67'] * len(old)))
    shell('input', 'text', text)
    shell('input', 'keyevent', 'KEYCODE_BACK')
    time.sleep(.4)


def database():
    # Copy an idle DB + WAL snapshot, never query/write application files in place.
    with tempfile.TemporaryDirectory() as temp:
        path = pathlib.Path(temp)
        for suffix in ['', '-wal', '-shm']:
            name = 'kanji.db' + suffix
            try:
                (path / name).write_bytes(call('exec-out', 'run-as', package, 'cat', 'databases/' + name))
            except subprocess.CalledProcessError:
                if not suffix:
                    raise
        connection = sqlite3.connect(path / 'kanji.db')
        connection.row_factory = sqlite3.Row
        assert connection.execute('pragma quick_check').fetchone()[0] == 'ok'
        recipes = [dict(row) for row in connection.execute('''
            SELECT t.*, n.name, n.language_tag FROM recipes t
            JOIN recipes_translations n ON n.recipe_id=t.id ORDER BY t.position,t.id,n.language_tag
        ''')]
        materials = [dict(row) for row in connection.execute('SELECT * FROM recipe_materials ORDER BY recipe_id,position')]
        connection.close()
        return {'recipes': recipes, 'materials': materials}


def record(label):
    value = database()
    steps.append({'step': label, 'database': value})
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps({'package': package, 'serial': args.serial, 'steps': steps},
                                     ensure_ascii=False, indent=2) + '\n')
    print(label, flush=True)
    return value


def active_rows(value):
    return [r for r in value['recipes'] if not r['is_archived']]


def open_library():
    shell('am', 'force-stop', package)
    shell('am', 'start', '-n', package + '/.MainActivity')
    time.sleep(2)
    tap('Открыть рецепты тренировок')


def open_detail(name):
    tap('Открыть рецепт «' + name + '»')


def create(name):
    tap('Создать')
    field(name)
    tap('Добавить материал')
    tap('По кандзи')
    tap('Добавить')
    tap('Сохранить')
    find(name)


def delete(name):
    open_detail(name)
    tap('Действия с рецептом «' + name + '»')
    tap('Удалить')
    # Dialog has a single confirm action bearing the same label.
    tap('Удалить')


assert re.fullmatch(r'[A-Za-z0-9_]+', args.prefix), 'Use an ASCII record prefix'
a_name, b_name = args.prefix + '_A', args.prefix + '_B'
open_library()
baseline = record('baseline')
existing = active_rows(baseline)
assert all(r['name'] == a_name for r in existing) and len(existing) <= 1, 'Non-test recipes present; abort without changing them'
if not existing:
    create(a_name)
a = active_rows(record('create A'))[0]
assert a['name'] == a_name and a['language_tag'] == 'ru'
a_id = a['id']
create(b_name)
created = record('create B')
assert len(active_rows(created)) == 2
b_id = next(r['id'] for r in active_rows(created) if r['name'] == b_name)
assert len({m['id'] for m in created['materials']}) == len(created['materials'])
assert all(m['material_type'] == 'kanji' for m in created['materials'])
# Body opens detail; permanent radio selection is gone, so is_active is unchanged.
previous_active = [r['id'] for r in active_rows(created) if r['is_active']]
open_detail(b_name)
assert [r['id'] for r in active_rows(database()) if r['is_active']] == previous_active
shell('input', 'keyevent', 'KEYCODE_BACK')
time.sleep(.4)
# Editing returns through the actual app graph and keeps UUID/material identity.
open_detail(a_name)
tap('Изменить')
a_name += '_edited'
field(a_name)
tap('Сохранить')
edited = record('edit A')
a_after = next(r for r in active_rows(edited) if r['id'] == a_id)
assert a_after['name'] == a_name and a_after['revision'] > a['revision']
assert edited['materials'] == created['materials']
shell('input', 'keyevent', 'KEYCODE_BACK')
time.sleep(.4)
active_before_drag = [r['id'] for r in active_rows(edited) if r['is_active']]
# Android input's built-in draganddrop produces the real platform drag session.
start, end = center(find(b_name)), center(find(a_name))
shell('input', 'touchscreen', 'draganddrop', *start, *end, '1200')
time.sleep(1)
reordered = record('native drag B before A')
assert [r['id'] for r in active_rows(reordered)] == [b_id, a_id]
assert [r['id'] for r in active_rows(reordered) if r['is_active']] == active_before_drag
# Actual process stop/relaunch, not ActivityScenario recreation or mock restoration.
open_library()
restarted = record('force-stop and relaunch')
assert restarted == reordered
find(a_name)
find(b_name)
delete(b_name)
deleted = record('delete B')
assert [r['id'] for r in active_rows(deleted)] == [a_id]
delete(a_name)
cleaned = record('delete A and cleanup')
assert not active_rows(cleaned)
assert all(r['is_archived'] and not r['is_active'] for r in cleaned['recipes'])
open_library()
assert not active_rows(record('cleanup survives process restart'))
print('PASS: real app UI, Room, native drag and process restart', flush=True)
