"""Capture the production catalog compositions through debug-only review entry points."""
import argparse
import hashlib
import json
import subprocess
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
REGISTRY = ROOT / 'doc/design-system/theme/screens.json'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--serial', default='emulator-5554')
    parser.add_argument('--orca', default='orca')
    parser.add_argument('--adb', default=str(Path.home() / 'Library/Android/sdk/platform-tools/adb'))
    parser.add_argument('--supplemental', action='store_true')
    parser.add_argument('--only', nargs='+', help='Recapture named cases and preserve the remaining manifest entries.')
    args = parser.parse_args()
    output = Path(__file__).parent / 'native'
    output.mkdir(exist_ok=True)

    def shell(command):
        result = subprocess.run([args.orca, 'emulator', 'exec', '--device', args.serial,
                                 '--command', command, '--json'], capture_output=True, text=True, check=True)
        receipt = json.loads(result.stdout)
        if not receipt.get('ok'):
            raise RuntimeError(receipt)
        return receipt['result']

    def adb(*parts, **kwargs):
        return subprocess.run([args.adb, '-s', args.serial, *parts], check=True, **kwargs)

    original_size = shell('wm size')
    original_override = original_size.split('Override size:')[-1].strip() if 'Override size:' in original_size else None
    cases = [(f'cat-{index:02}', index, '') for index in range(1, 18)]
    if args.supplemental:
        cases += [('tobira-chapters', 2, 'tobira-chapters'), ('tobira-words', 4, 'tobira-words'),
                  ('word-note', 7, 'word-note'), ('catalog-loading', 1, 'catalog-loading'),
                  ('catalog-error', 1, 'catalog-error'), ('selected-filters', 13, 'selected-filters'),
                  ('selected-strokes', 16, 'selected-strokes'), ('empty-kanji', 13, 'empty-kanji'),
                  ('empty-words', 4, 'empty-words'), ('empty-chapters', 2, 'empty-chapters'),
                  ('large-kanji', 8, 'large-kanji'), ('large-radical', 9, 'large-radical'),
                  ('large-word', 7, 'large-word'), ('large-filters', 13, 'large-filters'),
                  ('english', 13, ''), ('fallback', 13, '')]
    if args.only:
        cases = [case for case in cases if case[0] in args.only]
    apk = ROOT / 'app/build/outputs/apk/debug/app-debug.apk'
    apk_hash = hashlib.sha256(apk.read_bytes()).hexdigest()
    captures = []
    try:
        for theme in ['light', 'dark']:
            for name, scenario, variant in cases:
                narrow = name.startswith('large-')
                shell('wm size 945x2400' if narrow else 'wm size reset')
                language = 'en' if name == 'english' else 'de' if name == 'fallback' else 'ru'
                command = (f'am start -f 0x10008000 -n ru.mobui.kanji/.CatalogReviewActivity '
                           f'--ei scenario {scenario} --es language {language} --ez dark {str(theme == "dark").lower()} '
                           f'--ef scale {1.6 if narrow else 1.0}')
                if variant:
                    command += f' --es variant {variant}'
                shell(command)
                ready = False
                for attempt in range(20):
                    adb('shell', 'rm', '-f', '/sdcard/catalog-review.xml', stdout=subprocess.DEVNULL)
                    dump = subprocess.run([args.adb, '-s', args.serial, 'shell', 'uiautomator', 'dump',
                                           '/sdcard/catalog-review.xml'], capture_output=True, text=True)
                    if 'dumped' in dump.stdout:
                        xml = subprocess.check_output([args.adb, '-s', args.serial, 'shell', 'cat',
                                                       '/sdcard/catalog-review.xml'], text=True)
                        if 'catalog-review-error' in xml:
                            raise RuntimeError(f'Catalog failed to load: {name}')
                        if f'catalog-review-ready-{scenario}' in xml:
                            ready = True
                            break
                    time.sleep(.5)
                if not ready:
                    raise RuntimeError(f'{name} did not reach Ready')
                if name.startswith('empty-'):
                    shell('input keyevent KEYCODE_BACK')  # Hide native IME for comparison to the web reference.
                time.sleep(1)
                file = output / f'{name}-{theme}.png'
                with file.open('wb') as stream:
                    adb('exec-out', 'screencap', '-p', stdout=stream)
                captures.append({'id': name, 'file': file.name, 'theme': theme, 'language': language,
                                 'scale': 1.6 if narrow else 1, 'apk_sha256': apk_hash, 'sha256': hashlib.sha256(file.read_bytes()).hexdigest()})
                print(f'captured {name} {theme}', flush=True)
    finally:
        shell(f'wm size {original_override}' if original_override else 'wm size reset')
    manifest_file = output.parent / 'manifest.json'
    if args.only and manifest_file.exists():
        previous = json.loads(manifest_file.read_text()).get('captures', [])
        replaced = {(item['id'], item['theme']) for item in captures}
        captures = [item for item in previous if (item['id'], item['theme']) not in replaced] + captures
    manifest = {'apk_sha256': apk_hash,
                'screen_registry_sha256': hashlib.sha256(REGISTRY.read_bytes()).hexdigest(),
                'device': args.serial, 'captures': captures}
    (output.parent / 'manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n')


if __name__ == '__main__':
    main()
