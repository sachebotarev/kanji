# Дашборд v4 — название, иконки и дни недели

20 октября 2026. Реализовано вместе с маскотом после указания пользователя «Давай попробуем реализовать».

[Макет](dashboard-header-icons-v4.png). [Свежий снимок Pixel](dashboard-icon-review-home-pixel.png). Создано встроенным image_gen с последовательными точечными правками референса.

- Заголовок: название приложения «Кандзи дэс нэ» / Kanji Desu Ne.
- Иконки статистики уменьшаются до визуальной высоты цифр (ориентир 12 dp рядом с 11 sp); выравнивание по видимым глифам.
- Пламя: ориентир 13–14 dp рядом с 12 sp. Группа стрика центрируется под сеткой.
- Высота карточек, нижние отступы и вертикальная позиция строк статистики и стрика согласованы.
- Слева от сетки: Пн / Ср / Пт, строго на строках 1 / 3 / 5. Шесть столбцов и семь строк сохранены.

Макет показывает верхний фрагмент экрана. Генерация не задаёт новые размеры диаграммы, шрифт всей страницы или градиенты. Фон и кнопки в реализации сохраняют сплошные заливки темы. Остальные виджеты не меняются.

## Промпты последовательных правок

```text
Use case ui-mockup. Edit this actual Pixel screenshot for a very precise typography and alignment correction. Output only the upper dashboard region containing heading, both analytics cards and start button, as a large legible crop. Keep real app dark green palette #181E19 background #202720 cards #E7E9DF text, and exact existing Russian content. Do not redesign or alter fonts, chart, grid, borders, headings, card widths.
Change only bottom lines of both analytics cards:
1 Left card: green filled check circle, gold filled bulb, salmon filled error circle become MUCH SMALLER, about the visual cap height of the adjacent digits (874,249,125). Icons should be subordinate to the numbers, about 12 dp graphic height beside 11sp numbers, not current 18dp. Keep recognizable clear check and cross with enough contrast, consistent compact icon sizes, 4dp gap to number. Each icon optically centered on digit glyphs, not on text line box.
2 Right card: salmon flame becomes much smaller, about 13dp visual height beside 12sp text "7 дней подряд", optically centered on visible text. Center ENTIRE flame+text group horizontally below contribution grid, instead of left aligning to card. 4dp icon/text gap. Match bottom text baseline to left card legend digits.
3 Both cards must have exactly equal height and identical bottom padding. Bottom footer lines must sit at exactly same vertical position.
Preserve main heading "Главная", subtitle, gear, total 1248, grid 6 columns by7 rows and full green Начать тренировку button. Flat screenshot-like proposal, no phone frame, no decorative annotation, no labels before/after, no extra screens. No oversized icons. This is a careful minor correction based on a real screenshot, not a new dashboard concept.

Edit this analytics mockup with ONE change only: add small muted weekday labels to the LEFT of the contribution grid in Активность. Labels exactly "Пн", "Ср", "Пт" aligned with grid rows 1, 3, 5 respectively. Correct row mapping is mandatory. Grid has 7 rows: Monday row1, Tuesday row2, Wednesday row3, Thursday row4, Friday row5, Saturday row6, Sunday row7. Shift the grid right slightly if needed; center labels+grid together within card. Keep all else unchanged: small icons, centered flame+streak footer, equal card heights, title, donut, colors and CTA. Flat solid colors, no gradients. One finished corrected cropped mockup.

Edit this exact dashboard proposal. Change the top heading "Главная" to the app name EXACTLY "Кандзи дэс нэ". Keep it on one line, reduce heading font size if necessary, retaining room for gear button at far right. Change nothing else: preserve subtitle, compact small footer icons aligned to text, 6x7 activity grid with Пн Ср Пт labels, centered flame+streak group, equal card heights, palette and CTA. Flat solid backgrounds without gradients. One final image.
```
