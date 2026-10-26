# Единая карточка запуска тренировки

20 октября 2026. Макет для согласования, код не изменён.

[Макет двух тем](dashboard-combined-cta-v1.png). [Свежий скриншот Pixel до изменений](dashboard-combined-cta-before-pixel.png). Создано встроенным image_gen.

Приветствие и кнопка старта объединены в одну кликабельную карточку под названием приложения. Слева «Ня! Начнём?», «Начать тренировку →» и «Умный микс». Справа тот же кот; шапка выступает над верхней границей фона карточки. Над карточкой резервируется место под выступ, кот не перекрывает заголовок и шестерёнку. Отдельная кнопка и подпись микса под аналитикой удаляются. Вся карточка вызывает прежний onStartPractice и имеет одну accessibility-роль кнопки. Остальные действия не меняются.

После осмотра Pixel выявлены слишком мелкие внутренние штрихи галочки и крестика: вложенная иконка дополнительно сжималась внутри маленького круга. Предлагаются цельные согласованные векторные значки с читаемым рисунком: ориентир 14 dp рядом с числами 13 sp, одинаковая визуальная линия и оптическая центровка. Пламя также согласуется с текстом. Точные размеры проверяются по скриншоту реализации.

Генерация показывает только верхнюю часть экрана; история, конструктор и поддержка остаются ниже. Стрелки на неактивных словарях, случайные оттенки и градиенты из генерации не являются изменением требований. В реализации сохраняются существующие плоские заливки темы.

## Промпт

```text
Use case ui-mockup. Create a refined practical Android dashboard proposal in TWO themes side by side, portrait screen crops from app title through vocabulary card. Reference image1 actual Pixel screenshot gives current app content and theme. Reference image2 exact cat art identity.
Requested change: MERGE the current mascot greeting widget AND the separate start practice button into ONE prominent clickable start card under title. Title remains "Кандзи дэс нэ" with settings gear.
Merged card: compact, around 104dp high body, warm pale green in light and deeper forest green in dark, no extra CTA button elsewhere. Left side text "Ня! Начнём?" as main label and below small "Начать тренировку" with clear right arrow icon. Right side uses the exact waving ivory cat. The cat MUST visibly overlap the TOP edge of the card: hat and upper head extend ABOVE the rounded colored rectangle by about 24dp into screen background, paws and tail remain visible over card. Allow enough vertical gap below app title so hat does not overlap title or gear. Mascot not clipped and no separate tile. Treat whole card as one obvious button, not two controls. Inside lower left add tiny subtitle "Умный микс" only if spacious. Do not repeat standalone start button below analytics. This card replaces both previous CTA surfaces.
Below: two equal-height analytics cards horizontally, preserving donut 1248, legend learned874 assisted249 errors125, activity six columns seven rows, weekday labels Пн Ср Пт on rows1 3 5. Streak "7 дней подряд" centered under activity.
ICON CORRECTION: use coherent crisp standard Material filled icons, clear check-circle, lightbulb, cancel-circle, flame. Approx icon graphic14dp with numbers13sp, readable check/cross, balanced line weight, no tiny check nested inside a huge circle, no cartoon oversized flame. One baseline and exact optical vertical centering of icons and numbers. Same footer line heights. Show at realistic 412dp width. All symbols legible at small size.
Then existing vocabulary card with "Словари / 単語", "Встроенные учебники", "12 учебников · 4 активны", "Мой словарь", "Всего 156 · изучено 92", "18 с ошибками · 12 с подсказкой". No extra widgets in crop.
Light tokens #FAF8F1 background, #FFFDF7 surfaces, #263E36 text, #426B53 accent, pale #DDEDD8 CTA card. Dark #181E19 background, #202720 surface, #E7E9DF text, #ADCDB0 accent, #244F32 CTA card. Same cat in both themes without recoloring, retain forest-green cap, red stripe, gold badge. Flat solid fills, NO gradients shadows textures. Small outside labels "Светлая" "Тёмная". No device frames. High resolution crisp Russian text.
```
