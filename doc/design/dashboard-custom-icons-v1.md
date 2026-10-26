# Авторские PNG-иконки главной — v1

20 октября 2026. Согласовано пользователем и реализовано. Встроенный image_gen.

[Набор с малыми превью](dashboard-custom-icons-v1-sheet.png), [макет двух тем](dashboard-custom-icons-v1.png).

Восемь направлений: настройки — кремовое колесо с кошачьими ушками; учебники — раскрытая книга с красной закладкой; личный словарь — коробочка карточек со следом лапы; история — свиток с часами; конструктор — учебные плитки с карандашом; поддержка — чашка со следом лапы; запуск — мягкая стрелка с изогнутым хвостом; подробнее — родственная стрелка по диагонали. PNG-пламя из v5 сохраняется.

Палитра и контуры повторяют маскота: кремовый, тёмно-зелёный, шалфейный, красный и золотой. Читаемость в реальном размере важнее деталей. PNG не перекрашиваются через tint. Ориентиры графики: настройки24dp, учебные разделы28dp, поддержка20dp, запуск40dp, подробнее14dp; области нажатия настроек и всех действий минимум48dp. Варианты на тёмном фоне требуют только тонкого внешнего отделения, без яркой белой рамки.

Макет включает ранее согласованные текстовые правки: «Следы учёбы» вместо «Наши успехи» на карточке и в диалоге; у сетки также знак раскрытия. Текущее действие сетки в коде отсутствует: при реализации нового клика требуется отдельный callback/экран либо явно демонстрационная расшифровка, без имитации реальных данных.

Лист — концепт набора, не атлас для прямого использования. После согласования каждая финальная иконка генерируется отдельным PNG с прозрачностью и сохраняется в doc/design/ и ресурсах приложения. Макет может упрощать мелкие стрелки; при упаковке ориентир — выбранный оригинальный набор. Прочие размеры экрана и бизнес-логика не меняются.

## Промпт набора

```text
Use case ui-mockup / illustration asset design sheet. Design an ORIGINAL coordinated icon family for the existing Japanese kanji learning Android app with the ivory cat student mascot in reference1. Reference2 is the current actual dashboard showing placements. This is a REVIEW SHEET, not final separate asset files.
Create exactly EIGHT distinct icons, arranged in a clean 2 rows x4 columns contact sheet. Each cell has one medium-sized full-color icon and a pair of tiny24px equivalent previews of SAME icon on warm ivory and dark-green swatches, plus a small readable Russian name BELOW (not baked into icon). Use generous orderly space. All icons consistent visual scale, line weight and palette, as if drawn by same illustrator.
Icon1 "Настройки": compact ivory rounded adjustment wheel, subtle cat-ear silhouette on upper edge, forest-green outer contour, small brass center and tiny coral adjustment mark. Recognizable settings control, custom, not stock Material gear.
Icon2 "Учебники": open ivory book with forest-green spine and outlines, tiny red bookmark ending in a simple cat-ear notch, broad clean pages, no text.
Icon3 "Мой словарь": small box of two ivory vocabulary cards with sage edges, one simple golden paw seal. Distinct silhouette from open book, no text.
Icon4 "История": short cream rolled practice scroll with a small readable gold clock face over lower corner, green outline, no letters.
Icon5 "Конструктор": three chunky ivory/sage learning tiles assembling in an L shape plus one short coral pencil. Clear arrangement/tool metaphor, no kanji or letters.
Icon6 "Поддержка": simple ivory coffee mug with green handle and a single tiny coral paw emblem, one soft steam curl, not a face.
Icon7 "Начать": clean custom right-arrow with slightly curved tail inspired by cat tail, broad clear arrowhead, ivory center and green contour, subtle hand-drawn character, NO paw, NO props, not a chevron. Legible at32dp.
Icon8 "Подробнее": compact custom northeast arrow matching icon7, ivory center and green contour, restrained curve, easily understood expand action, legible at14dp. No extra box or circle.
Style: polished flat2D app illustrations matching mascot: thick soft forest-green outlines, warm ivory major fills #FFF8E8, sage #ADCDB0, muted red band accent #D66C52, small gold #E8BA4C. Not emojis, not3D, no plastic, no gradients, texture, shading, glow, drop shadow or busy decoration. Do NOT put the whole cat or student hat into each icon. Strong shapes and enough ivory fill to work unchanged on #FAF8F1 and #181E19 backgrounds. Thin sage outer edge only where needed for dark background, not sticker border. Simplicity and usefulness over cuteness. No duplicated icons, exactly8. Show icons at realistic relative sizes too, not only enlarged. Professional coherent original asset exploration.
```

