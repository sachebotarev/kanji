"""Prepare reviewed main content offline; does not modify the published seed or user DB."""
import argparse
from collections import Counter
import csv
import hashlib
import json
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]


def encoded(value):
    return json.dumps(value, ensure_ascii=False, indent=2) + '\n'


def resolve_translation(rows, language_tag):
    """Resolve a complete translation+note unit, never combine different languages."""
    by_tag = {r['language_tag'].lower(): r for r in rows}
    tag = language_tag.lower()
    return by_tag.get(tag) or by_tag.get(tag.split('-')[0]) or by_tag.get('ru')


def split_translation(written, reading, original):
    # Reviewed rule: remove only a terminal, exact duplicate of the whole reading.
    suffix = ' — чтение ' + reading
    if original.endswith(suffix):
        return original[:-len(suffix)], None, 'redundant_reading'
    # Every other change is an explicit editorial decision, not a heuristic split.
    decisions = {
        ('小人', 'こども'): ('ребёнок', 'В этой записи 小人 читается こども.'),
        ('明日', 'みょうにち'): ('завтра', 'Официальное чтение みょうにち.'),
        ('私', 'わたくし'): ('я', 'Вежливая и официальная форма.'),
        ('お手洗い', 'おてあらい'): ('туалет', 'Вежливое название.'),
        ('何ですか。', 'なんですか。'): ('что это?', 'Вежливый вопрос.'),
        ('お宅', 'おたく'): ('ваш дом', 'Вежливое обращение.'),
        ('奥さん', 'おくさん'): ('жена собеседника или другого человека', 'Вежливое обозначение.'),
        ('奥様', 'おくさま'): ('жена собеседника или другого человека', 'Особо вежливое обозначение.'),
        ('お礼', 'おれい'): ('благодарность', 'Вежливое слово.'),
        ('一個', 'いっこ'): ('один предмет', None),
        ('並の', 'なみの'): ('обычный, среднего уровня', None),
        ('便所', 'べんじょ'): ('туалет', None),
        ('個々の', 'ここの'): ('каждый отдельный, индивидуальный', None),
        ('個人的な', 'こじんてきな'): ('личный, индивидуальный', None),
        ('元気な', 'げんきな'): ('здоровый, бодрый', None),
        ('共通の', 'きょうつうの'): ('общий для нескольких, совместный', None),
        ('別の', 'べつの'): ('другой, иной', None),
        ('到る', 'いたる'): ('достигать, доходить', None),
        ('区', 'く'): ('городской район', None),
        ('友', 'とも'): ('друг', None),
        ('取り引き', 'とりひき'): ('торговая сделка', None),
        ('問', 'とい'): ('вопрос, задание', None),
        ('大きな', 'おおきな'): ('большой', None),
        ('家内', 'かない'): ('моя жена', None),
        ('市', 'し'): ('город', None),
        ('必要な', 'ひつような'): ('необходимый', None),
        ('悪性の', 'あくせいの'): ('злокачественный', None),
        ('文化的な', 'ぶんかてきな'): ('культурный', None),
        ('晩御飯', 'ばんごはん'): ('ужин', None),
        ('最初の', 'さいしょの'): ('первый, начальный', None),
        ('有る', 'ある'): ('существовать, иметься', None),
        ('比べる', 'くらべる'): ('сравнивать', None),
        ('比較する', 'ひかくする'): ('сравнивать', None),
        ('無い', 'ない'): ('отсутствовать, не иметься', None),
        ('焼肉', 'やきにく'): ('жареное мясо', None),
        ('田', 'た'): ('рисовое поле', None),
        ('県', 'けん'): ('префектура', None),
        ('真白', 'まっしろ'): ('совершенно белый', None),
        ('米国', 'べいこく'): ('США', None),
        ('者', 'もの'): ('человек, лицо', None),
        ('買物', 'かいもの'): ('покупки', None),
        ('適した', 'てきした'): ('подходящий, соответствующий', None),
        ('顔面', 'がんめん'): ('лицо', None),
    }
    if (written, reading) in decisions:
        translation, note = decisions[written, reading]
        return translation, note, 'editorial_note'
    return original, None, 'unchanged'


