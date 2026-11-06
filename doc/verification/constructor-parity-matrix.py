#!/usr/bin/env python3
"""Capture constructor states on a dedicated emulator, without clearing app data.

Requires a disposable recipe named PARITY_MATRIX_20261105 with a Book material.
Does not save recipe drafts or delete recipes. --tags-only may create exactly one
PARITY_TAG_20261105 fixture (recreated for empty/populated states); remove it
through the UI after verification.
Restores platform settings. Commits manifest entries only after complete configurations.
"""
import argparse
import json
import pathlib
import re
import subprocess
import time
import xml.etree.ElementTree as ET

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--adb', default='adb')
parser.add_argument('--serial', required=True)
parser.add_argument('--output', type=pathlib.Path, required=True)
parser.add_argument('--configuration', help='For example ru-dark-1.0; otherwise all eight')
parser.add_argument('--resume', action='store_true')
parser.add_argument('--tags-only', action='store_true', help='Capture existing-tag edit/delete dialogs; creates one named test tag if absent')
args = parser.parse_args()
assert args.serial.startswith('emulator-'), 'Only a dedicated emulator is allowed'
adb = [args.adb, '-s', args.serial]
package = 'ru.mobui.kanji'
name = 'PARITY_MATRIX_20261105'


def command(*parts):
    return subprocess.check_output(adb + list(map(str, parts)), stderr=subprocess.STDOUT, timeout=60)


def shell(*parts):
    return command('shell', *parts)


def nodes():
    shell('uiautomator', 'dump', '/sdcard/constructor-parity.xml')
    return list(ET.fromstring(command('exec-out', 'cat', '/sdcard/constructor-parity.xml')).iter('node'))


def labels():
    return [n.get('text') or n.get('content-desc') for n in nodes()]


def tap(label, partial=False):
    candidates = [n for n in nodes() if any(
        (label in value if partial else label == value)
        for value in [n.get('text', ''), n.get('content-desc', '')])]
    assert len(candidates) == 1, (label, len(candidates), labels())
    click(candidates[0])


