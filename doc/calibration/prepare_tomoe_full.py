"""Export every Tomoe entry with an available local reference, without score-based filtering."""
from pathlib import Path
from collections import Counter
import hashlib
import json
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parent
source = root / 'external/tomoe/handwriting-ja.xml'
references = {}
with (root / 'external/references.tsv').open() as f:
    for line in f:
        key, _, rest = line.partition('\t')
        references[int(key, 16)] = rest.count(';') + 1
counts = Counter()
excluded = []
with (root / 'tomoe-full-inputs.tsv').open('w') as out:
    for index, character in enumerate(ET.parse(source).getroot().findall('character')):
        counts['total_entries'] += 1
        glyph = character.findtext('utf8', '')
        is_kanji = len(glyph) == 1 and any(a <= ord(glyph) <= b for a, b in [
            (0x3400, 0x4dbf), (0x4e00, 0x9fff), (0xf900, 0xfaff), (0x20000, 0x323af)])
        if is_kanji:
            counts['kanji_entries'] += 1
        strokes = [[(int(p.attrib['x']), int(p.attrib['y'])) for p in s.findall('point')]
                   for s in character.findall('./strokes/stroke')]
        reason = None
        if len(glyph) != 1 or ord(glyph) not in references:
            reason = 'no_reference'
        elif not strokes or not all(strokes):
            reason = 'empty_strokes'
        elif not all(0 <= x <= 1000 and 0 <= y <= 1000 for s in strokes for x, y in s):
            reason = 'coordinates_out_of_range'
        if reason:
            counts[reason] += 1
            excluded.append(dict(index=index, glyph=glyph, reason=reason))
            continue
        identifier = f'tomoe-{ord(glyph):04x}-entry{index}'
        geometry = ';'.join(' '.join(f'{x / 1000:.3f},{y / 1000:.3f}' for x, y in s) for s in strokes)
        out.write(f'{identifier}\t{ord(glyph):04x}\tpositive\tfull\t{geometry}\n')
        counts['exported_entries'] += 1
        category = 'count_compatible' if len(strokes) == references[ord(glyph)] else 'count_mismatch'
        counts[category] += 1
        if is_kanji:
            counts['kanji_exported_entries'] += 1
            counts['kanji_' + category] += 1
manifest = dict(source='external/tomoe/handwriting-ja.xml',
    source_sha256=hashlib.sha256(source.read_bytes()).hexdigest(),
    license='external/tomoe/COPYING',
    selection='Every source entry, including repeated characters and stroke-count variants; exclusions listed below.',
    normalization='Original x/y divided by 1000; no centering, resizing or augmentation.',
    counts=dict(counts), excluded=excluded)
(root / 'tomoe-full-manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2)+'\n')
print(json.dumps(dict(counts),ensure_ascii=False))
