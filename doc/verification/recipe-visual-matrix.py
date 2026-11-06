#!/usr/bin/env python3
"""Capture six real app views for RU/EN x day/night x font 1.0/1.5.

Use only a dedicated emulator with the two named visual-test recipes already saved.
No application data is deleted; previous font/night/app-locale settings are restored.
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
parser.add_argument('--name', default='Sample training for kanji and complete phrases')
parser.add_argument('--resume', action='store_true', help='Skip configurations already in a completed manifest')
args = parser.parse_args()
adb = [args.adb, '-s', args.serial]
package = 'ru.mobui.kanji'


def command(*parts):
    return subprocess.check_output(adb + list(map(str, parts)), stderr=subprocess.STDOUT)


def shell(*parts):
    return command('shell', *parts)


def nodes():
    shell('uiautomator', 'dump', '/sdcard/kanji-matrix.xml')
    return list(ET.fromstring(command('exec-out', 'cat', '/sdcard/kanji-matrix.xml')).iter('node'))


def tap(label):
    matches = [n for n in nodes() if n.get('text') == label or n.get('content-desc') == label]
    assert len(matches) == 1, (label, len(matches))
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', matches[0].get('bounds')))
    shell('input', 'tap', (x1 + x2) // 2, (y1 + y2) // 2)
    time.sleep(.6)


def scroll_editor(reverse=False):
    views = [n for n in nodes() if n.get('scrollable') == 'true']
    assert len(views) == 1
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', views[0].get('bounds')))
    upper, lower = y1 + (y2 - y1) // 4, y1 + 3 * (y2 - y1) // 4
    shell('input', 'swipe', (x1 + x2) // 2, upper if reverse else lower,
          (x1 + x2) // 2, lower if reverse else upper, '400')
    time.sleep(.5)


def capture(folder, screen):
    tree = nodes()
    # Preserve the original platform hierarchy, including bounds and localized labels.
    (folder / (screen + '.xml')).write_bytes(command('exec-out', 'cat', '/sdcard/kanji-matrix.xml'))
    (folder / (screen + '.png')).write_bytes(command('exec-out', 'screencap', '-p'))
    labels = [n.get('text') or n.get('content-desc') for n in tree]
    assert any(labels)
    if screen == 'jlpt':
        assert all(f'N{level}' in labels for level in range(1, 6))
    if screen == 'editor-bottom':
        assert 'Limit time' in labels or 'Ограничить время' in labels


font = shell('settings', 'get', 'system', 'font_scale').decode().strip()
night = shell('cmd', 'uimode', 'night').decode().strip().split(': ')[-1]
locale_text = shell('cmd', 'locale', 'get-app-locales', package, '--user', '0').decode()
locales = re.search(r'\[(.*?)\]', locale_text).group(1)
args.output.mkdir(parents=True, exist_ok=True)
manifest_path = args.output / 'manifest.json'
manifest = json.loads(manifest_path.read_text()) if args.resume and manifest_path.exists() else []
completed = {entry['configuration'] for entry in manifest}
try:
    for language in ['ru', 'en']:
        for mode in ['no', 'yes']:
            for scale in ['1.0', '1.5']:
                key = f'{language}-{"dark" if mode == "yes" else "light"}-{scale}'
                if key in completed:
                    continue
                shell('am', 'force-stop', package)
                shell('cmd', 'locale', 'set-app-locales', package, '--user', '0', '--locales', language)
                shell('cmd', 'uimode', 'night', mode)
                shell('settings', 'put', 'system', 'font_scale', scale)
                shell('am', 'start', '-n', package + '/.MainActivity')
                time.sleep(3)
                folder = args.output / key
                folder.mkdir(exist_ok=True)
                tap('Выбрать' if language == 'ru' else 'Choose')
                capture(folder, 'library')
                tap(f'Действия с тренировкой «{args.name}»' if language == 'ru' else f'Actions for training “{args.name}”')
                tap('Изменить тренировку' if language == 'ru' else 'Edit training')
                capture(folder, 'editor')
                scroll_editor()
                capture(folder, 'editor-bottom')
                scroll_editor(reverse=True)
                tap('По кандзи' if language == 'ru' else 'By kanji')
                capture(folder, 'material')
                tap('Кандзи' if language == 'ru' else 'Kanji')
                capture(folder, 'kanji')
                tap('JLPT')
                capture(folder, 'jlpt')
                manifest.append({'configuration': key, 'screens': ['library', 'editor', 'editor-bottom', 'material', 'kanji', 'jlpt']})
                (args.output / 'manifest.json').write_text(json.dumps(manifest, indent=2) + '\n')
                print(key, flush=True)
finally:
    shell('am', 'force-stop', package)
    shell('settings', 'put', 'system', 'font_scale', font)
    shell('cmd', 'uimode', 'night', night)
    shell('cmd', 'locale', 'set-app-locales', package, '--user', '0', '--locales', locales)
