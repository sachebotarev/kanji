"""Capture CAT-01/CAT-18/APP-03 without creating or editing records.

Run only on an isolated emulator with the current APK installed:
python3 doc/verification/roots-parity-matrix.py OUTPUT
"""
import json
import pathlib
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

OUTPUT = pathlib.Path(sys.argv[1])
ADB = [str(pathlib.Path.home() / 'Library/Android/sdk/platform-tools/adb'), '-s', 'emulator-5554']
PACKAGE = 'ru.mobui.kanji'


def adb(*args):
    return subprocess.check_output(ADB + list(args), timeout=45)


def nodes():
    adb('shell', 'uiautomator', 'dump', '/sdcard/roots-parity.xml')
    return list(ET.fromstring(adb('exec-out', 'cat', '/sdcard/roots-parity.xml')).iter('node'))


def tap(label):
    for attempt in range(3):
        matches = [n for n in nodes() if n.get('text') == label]
        if len(matches) == 1:
            x1, y1, x2, y2 = map(int, re.findall(r'\d+', matches[0].get('bounds')))
            if x2 > x1 and y2 > y1:
                adb('shell', 'input', 'tap', str((x1 + x2) // 2), str((y1 + y2) // 2))
                time.sleep(.5)
                return
        adb('shell', 'input', 'swipe', '520', '1850', '520', '650', '350')
    raise RuntimeError('Missing unique visible entry: ' + label)


def main():
    assert adb('shell', 'getprop', 'ro.kernel.qemu').strip() == b'1', 'Emulator required'
    original_scale = adb('shell', 'settings', 'get', 'system', 'font_scale').decode().strip()
    original_night = adb('shell', 'cmd', 'uimode', 'night').decode()
    original_locales = adb('shell', 'cmd', 'locale', 'get-app-locales', PACKAGE, '--user', '0').decode()
    original_locales = original_locales.split('[')[-1].split(']')[0]
    manifest_path = OUTPUT / 'manifest.json'
    manifest = json.loads(manifest_path.read_text()) if manifest_path.exists() else []
    apk_path = adb('shell', 'pm', 'path', PACKAGE).decode().strip().split(':', 1)[1]
    apk_sha = adb('shell', 'sha256sum', apk_path).decode().split()[0]
    assert all(item['apk_sha256'] == apk_sha for item in manifest), 'Cannot resume with a different APK'
    completed = {item['configuration'] for item in manifest}
    try:
        for language in ['ru', 'en']:
            for theme in ['light', 'dark']:
                for scale in ['1.0', '1.5']:
                    key = f'{language}-{theme}-{scale}'
                    if key in completed:
                        continue
                    adb('shell', 'settings', 'put', 'system', 'font_scale', scale)
                    adb('shell', 'cmd', 'uimode', 'night', 'yes' if theme == 'dark' else 'no')
                    adb('shell', 'cmd', 'locale', 'set-app-locales', PACKAGE, '--user', '0', '--locales', language)
                    adb('shell', 'am', 'force-stop', PACKAGE)
                    adb('shell', 'am', 'start', '-n', PACKAGE + '/.MainActivity')
                    home = 'Кандзи дэс нэ' if language == 'ru' else 'Kanji Desu Ne'
                    for _ in range(25):
                        if any(n.get('text') == home for n in nodes()):
                            break
                        time.sleep(1)
                    else:
                        raise RuntimeError('Home not ready: ' + key)
                    folder = OUTPUT / key
                    folder.mkdir(parents=True, exist_ok=True)
                    for label, name in zip(
                        ['Учебники', 'Справочники', 'Мой словарь'] if language == 'ru' else ['Textbooks', 'References', 'My vocabulary'],
                        ['CAT-01', 'CAT-18', 'APP-03-empty'],
                    ):
                        tap(label)
                        time.sleep(1)
                        nodes()
                        (folder / (name + '.png')).write_bytes(adb('exec-out', 'screencap', '-p'))
                        (folder / (name + '.xml')).write_bytes(adb('exec-out', 'cat', '/sdcard/roots-parity.xml'))
                        adb('shell', 'input', 'keyevent', '4')
                        time.sleep(.5)
                    manifest.append({'configuration': key, 'apk_sha256': apk_sha, 'screens': ['CAT-01', 'CAT-18', 'APP-03-empty']})
                    (OUTPUT / 'manifest.json').write_text(json.dumps(manifest, indent=2) + '\n')
                    print(key, flush=True)
    finally:
        adb('shell', 'settings', 'put', 'system', 'font_scale', original_scale)
        adb('shell', 'cmd', 'uimode', 'night', 'yes' if 'yes' in original_night else 'no')
        adb('shell', 'cmd', 'locale', 'set-app-locales', PACKAGE, '--user', '0', '--locales', original_locales)
        adb('shell', 'am', 'force-stop', PACKAGE)


if __name__ == '__main__':
    main()
