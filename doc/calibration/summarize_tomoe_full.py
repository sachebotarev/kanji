"""Summarize recorded runs without changing or filtering cases based on their scores."""
from pathlib import Path
import csv
import json
import statistics

root = Path(__file__).resolve().parent

def kanji(row):
    code = int(row['target'], 16)
    return any(a <= code <= b for a, b in [(0x3400, 0x4dbf), (0x4e00, 0x9fff), (0xf900, 0xfaff), (0x20000, 0x323af)])

def load(name):
    with (root / name).open() as f:
        return [r for r in csv.DictReader(f, delimiter='\t') if kanji(r)]

def summarize(name, match, budget, hooks):
    rows = load(name)
    compatible = [r for r in rows if r['inputStrokes'] == r['referenceStrokes']]
    eligible = [r for r in compatible if not r['hooks']]
    accepted = sum(r['accepted'] == 'true' for r in eligible)
    return dict(file=name, match_percent=match, budget_percent=budget, hook_algorithm=hooks,
        kanji_entries=len(rows), count_compatible=len(compatible), count_mismatch=len(rows)-len(compatible),
        hooks_passed=len(eligible), hooks_failed=len(compatible)-len(eligible),
        accepted=accepted, accepted_percent_of_hook_passed=accepted/len(eligible)*100,
        mean_score_count_compatible=statistics.fmean(float(r['score']) for r in compatible),
        mean_budget_count_compatible=statistics.fmean(float(r['budget']) for r in compatible))

summary = dict(
    denominator='All kanji entries with matching stroke count and no missing-hook failure; dictionary failures stay in denominator.',
    baseline=summarize('tomoe-full-before.tsv', 72, 28, 'single strongest reference tangent'),
    target_90_comparison=summarize('tomoe-full-target90.tsv', 65.30, 27.86, 'salient reference tangent range'),
    calibrated_target_80=summarize('tomoe-full-final.tsv', 73.03, 27.36, 'salient reference tangent range'),
    normalization='Original coordinates / 1000, no alignment before matching.',
    weighting='One source entry is one case, including repeated characters; mismatched stroke counts excluded.',
    caveat='The same dictionary was used for calibration and evaluation; this is not held-out smartphone handwriting accuracy.',
)
assert summary['calibrated_target_80']['accepted_percent_of_hook_passed'] >= 80
(root / 'tomoe-full-summary.json').write_text(json.dumps(summary, ensure_ascii=False, indent=2)+'\n')
print(json.dumps(summary, ensure_ascii=False, indent=2))