def click(node):
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', node.get('bounds')))
    assert x2 > x1 and y2 > y1, node.attrib
    shell('input', 'tap', (x1 + x2) // 2, (y1 + y2) // 2)
    time.sleep(.35)


def back():
    shell('input', 'keyevent', '4')
    time.sleep(.35)


def shot(folder, screen):
    assert any(labels()), screen
    (folder / (screen + '.xml')).write_bytes(command('exec-out', 'cat', '/sdcard/constructor-parity.xml'))
    (folder / (screen + '.png')).write_bytes(command('exec-out', 'screencap', '-p'))


def scroll(reverse=False):
    views = [n for n in nodes() if n.get('scrollable') == 'true']
    if not views:
        return  # All controls already fit at this font scale.
    assert len(views) == 1, len(views)
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', views[0].get('bounds')))
    high, low = y1 + (y2 - y1) // 4, y1 + 3 * (y2 - y1) // 4
    shell('input', 'swipe', (x1 + x2) // 2, high if reverse else low,
          (x1 + x2) // 2, low if reverse else high, 350)
    time.sleep(.4)


font = shell('settings', 'get', 'system', 'font_scale').decode().strip()
night = shell('cmd', 'uimode', 'night').decode().strip().split(': ')[-1]
locales = re.search(r'\[(.*?)\]', shell('cmd', 'locale', 'get-app-locales', package, '--user', 0).decode()).group(1)
args.output.mkdir(parents=True, exist_ok=True)
manifest_path = args.output / 'manifest.json'
manifest = json.loads(manifest_path.read_text()) if args.resume and manifest_path.exists() else []
installed_apk = shell('pm', 'path', package).decode().strip().removeprefix('package:')
apk_sha = shell('sha256sum', installed_apk).decode().split()[0]
assert all(entry.get('apk_sha256') == apk_sha for entry in manifest), 'Cannot resume captures from a different APK'
completed = {entry['configuration'] for entry in manifest}
try:
    for language in ['ru', 'en']:
        for mode in ['no', 'yes']:
            for scale in ['1.0', '1.5']:
                key = f'{language}-{"dark" if mode == "yes" else "light"}-{scale}'
                if key in completed or (args.configuration and key != args.configuration):
                    continue
                def text(ru, en):
                    return ru if language == 'ru' else en

                shell('am', 'force-stop', package)
                shell('cmd', 'locale', 'set-app-locales', package, '--user', 0, '--locales', language)
                shell('cmd', 'uimode', 'night', mode)
                shell('settings', 'put', 'system', 'font_scale', scale)
                shell('am', 'start', '-n', package + '/.MainActivity')
                # Installation/seed loading can exceed a fixed startup delay.
                deadline = time.monotonic() + 35
                while text('Выбрать', 'Choose') not in labels():
                    assert time.monotonic() < deadline, 'Home did not finish loading'
                folder = args.output / key
                folder.mkdir(exist_ok=True)
                tap(text('Выбрать', 'Choose'))
                shot(folder, 'library')
                actions = text(f'Действия с тренировкой «{name}»', f'Actions for training “{name}”')
                if args.tags_only:
                    tap(actions)
                    tap(text('Изменить тренировку', 'Edit training'))
                    tap(text('Добавить материал', 'Add material'))
                    tap(text('Из словаря', 'From a dictionary'))
                    tap(text('Теги', 'Tags'))
                    tap(text('Ещё действия', 'More actions'))
                    tap(text('Управление тегами', 'Manage tags'))
                    tag_name = 'PARITY_TAG_20261105'
                    # Only our isolated, unused tag is recreated to cover empty and populated lists.
                    if tag_name in labels():
                        tap(text(f'Удалить тег: {tag_name}', f'Delete tag: {tag_name}'))
                        tap(text('Удалить', 'Delete'))
                    assert tag_name not in labels()
                    shot(folder, 'tag-management-empty')
                    back()
                    shot(folder, 'tags-empty')
                    tap(text('Ещё действия', 'More actions'))
                    tap(text('Управление тегами', 'Manage tags'))
                    tap(text('Создать тег', 'Create tag'))
                    shot(folder, 'create-tag-dialog')
                    click(next(n for n in nodes() if n.get('class') == 'android.widget.EditText'))
                    shell('input', 'text', tag_name)
                    back()
                    # Dialog title and action share their label; the action follows the title.
                    action = text('Создать тег', 'Create tag')
                    click([n for n in nodes() if n.get('text') == action or n.get('content-desc') == action][-1])
                    shot(folder, 'tag-management-nonempty')
                    tap(tag_name)
                    shot(folder, 'edit-tag-dialog')
                    tap(text('Отмена', 'Cancel'))
                    tap(text(f'Удалить тег: {tag_name}', f'Delete tag: {tag_name}'))
                    shot(folder, 'delete-tag-dialog')
                    tap(text('Отмена', 'Cancel'))
                    back()
                    shot(folder, 'tags-nonempty')
                    tap(tag_name)
                    shot(folder, 'tags-selected')
                    screens = sorted(p.stem for p in folder.glob('*.png'))
                    manifest.append({'configuration': key, 'apk_sha256': apk_sha, 'screens': screens})
                    manifest_path.write_text(json.dumps(manifest, indent=2) + '\n')
                    print(key, len(screens), flush=True)
                    continue
                tap(actions)
                tap(text('Удалить', 'Delete'))
                shot(folder, 'delete-dialog')
                tap(text('Отмена', 'Cancel'))
                tap(actions)
                tap(text('Изменить тренировку', 'Edit training'))
                shot(folder, 'editor')
                scroll()
                shot(folder, 'editor-bottom')
                scroll(True)
                tap(text('Добавить материал', 'Add material'))
                shot(folder, 'book')
                tap(text('Главы', 'Chapters'))
                shot(folder, 'chapters')
                back()
                tap(text('Кандзи', 'Kanji'))
                shot(folder, 'book-kanji')
                tap(text('Все черты', 'All stroke counts'))
                shot(folder, 'strokes')
                back()
                tap(text('Все ключи', 'All radicals'))
                shot(folder, 'radicals')
                back()
                tap('JLPT')
                shot(folder, 'jlpt')
                tap(text('Что такое JLPT?', 'What is JLPT?'))
                shot(folder, 'jlpt-help')
                tap(text('Закрыть', 'Close'))
                back()
                back()
                tap(text('По кандзи', 'By kanji'))
                shot(folder, 'kanji-material')
                tap(text('Кандзи', 'Kanji'))
                shot(folder, 'global-kanji')
                back()
                tap(text('Набор слов', 'Word set'))
                shot(folder, 'words-material')
                tap(text('Выбрать слова', 'Choose words'))
                shot(folder, 'words')
                tap('21世紀')
                shot(folder, 'words-chosen')
                tap(text('Выбрано: 1', 'Selected: 1'))
                shot(folder, 'words-selected')
                tap(text('← Все', '← All'))
                tap(text('Поиск', 'Search'))
                click(next(n for n in nodes() if n.get('class') == 'android.widget.EditText'))
                shell('input', 'text', 'NO_MATCH_20261105')
                back()
                shot(folder, 'words-search-empty')
                back()
                tap(text('Из словаря', 'From a dictionary'))
                shot(folder, 'dictionary-material')
                tap(text('Теги', 'Tags'))
                shot(folder, 'tags')
                tap(text('Ещё действия', 'More actions'))
                tap(text('Управление тегами', 'Manage tags'))
                shot(folder, 'tag-management')
                tap(text('Создать тег', 'Create tag'))
                shot(folder, 'create-tag-dialog')
                tap(text('Отмена', 'Cancel'))
                back()
                back()
                back()
                # Material editing is cancelled; original recipe remains untouched.
                click(next(n for n in nodes() if n.get('class') == 'android.widget.EditText'))
                shell('input', 'text', '_UNSAVED')
                back()  # keyboard
                back()  # leave confirmation
                shot(folder, 'unsaved-dialog')
                tap(text('Сбросить', 'Discard'))
                screens = sorted(p.stem for p in folder.glob('*.png'))
                manifest.append({'configuration': key, 'apk_sha256': apk_sha, 'screens': screens})
                manifest_path.write_text(json.dumps(manifest, indent=2) + '\n')
                print(key, len(screens), flush=True)
finally:
    shell('am', 'force-stop', package)
    shell('settings', 'put', 'system', 'font_scale', font)
    shell('cmd', 'uimode', 'night', night)
    shell('cmd', 'locale', 'set-app-locales', package, '--user', 0, '--locales', locales)