def prepare():
    snapshot_path = HERE / 'sources/classification-snapshot.json'
    snapshot = json.loads(snapshot_path.read_text())
    grades = {k: v['grade'] for k, v in snapshot.items() if v['grade'] in range(1, 7)}
    assert Counter(grades.values()) == {1: 80, 2: 160, 3: 200, 4: 200, 5: 185, 6: 181}
    # MEXT 2017 curriculum, effective 2020; explanatory text p.18 (PDF p.22).
    for symbols, old, new in [
        ('茨媛岡潟岐熊香佐埼崎滋鹿縄井沖栃奈梨阪阜', None, 4),
        ('賀群徳富', 5, 4), ('城', 6, 4),
        ('囲紀喜救型航告殺士史象賞貯停堂得毒費粉脈歴', 4, 5),
        ('胃腸', 4, 6), ('恩券承舌銭退敵俵預', 5, 6),
    ]:
        for symbol in symbols:
            assert grades.get(symbol) == old, symbol
            grades[symbol] = new
    assert Counter(grades.values()) == {1: 80, 2: 160, 3: 200, 4: 202, 5: 193, 6: 191}
    with (ROOT / 'data/catalog/kanji.csv').open(encoding='utf-8-sig', newline='') as stream:
        catalog = list(csv.reader(stream))
    assert catalog[0][8] == 'Входит в Дзёё кандзи'
    kanji = []
    for row in catalog[1:]:
        ident, symbol = int(row[0]), row[1]
        assert ident == ord(symbol)
        grade = grades.get(symbol)
        level = snapshot.get(symbol, {}).get('jlpt_new')
        assert level is None or level in range(1, 6)
        # Elementary years come from the MEXT-adjusted list. Every other jōyō
        # character belongs to junior high as a stage; that curriculum has no
        # per-year list, so the year stays empty. Jinmeiyō is not a school stage.
        if grade:
            stage, year = 'elementary', grade
        elif row[8] == 'true':
            stage, year = 'junior_high', None
        else:
            stage, year = None, None
        kanji.append({'kanji_id': ident, 'symbol': symbol,
                      'school_stage': stage, 'school_year': year, 'jlpt_level': level})
    assert len(kanji) == 3001 and len({r['kanji_id'] for r in kanji}) == 3001
    seed_path = ROOT / 'data/catalog/seed/catalog.json'
    seed = json.loads(seed_path.read_text())
    def rows(table):
        t = seed['tables'][table]
        return [dict(zip(t['columns'], r)) for r in t['rows']]
    words = {w['id']: w for w in rows('words')}
    translations, changes = [], []
    for row in rows('words_translations'):
        w = words[row['word_id']]
        original = row['translation']
        text, note, reason = (split_translation(w['written_form'], w['reading'], original)
                              if row['language_tag'] == 'ru' else (original, None, 'unchanged'))
        assert text.strip()
        item = {'word_id': row['word_id'], 'language_tag': row['language_tag'],
                'translation': text, 'usage_note': note, 'original_translation': original}
        translations.append(item)
        if reason != 'unchanged':
            changes.append({**item, 'written_form': w['written_form'], 'reading': w['reading'], 'reason': reason})
    inputs = [snapshot_path, seed_path, ROOT / 'data/catalog/kanji.csv', HERE / 'build_content.py']
    report = {'kanji_count': len(kanji), 'translations_count': len(translations),
              'school_stage_counts': dict(sorted(Counter(r['school_stage'] for r in kanji if r['school_stage']).items())),
              'school_year_counts': dict(sorted(Counter(r['school_year'] for r in kanji if r['school_year']).items())),
              'stage_without_year': sum(r['school_stage'] is not None and r['school_year'] is None for r in kanji),
              'without_school': sum(r['school_stage'] is None for r in kanji),
              'jlpt_counts': dict(sorted(Counter(r['jlpt_level'] for r in kanji if r['jlpt_level']).items())),
              'without_jlpt': sum(r['jlpt_level'] is None for r in kanji),
              'editorial_changes': len(changes), 'notes_count': sum(r['usage_note'] is not None for r in translations),
              'inputs_sha256': {str(p.relative_to(ROOT)): hashlib.sha256(p.read_bytes()).hexdigest() for p in inputs}}
    return {'kanji-classifications.json': kanji, 'words-translations.json': translations,
            'editorial-changes.json': changes, 'validation.json': report}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    for name, value in prepare().items():
        path = (ROOT / 'data/catalog/content' if name in (
            'kanji-classifications.json', 'words-translations.json') else HERE) / name
        text = encoded(value)
        if args.check:
            assert path.read_text() == text, f'Generated content drift: {path}'
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(text)
    print('Main content: OK')


if __name__ == '__main__':
    main()
