"""Offline, reproducible catalog preparation. No Android/user database is modified."""
import argparse
import csv
import hashlib
import json
from pathlib import Path
import re
import sqlite3
import tempfile

ROOT = Path(__file__).resolve().parents[3]
HERE = Path(__file__).resolve().parent
SOURCES = [(1, 'Tobira', 'tobira-kanji.csv'), (2, 'Basic Kanji Book', 'basic-kanji-book.csv')]
TABLES = ['catalog_metadata', 'radicals', 'radicals_translations', 'kanji',
          'kanji_translations', 'kanji_readings', 'words', 'words_translations',
          'word_reading_parts', 'sources', 'sources_translations', 'source_kanji', 'source_words']


def encoded(value):
    return (json.dumps(value, ensure_ascii=False, indent=2) + '\n').encode()


def digest(data):
    return hashlib.sha256(data).hexdigest()


def require(condition, message):
    if not condition:
        raise ValueError(message)


def read_csv(path, delimiter=','):
    with path.open(encoding='utf-8-sig', newline='') as stream:
        reader = csv.reader(stream, delimiter=delimiter, strict=True)
        header = next(reader)
        rows = list(reader)
    require(all(len(r) == len(header) for r in rows), f'Invalid columns: {path}')
    return header, rows


def prepare(input_dir, registry, version):
    """Existing IDs are immutable; new IDs are appended, never inferred from row positions."""
    require(version > 0, 'Content version must be positive')
    registry = json.loads(json.dumps(registry))
    require(registry['format_version'] == 1, 'Unsupported registry')
    require(registry['sources'] == [{'id': i, 'title': title} for i, title, _ in SOURCES],
            'Source identities changed: explicit migration required')
    word_map = {}
    word_ids = set()
    for item in registry['words']:
        key = (item['written_form'], item['reading'])
        require(item['id'] > 0 and item['id'] not in word_ids and key not in word_map,
                'Duplicate/invalid word identity in registry')
        word_ids.add(item['id'])
        word_map[key] = item
    link_map = {}
    link_ids = set()
    for item in registry['source_words']:
        key = (item['source_id'], item['word_id'], item['kanji_id'])
        require(item['id'] > 0 and item['id'] not in link_ids and key not in link_map,
                'Duplicate/invalid source word identity')
        require(item['word_id'] in word_ids and item['source_id'] in {1, 2}, 'Orphan registry link')
        link_ids.add(item['id'])
        link_map[key] = item
    tables = {name: [] for name in TABLES}
    tables['catalog_metadata'] = [[1, 1, version]]
    _, radical_rows = read_csv(input_dir / 'radicals.csv')
    require(len(radical_rows) == 214, 'Expected 214 radicals')
    for number, symbol, name, strokes in radical_rows:
        tables['radicals'].append([int(number), symbol, int(strokes)])
        tables['radicals_translations'].append([int(number), 'ru', name])
    require({r[0] for r in tables['radicals']} == set(range(1, 215)), 'Missing radical IDs')
    _, kanji_rows = read_csv(input_dir / 'kanji.csv')
    kanji_ids = set()
    for row in kanji_rows:
        ident, symbol, meanings, on, kun, strokes, radical, *flags = row
        ident = int(ident)
        require(len(symbol) == 1 and ord(symbol) == ident and not 0xD800 <= ident <= 0xDFFF,
                f'Invalid Unicode ID: {symbol}')
        require(ident not in kanji_ids, f'Duplicate kanji: {symbol}')
        kanji_ids.add(ident)
        require(len(flags) == 3 and all(f in ('true', 'false') for f in flags), 'Invalid boolean')
        tables['kanji'].append([ident, symbol, int(strokes), int(radical) if radical else None,
                                *[int(f == 'true') for f in flags]])
        for pos, meaning in enumerate(meanings.split('|'), 1):
            tables['kanji_translations'].append([ident, 'ru', pos, meaning])
        for kind, readings in [('on', on), ('kun', kun)]:
            for pos, reading in enumerate(readings.split('|') if readings else [], 1):
                tables['kanji_readings'].append([ident, kind, pos, reading])
    translations = {}
    placements = {}
    link_keys = set()
    candidates = []
    duplicate_links = 0
    contextual_links = []
    source_counts = []
    for source_id, title, filename in SOURCES:
        header, rows = read_csv(input_dir / filename, '|')
        require(header == ['Слово кандзи', 'Чтение слова', 'Изучаемый кандзи',
                           f'Номер кандзи в {title}', f'Глава в {title}',
                           'Чтение по кандзи', 'Перевод на русский'], f'Unexpected header: {filename}')
        tables['sources'].append([source_id, 'textbook', title, 0])
        source_counts.append({'source_id': source_id, 'input_rows': len(rows)})
        for line, row in enumerate(rows, 2):
            written, reading, symbol, number, chapter_text, parts, translation = row
            require(all(v.strip() for v in [written, reading, symbol, number, chapter_text, translation]),
                    f'Empty required value: {filename}:{line}')
            require(len(symbol) == 1 and ord(symbol) in kanji_ids, f'Unknown kanji: {filename}:{line}')
            if symbol not in written:
                contextual_links.append({'source_id': source_id, 'line': line, 'written_form': written, 'kanji': symbol})
            match = re.fullmatch(re.escape(title) + r' (?:I|II), глава (\d+)', chapter_text)
            require(match is not None, f'Invalid chapter: {chapter_text}')
            placement = [source_id, ord(symbol), int(number), int(match[1])]
            key = (source_id, ord(symbol))
            require(key not in placements or placements[key] == placement, f'Conflicting placement: {key}')
            placements[key] = placement
            pair = (written, reading)
            require(pair not in translations or translations[pair] == translation,
                    f'Conflicting translation: {pair}')
            translations[pair] = translation
            # Assign words after reading all input: only the initial allocation uses sorted pairs.
            link = (source_id, pair, ord(symbol))
            if link in link_keys:
                duplicate_links += 1
            link_keys.add(link)
            if parts:
                require(''.join(parts.split('|')) == reading, f'Reading parts mismatch: {filename}:{line}')
                candidates.append({'source_id': source_id, 'line': line, 'written_form': written,
                                   'reading': reading, 'raw_reading_parts': parts})
    next_word_id = max(word_ids, default=0) + 1
    for written, reading in sorted(translations):
        if (written, reading) not in word_map:
            item = {'id': next_word_id, 'written_form': written, 'reading': reading}
            next_word_id += 1
            registry['words'].append(item)
            word_map[(written, reading)] = item
    for pair, item in sorted(word_map.items(), key=lambda x: x[1]['id']):
        active = pair in translations
        tables['words'].append([item['id'], *pair, int(not active)])
        if active:
            tables['words_translations'].append([item['id'], 'ru', translations[pair]])
    next_link_id = max(link_ids, default=0) + 1
    active_links = sorted((sid, word_map[pair]['id'], kid) for sid, pair, kid in link_keys)
    for key in active_links:
        if key not in link_map:
            item = dict(zip(['source_id', 'word_id', 'kanji_id'], key), id=next_link_id)
            next_link_id += 1
            registry['source_words'].append(item)
            link_map[key] = item
        tables['source_words'].append([link_map[key]['id'], *key])
    tables['source_kanji'] = list(placements.values())
    for name in TABLES:
        tables[name].sort(key=lambda r: tuple((v is not None, v) for v in r))
    registry['words'].sort(key=lambda x: x['id'])
    registry['source_words'].sort(key=lambda x: x['id'])
    candidates.sort(key=lambda r: (r['source_id'], r['written_form'], r['reading'], r['line']))
    # No guesses from kana-only CSV fragments to written parts. Whole reading remains available.
    report = {'content_version': version, 'counts': {k: len(v) for k, v in tables.items()},
              'sources': source_counts, 'duplicate_input_links_collapsed': duplicate_links,
              'contextual_links_without_literal_kanji': contextual_links,
              'reading_parts_policy': 'deferred: no verified written_part mapping in CSV',
              'reading_parts_candidates': len(candidates),
              'archived_words': sum(r[3] for r in tables['words'])}
    return registry, tables, candidates, report


