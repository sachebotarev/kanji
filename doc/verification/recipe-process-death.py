#!/usr/bin/env python3
"""English UI: restore an unsaved recipe after Android kills its background process.

Run on an isolated emulator. Does not force-stop, clear data or write SQL.
Starts only from the dashboard and discards only the draft created by this script.
"""
import argparse
import hashlib
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
parser.add_argument('--output', type=pathlib.Path, required=True)
args = parser.parse_args()
adb = [args.adb, '-s', args.serial]
package = 'ru.mobui.kanji'
name = 'PROCESS_DRAFT_20261105'


def command(*parts):
    return subprocess.check_output(adb + list(map(str, parts)), stderr=subprocess.STDOUT)


def shell(*parts):
    return command('shell', *parts)


def nodes():
    shell('uiautomator', 'dump', '/sdcard/kanji-process-death.xml')
    return list(ET.fromstring(command('exec-out', 'cat', '/sdcard/kanji-process-death.xml')).iter('node'))


def find(label):
    matches = [n for n in nodes() if n.get('text') == label or n.get('content-desc') == label]
    assert len(matches) == 1, (label, len(matches))
    return matches[0]


def tap_node(node):
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', node.get('bounds')))
    shell('input', 'tap', (x1 + x2) // 2, (y1 + y2) // 2)
    time.sleep(.5)


def tap(label):
    tap_node(find(label))


def enter(text):
    fields = [n for n in nodes() if n.get('class') == 'android.widget.EditText' and not n.get('text')]
    assert len(fields) == 1, fields
    tap_node(fields[0])
    shell('input', 'text', text)
    shell('input', 'keyevent', 'KEYCODE_BACK')
    time.sleep(.5)


def recipe_digest():
    with tempfile.TemporaryDirectory() as folder:
        path = pathlib.Path(folder)
        for suffix in ['', '-wal', '-shm']:
            try:
                data = command('exec-out', 'run-as', package, 'cat', 'databases/kanji.db' + suffix)
                (path / ('kanji.db' + suffix)).write_bytes(data)
            except subprocess.CalledProcessError:
                if not suffix:
                    raise
        db = sqlite3.connect(path / 'kanji.db')
        assert db.execute('pragma quick_check').fetchone()[0] == 'ok'
        rows = {}
        tables = ['recipes', 'recipes_translations', 'recipe_materials',
                  'recipe_materials_chapters', 'recipe_materials_kanji', 'recipe_materials_words', 'recipe_materials_tags']
        for table in tables:
            rows[table] = sorted(db.execute('SELECT * FROM ' + table).fetchall(), key=repr)
        db.close()
        return hashlib.sha256(json.dumps(rows, sort_keys=True).encode()).hexdigest()


# Do not navigate away from an unrelated existing editor or kill its process.
find('Current training')
baseline = recipe_digest()
tap('Choose')
tap('+ Create')
enter(name)
tap('Add material')
tap('By kanji')
tap('Add')
tap('Set a number')
enter('17')
find(name)
find('By kanji')
find('17')
assert recipe_digest() == baseline, 'Draft wrote to Room before Save'
old_pid = shell('pidof', package).decode().strip()
shell('input', 'keyevent', 'KEYCODE_HOME')
time.sleep(3)
shell('am', 'kill', '--user', 'current', package)
time.sleep(2)
try:
    alive = shell('pidof', package).decode().strip()
except subprocess.CalledProcessError:
    alive = ''
assert not alive, 'Background process was not killed; restoration is not verified'
# Return through Android Recents: starting a new MAIN intent can create another
# standard-launchMode Activity instead of restoring the retained task.
shell('input', 'keyevent', 'KEYCODE_APP_SWITCH')
time.sleep(1)
snapshots = [n for n in nodes() if n.get('content-desc') == 'Kanji Desu Ne']
assert len(snapshots) == 1, 'Select the retained app task, not a new launcher instance'
tap_node(snapshots[0])
deadline = time.monotonic() + 25
while True:
    try:
        find(name)
        break
    except AssertionError:
        if time.monotonic() >= deadline:
            raise
        time.sleep(1)
new_pid = shell('pidof', package).decode().strip()
assert new_pid and new_pid != old_pid
find('By kanji')
find('17')
assert recipe_digest() == baseline, 'Restoration published the draft'
shell('input', 'keyevent', 'KEYCODE_BACK')
find('Save changes?')
tap('Discard')
find('+ Create')
assert recipe_digest() == baseline, 'Discard changed persisted recipes'
result = {'status': 'PASS', 'serial': args.serial, 'locale': 'en', 'old_pid': old_pid,
          'new_pid': new_pid, 'draft_name': name, 'material': 'kanji, all', 'task_count': 17,
          'recipe_tables_sha256_before_and_after': baseline, 'cleanup': 'discarded without saving',
          'method': 'HOME, am kill --user current, select retained task from Android Recents'}
args.output.parent.mkdir(parents=True, exist_ok=True)
args.output.write_text(json.dumps(result, indent=2) + '\n')
print(json.dumps(result, indent=2))
