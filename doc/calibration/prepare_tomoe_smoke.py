"""Prepare a fixed, small Tomoe probe; never select samples using checker scores."""
from pathlib import Path
import hashlib
import json
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parent
source = root / 'external/tomoe/handwriting-ja.xml'
entries = {}
for index, character in enumerate(ET.parse(source).getroot().findall('character')):
    glyph = character.findtext('utf8')
    strokes = [[(int(p.attrib['x']), int(p.attrib['y'])) for p in s.findall('point')]
               for s in character.findall('./strokes/stroke')]
    if glyph not in entries and strokes and all(strokes):
        entries[glyph] = (index, strokes)

# Chosen before evaluation: basic forms, confusable pairs, and two complex signs.
positive = '日月山川水火木本土士未末牛午学脳'
pairs = [('土', '士'), ('未', '末'), ('牛', '午'), ('木', '本'), ('日', '月'), ('山', '川')]
cases = [(g, g, 'positive') for g in positive]
for a, b in pairs:
    cases.extend([(a, b, 'negative'), (b, a, 'negative')])
manifest = []
with (root / 'tomoe-smoke-inputs.tsv').open('w') as out:
    for glyph, target, label in cases:
        index, strokes = entries[glyph]
        points = [p for s in strokes for p in s]
        assert all(0 <= x <= 1000 and 0 <= y <= 1000 for x, y in points)
        identifier = f'tomoe-{ord(glyph):04x}-entry{index}-to-{ord(target):04x}'
        geometry = ';'.join(' '.join(f'{x / 1000:.3f},{y / 1000:.3f}' for x, y in s) for s in strokes)
        out.write(f'{identifier}\t{ord(target):04x}\t{label}\tsmoke\t{geometry}\n')
        manifest.append(dict(id=identifier, input=glyph, target=target, label=label,
                             source_entry_index=index, strokes=len(strokes), points=len(points)))
(root / 'tomoe-smoke-manifest.json').write_text(json.dumps(dict(
    source='external/tomoe/handwriting-ja.xml',
    source_sha256=hashlib.sha256(source.read_bytes()).hexdigest(),
    license='external/tomoe/COPYING',
    selection='First nonempty dictionary entry for each selected character; selected before scoring.',
    normalization='Divide original x/y coordinates by 1000; no bounding-box alignment or augmentation.',
    cases=manifest,
), ensure_ascii=False, indent=2) + '\n')
print(f'Prepared {len(manifest)} cases ({len(positive)} positive, {len(cases)-len(positive)} negative).')
