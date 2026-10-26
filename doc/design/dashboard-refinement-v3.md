# Дашборд — уточнение v3

20 октября 2026. [Макет двух тем и расшифровки](dashboard-refinement-v3.png). Встроенный image_gen; код пока не изменён.

Основа — dashboard-refinement-v1.md. Сохраняются все уточнения: компактная аналитика без легенды на главной, диалог подробностей по нажатию на статистику, тоньше стрелка, ближе подпись микса, меньше заголовок словарей, тонкая светлая внешняя обводка кота в тёмной теме.

Новые требования пользователя:

- Заголовок «Активность» заменяется пламенем и «7 дней подряд». Отдельной подписи внизу сетки нет. Пн/Ср/Пт остаются на строках 1/3/5. Семантическое описание активности сохраняется.
- Фоновая плашка запуска увеличивается с 64 до 80 dp. Кот чуть крупнее, по-прежнему выступает сверху и снизу. Порядок текст/кот/стрелка сохранён, подпись микса отдельно ниже. Необходимый зазор от лап до подписи — не менее 8 dp.

Генерация может давать градиенты и случайные контуры: в коде используются плоские заливки принятой темы. Приоритет у описанных размеров и отношений.

## Промпты последовательных изменений

```text
Edit this dashboard refinement mockup with one strictly localized change, applied consistently to ALL THREE panels (light dashboard, dark dashboard, dimmed dashboard behind dialog).
In the activity card, REPLACE its top title "Активность" with a small flame icon plus text "7 дней подряд" on a single line. Use the same title-size hierarchy as "Проверено слов" in adjacent card, with a proportionate small vector-like flame, no emoji styling. REMOVE the bottom flame+"7 дней подряд" row entirely. The grid and weekday labels remain below the new title. Center the grid vertically in the available body, preserve 6 columns and 7 rows, Пн/Ср/Пт label rows1/3/5. Do not make the card taller, keep both analytics cards equal height. No duplicate streak caption, no word Активность anywhere.
Keep every other element and dimensions unchanged: donut with NO legend on main screen, unified CTA text/cat/arrow, cat overhang both top and bottom, separate smart mix subtitle, dictionary section, and the details dialog on right with three category rows ONLY there. Preserve both themes, exact Russian text and restrained flat colors. One final image.

Edit this three-panel dashboard mockup with one localized change: make the main green "Ня! Начнём?" start BUTTON body 25 percent TALLER, corresponding to80dp instead of64dp. Apply to all three panels, including dimmed behind dialog. Keep button width, text/cat/arrow left-to-right arrangement and typography unchanged. Increase cat just slightly, about10 percent, so its hat still visibly overhangs ABOVE button and feet/tail BELOW. Preserve subtle sage silhouette keyline on dark theme cat, not white sticker outline. Keep arrow centered vertically and its restrained thin stroke. The separate subtitle "Умный микс · учебники и мой словарь" stays below with8dp safe gap from cat feet; move lower modules down only as necessary. No separate second button. Keep analytics compact, no legend on main dashboard, "flame 7 дней подряд" in activity HEADER only, weekdays and grid unchanged. Keep details dialog content unchanged. Flat fills, existing light/dark colors, no redesign, no gradients. One final updated comparison.
```
