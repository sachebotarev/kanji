"""Generate review documentation from the explicit, manually maintained approval registry."""
from pathlib import Path
import html
import json
import os
import re

ROOT = Path(__file__).resolve().parents[2]
DESIGN = Path(__file__).resolve().parent
CATALOG = DESIGN / '05-screens/catalog'


def main():
    registry = json.loads((DESIGN / 'theme/screens.json').read_text())
    registry = {**registry, 'generated_from': '../../theme/screens.json', 'screens': [s for s in registry['screens'] if s['module'] == 'catalog']}
    (CATALOG / 'review-status.json').write_text(json.dumps(registry, ensure_ascii=False, indent=2) + '\n')
    screens = registry['screens']
    assert len(screens) == 17 and len({s['id'] for s in screens}) == 17
    assert all(s['status'] in {'approved', 'unapproved'} for s in screens)
    pending = sum(s['status'] != 'approved' for s in screens)
    summary = f'Ожидают согласования: {pending} из {len(screens)}.'
    rows, cards = [], []
    review = CATALOG / 'review'
    review.mkdir(exist_ok=True)
    for screen in screens:
        ru = next(t for t in screen['translations'] if t['language_tag'] == 'ru')
        status = 'Согласовано' if screen['status'] == 'approved' else 'Не согласовано'
        spec = CATALOG / screen['spec']
        source = spec.read_text()
        source = re.sub(r'^Статус:.*?HTML-маршрут:', f"Статус: {status.lower()}. HTML-маршрут:", source, flags=re.M)
        source = source.replace('Эталоны: [тёмная тема]', 'Исходные снимки предыдущей редакции: [тёмная тема]')
        link = f"[Текущий макет](../../prototypes/index.html#catalog/{screen['id']}) · [Статус и изменения](review-status.json)."
        source = re.sub(r'^\[Текущий макет\].*\n?', '', source, flags=re.M)
        source = re.sub(r'\n{3,}', '\n\n', source)
        source = source.replace('\n\n', '\n\n' + link + '\n\n', 1)
        spec.write_text(source)
        rows.append(f"| {screen['id']} | [{ru['title']}]({screen['spec']}) | **{status}** | {ru['changes']} |")
        image = ROOT / 'doc/verification/catalog/refactor-2026-11-02' / (screen['id'].lower() + '.png')
        image_src = os.path.relpath(image, review)
        mockup = '../../../prototypes/index.html#catalog/' + screen['id']
        cards.append(f'''<article><p class="status" data-approval="{screen['status']}">{html.escape(screen['id'])} · {status}</p><h2>{html.escape(ru['title'])}</h2><p>{html.escape(ru['changes'])}</p><a href="{html.escape(mockup)}">Открыть интерактивный макет</a><details><summary>Снимок текущего Android</summary><img loading="lazy" src="{html.escape(image_src)}" alt="{html.escape(ru['title'])}"></details></article>''')
    (CATALOG / 'README.md').write_text('''# Каталог: экраны для повторного согласования

Все экраны перерисованы под общую тему Material 3. REVIEW_SUMMARY Это дизайн-макет; Android-код не менялся.

[Открыть интерактивный макет](../../prototypes/textbooks/index.html) · [Обзор экранов и снимки Android](review/index.html) · [Конфигурация статусов](../../theme/screens.json).

| ID | Экран | Статус | Изменения для просмотра |
| --- | --- | --- | --- |
'''.replace('REVIEW_SUMMARY', summary) + '\n'.join(rows) + '''

Статусы задаются в `doc/design-system/theme/screens.json`; локальный `review-status.json` — генерируемая совместимая копия. Статус меняется только после явного решения пользователя. Изменение общего компонента снимает согласование со всех затронутых экранов/состояний. Ключ рендерера `wip` не означает согласование.

Контракты переходов — [UX](../../02-ux/README.md), токены — [основы](../../03-foundations/README.md), компоненты — [библиотека](../../04-components/README.md), устройство Android — [реализация](../../../implementation/catalog-ui.md).

[Исходные эталоны](approved/index.html) сохранены для сравнения и не подтверждают согласование текущей редакции. В каталоге нет конструктора, редактирования слов и статистики. Добавление языка не меняет маршруты и бизнес-логику.
''')
    (review / 'index.html').write_text('''<!doctype html><html lang="ru" data-theme="light"><link rel="stylesheet" href="../../../prototypes/shared/material-theme.css"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Каталог — повторное согласование</title><style>body{font:16px/1.5 system-ui;margin:0;background:var(--md-sys-color-surface);color:var(--md-sys-color-on-surface)}main{max-width:1200px;margin:auto;padding:32px}a{color:var(--md-sys-color-primary)}h1{margin:0}header{margin-bottom:24px}.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:16px}article{background:var(--md-sys-color-surface-container);padding:20px;border-radius:20px}h2{font-size:20px;margin:8px 0}.status{font-weight:600;color:var(--md-sys-color-tertiary)}.status[data-approval=approved]{color:var(--md-sys-color-primary)}img{display:block;max-width:100%;height:auto;margin:12px auto 0}details{margin-top:16px}summary{cursor:pointer}</style><main><header><h1>Каталог: повторное согласование</h1><p>REVIEW_SUMMARY Откройте интерактивный макет для проверки переходов, тем и масштаба. Android-снимки показывают текущую реализацию; новых копий изображений здесь нет.</p><a href="../../../prototypes/textbooks/">Открыть макет</a> · <a href="../review-status.json">Реестр статусов</a></header><div class="grid">'''.replace('REVIEW_SUMMARY', summary) + ''.join(cards) + '</div></main>\n')
    print(f"Review documentation generated: {len(screens)} screens")


if __name__ == '__main__':
    main()
