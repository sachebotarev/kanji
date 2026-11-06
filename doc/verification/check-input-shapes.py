#!/usr/bin/env python3
"""Check the single-source input token and prohibit per-screen native shape overrides."""
import json
import re
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
source = json.loads((ROOT / 'doc/design-system/theme/theme-source.json').read_text())
radius = source['component_shape']['text_field']
xml = ET.parse(ROOT / 'core/designsystem/src/main/res/values/input-shape.xml')
assert xml.find("dimen[@name='kanji_input_corner_radius']").text == f'{radius}dp'
css = (ROOT / 'doc/design-system/prototypes/shared/material-theme.css').read_text()
assert f'--app-input-corner-radius: {radius}px;' in css
exported = json.loads((ROOT / 'doc/design-system/prototypes/shared/material-theme.json').read_text())
assert exported['component_shape'] == source['component_shape']

# Skip comments and string literals before balancing call parentheses; callbacks may contain calls.
ignored = re.compile(r'//[^\n]*|/\*.*?\*/|""".*?"""|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'', re.S)
count = 0
for module in ('app', 'core', 'feature'):
    for file in (ROOT / module).rglob('*.kt'):
        if '/src/main/' not in file.as_posix() or 'build' in file.parts:
            continue
        text = ignored.sub(lambda match: ' ' * len(match[0]), file.read_text())
        for call in re.finditer(r'\b(?:OutlinedTextField|TextField|BasicTextField)\s*\(', text):
            depth = 1
            end = call.end()
            while depth and end < len(text):
                depth += (text[end] == '(') - (text[end] == ')')
                end += 1
            assert depth == 0, f'Unbalanced field call: {file}'
            args = text[call.end():end - 1]
            assert re.search(r'\bshape\s*=\s*MaterialTheme\.inputShape\b', args), f'Local/missing input shape: {file}:{text[:call.start()].count(chr(10)) + 1}'
            count += 1
assert count, 'No native input calls found'
print(f'OK: {count} native input calls; Android, CSS and JSON share the {radius} dp/px theme token')