## Промпт макета

```text
Use case ui-mockup. Replace stock icons in the current dashboard image1 with the coordinated original illustrated icon family from image2. Make a TWO-PANEL comparison light and dark dashboards, full screen crops including support row, no hardware frame. Preserve all accepted layout, typography hierarchy, component sizes, spacing, colors, cat greeting CTA and chart/grid data.
Use icon sheet semantic mappings: cat-ear adjustment wheel = top-right settings at24dp graphic; open book with red bookmark = textbooks row28dp; paw-card box = personal dictionary28dp; scroll/clock = history28dp; three tiles/pencil = training builder28dp; mug/paw = quiet support20dp; curved right-arrow = main CTA40dp; curved northeast-arrow = BOTH analytics card header top-right at14dp each. Show SAME full-color ivory/green/coral/gold icons in both themes, no tint/recolor, thin muted sage outside edge only as needed for dark contrast. Don't enlarge icons beyond text scale; main cat remains dominant.
Also rename "Наши успехи" to EXACTLY "Следы учёбы". Total1248 no inline legend. ADD the small matching northeast expand icon to the activity header as well as statistics header. Activity title small flame plus "7 дней подряд", maintain row1/3/5 Пн Ср Пт, six columns seven rows. Flame stays the already custom orange/gold flame.
Keep CTA below analytics with text "Ня! Начнём?" then small "Умный микс" inside left, exact waving cat with cap-redband-goldbadge protruding BOTH top and bottom, curved arrow to its right with12dp visual gap.80dp body. Not extra cat in every icon. Primary vocabulary card unchanged. History and builder plain compact rows separated with one hairline, NO cards; coffee row quiet centered with no border. NO new chevrons on inactive rows. No new functionality indicated except two analytics expand symbols.
Palette light#FAF8F1 #FFFDF7 #263E36 #DDEDD8 CTA, dark#181E19 #202720 #E7E9DF #244F32 CTA. Flat solid fills no gradients textures glow or shadows. Small outside labels "Светлая" / "Тёмная". Professional implementable412dp Android layout.
```

## Реализация

Восемь отдельных исходников PNG и [промпты](dashboard-icons/prompts.md) сохранены в dashboard-icons/. Подключены через IllustratedIcon без tint, с высокой фильтрацией уменьшения и оптической компенсацией полей. Название «Следы учёбы» обновлено на главной, в accessibility-описании и в диалоге. Сетка раскрывает ActivityDetailsDialog с тем же демонстрационным снимком: стрик, период, активные дни и число заданий. Состояния диалогов находятся в DashboardScreen; виджеты получают callbacks.

По последующему запросу пользователя строка поддержки удалена с главной. Её авторский исходник сохранён для возможного отдельного использования.

Сборка debug и lint прошли. DashboardNavigationTest — 3/3 (запуск/возврат, статистика, активность). После тестов визуально уточнены оптические размеры; новых изменений навигации не было. Итоговая сборка установлена на Pixel. [Тёмная тема](dashboard-custom-icons-review-pixel.png), [светлая](dashboard-custom-icons-pixel-light.png). Системная тема возвращена в исходное тёмное состояние.