def make_database(path, tables):
    con = sqlite3.connect(path)
    try:
        con.executescript((HERE / 'schema.sql').read_text())
        with con:
            for name in TABLES:
                rows = tables[name]
                if rows:
                    con.executemany(f'INSERT INTO {name} VALUES ({",".join("?" for _ in rows[0])})', rows)
            con.execute('PRAGMA user_version=1')
            require(not con.execute('PRAGMA foreign_key_check').fetchall(), 'Foreign key violations')
            require(con.execute('PRAGMA integrity_check').fetchall() == [('ok',)], 'Integrity check failed')
        con.execute('VACUUM')
    finally:
        con.close()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--content-version', type=int)
    parser.add_argument('--initialize-registry', action='store_true')
    parser.add_argument('--update-registry', action='store_true')
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    current_manifest = ROOT / 'data/catalog/seed/manifest.json'
    if args.content_version is None:
        args.content_version = json.loads(current_manifest.read_text())['content_version'] if current_manifest.exists() else 1
    registry_path = HERE / 'identity-registry.json'
    if args.initialize_registry:
        require(not registry_path.exists(), 'Registry already exists; initialization forbidden')
        registry = {'format_version': 1, 'sources': [{'id': i, 'title': t} for i, t, _ in SOURCES],
                    'words': [], 'source_words': []}
    else:
        registry = json.loads(registry_path.read_text())
    updated, tables, candidates, report = prepare(ROOT / 'data/catalog', registry, args.content_version)
    require(updated == registry or args.initialize_registry or args.update_registry,
            'New identities need review: use --update-registry (never regenerate the registry)')
    require(not args.check or updated == registry, '--check cannot modify the identity registry')
    # The checked-in package is immutable per content version.
    output_dir = ROOT / 'data/catalog/seed'
    with tempfile.TemporaryDirectory() as temp:
        temp = Path(temp)
        database_path = temp / 'catalog.sqlite'
        make_database(database_path, tables)
        con = sqlite3.connect(database_path)
        columns = {name: [r[1] for r in con.execute(f'PRAGMA table_info({name})')] for name in TABLES}
        con.close()
        payload = {'format_version': 1, 'schema_version': 1, 'content_version': args.content_version,
                   'tables': {name: {'columns': columns[name], 'rows': tables[name]} for name in TABLES}}
        files = {'catalog.sqlite': database_path.read_bytes(), 'catalog.json': encoded(payload)}
        inputs = {name: digest((ROOT / 'data/catalog' / name).read_bytes())
                  for name in ['radicals.csv', 'kanji.csv', *[s[2] for s in SOURCES]]}
        manifest = {'format_version': 1, 'schema_version': 1, 'content_version': args.content_version,
                    'identity_registry_sha256': digest(encoded(updated)),
                    'schema_sha256': digest((HERE / 'schema.sql').read_bytes()),
                    'inputs_sha256': inputs,
                    'files': {name: {'sha256': digest(data), 'bytes': len(data)} for name, data in files.items()},
                    'counts': report['counts']}
        files['manifest.json'] = encoded(manifest)
        docs = {'validation.json': encoded(report), 'reading-parts-candidates.json': encoded(candidates)}
        if args.check:
            for name, data in files.items():
                target = output_dir / name
                if name == 'catalog.sqlite':
                    # SQLite physical pages may differ across SQLite versions; compare schema and all rows.
                    check = sqlite3.connect(f'{target.as_uri()}?mode=ro', uri=True)
                    require(check.execute('PRAGMA integrity_check').fetchall() == [('ok',)], 'Invalid SQLite')
                    require(not check.execute('PRAGMA foreign_key_check').fetchall(), 'Invalid foreign keys')
                    for table in TABLES:
                        actual = sorted(check.execute(f'SELECT * FROM {table}').fetchall())
                        require(actual == sorted(map(tuple, tables[table])), f'Database drift: {table}')
                    expected_schema = sqlite3.connect(database_path)
                    require(check.execute('SELECT type,name,sql FROM sqlite_master ORDER BY type,name').fetchall()
                            == expected_schema.execute('SELECT type,name,sql FROM sqlite_master ORDER BY type,name').fetchall(),
                            'SQLite schema drift')
                    expected_schema.close()
                    check.close()
                elif name == 'manifest.json':
                    current = json.loads(target.read_text())
                    actual_database = (output_dir / 'catalog.sqlite').read_bytes()
                    manifest['files']['catalog.sqlite'] = {'sha256': digest(actual_database), 'bytes': len(actual_database)}
                    require(current == manifest, 'Manifest drift or hash mismatch')
                else:
                    require(target.read_bytes() == data, f'Generated file drift: {target}')
            for name, data in docs.items():
                require((HERE / name).read_bytes() == data, f'Report drift: {name}')
        else:
            existing = output_dir / 'manifest.json'
            if existing.exists():
                old = json.loads(existing.read_text())
                require(args.content_version >= old['content_version'], 'Content version downgrade forbidden')
                require(args.content_version != old['content_version'] or manifest == old,
                        'Published content version changed; increment --content-version')
            output_dir.mkdir(parents=True, exist_ok=True)
            for name, data in files.items():
                (output_dir / name).write_bytes(data)
            for name, data in docs.items():
                (HERE / name).write_bytes(data)
            if updated != registry or args.initialize_registry:
                registry_path.write_bytes(encoded(updated))
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
