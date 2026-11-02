import unittest

from build_content import prepare, resolve_translation, split_translation


class ContentTest(unittest.TestCase):
    def test_coverage_and_missing_values(self):
        result = prepare()
        self.assertEqual(len(result['kanji-classifications.json']), 3001)
        self.assertEqual(len(result['words-translations.json']), 2491)
        by_symbol = {r['symbol']: r for r in result['kanji-classifications.json']}
        self.assertEqual(by_symbol['茨']['school_stage'], 'elementary')
        self.assertEqual(by_symbol['茨']['school_year'], 4)
        self.assertEqual(by_symbol['胃']['school_year'], 6)
        self.assertEqual(by_symbol['囲']['school_year'], 5)
        self.assertEqual(by_symbol['亜']['school_stage'], 'junior_high')
        self.assertIsNone(by_symbol['亜']['school_year'])
        self.assertEqual(by_symbol['剝']['school_stage'], 'junior_high')
        self.assertIsNone(by_symbol['丑']['school_stage'])
        self.assertIsNone(by_symbol['々']['school_stage'])
        self.assertIsNone(by_symbol['々']['school_year'])
        self.assertIsNone(by_symbol['々']['jlpt_level'])
        self.assertTrue(all(set(r) == {'kanji_id', 'symbol', 'school_stage', 'school_year', 'jlpt_level'}
                            for r in by_symbol.values()))
        report = result['validation.json']
        self.assertEqual(report['school_stage_counts'], {'elementary': 1026, 'junior_high': 1110})
        self.assertEqual(report['stage_without_year'], 1110)
        self.assertEqual(report['without_school'], 865)

    def test_only_exact_reading_duplicate_removed(self):
        self.assertEqual(split_translation('日本', 'にっぽん', 'Япония — чтение にっぽん'),
                         ('Япония', None, 'redundant_reading'))
        for original in ['чтение книг', 'онъёми — китайское чтение кандзи', 'Япония — чтение にほん']:
            self.assertEqual(split_translation('読書', 'どくしょ', original),
                             (original, None, 'unchanged'))

    def test_originals_and_identity_preserved(self):
        result = prepare()
        self.assertEqual(len({(r['word_id'], r['language_tag']) for r in result['words-translations.json']}), 2491)
        self.assertEqual(len(result['editorial-changes.json']), 22)
        self.assertEqual(sum(r['usage_note'] is not None for r in result['words-translations.json']), 9)
        self.assertTrue(all(r['original_translation'] for r in result['words-translations.json']))

    def test_localized_unit_and_russian_fallback(self):
        ru = {'language_tag': 'ru', 'translation': 'я', 'usage_note': 'Вежливая форма.'}
        en = {'language_tag': 'en', 'translation': 'I', 'usage_note': None}
        self.assertIs(resolve_translation([ru, en], 'en-US'), en)
        self.assertIs(resolve_translation([ru, en], 'de'), ru)
        self.assertIs(resolve_translation([ru, en], 'ru'), ru)
        self.assertIsNone(resolve_translation([], 'de'))


if __name__ == '__main__':
    unittest.main()
