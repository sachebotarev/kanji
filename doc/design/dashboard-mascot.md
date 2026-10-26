# Кот на дашборде

20 октября 2026. Реализован декоративный приветственный блок.

- [Прозрачный исходник](dashboard-mascot-wave-v1.png), создан встроенным image_gen по референсу launcher-icon-cat-v1-source.png.
- Ресурс приложения: feature/dashboard/src/main/res/drawable-nodpi/mascot_wave.png — точная копия исходника с alpha-каналом.
- [Предварительный концепт двух тем](dashboard-mascot-concept-v1.png). После замечания о повторении названия слоган в коде изменён на «Ня! Начнём?». Название «Кандзи дэс нэ» находится только в заголовке.
- [Реализация на Pixel: светлая](dashboard-mascot-pixel-light.png), [тёмная](dashboard-mascot-pixel-dark.png).

Пользователь разрешил пробную реализацию. Персонаж сохраняет внешность кота с иконки: кремовая шерсть, зелёный контур, ученическая шапка с красной полосой и жёлтым значком. Изображение декоративное; содержимое блока и размеры адаптируются независимо от него.

## Промпт иллюстрации

```text
Use case identity-preserve. Create a transparent-background UI mascot illustration based on this EXACT existing app-icon cat character. Preserve identity: warm ivory cat, simple dark forest green oval eyes, tiny triangular nose, rounded cat smile, two whisker marks per cheek, peach inner ears, thick clean dark-green outlines, distinctive green Japanese student cap shaped like 宀 with red band and gold circular badge, curved hooked tail. Same colors and graphic style as reference. New friendly greeting pose: full small seated floating cat, left paw resting low, right paw raised in a small welcoming wave, slight happy head tilt, both hind paws and short hooked tail visible. Keep proportions recognizably identical, don't introduce clothing or accessories. Polished flat illustration suitable for Android dashboard at 80dp high. Simple legible silhouette, generous transparent margins, no background, no tile, no border, no floor, no shadows, no texture, no gradient, no text, no letters, no props. One character only. Actual alpha transparency.
```

## Промпт концепта

```text
Use case ui-mockup. Create a practical Android dashboard header + analytics mockup in TWO THEMES side by side. Reference image1 is the existing dashboard design: preserve all analytics data, equal card heights, tiny inline legend icons, weekday labels Пн Ср Пт aligned with rows1 3 5, centered small flame and "7 дней подряд", and "Начать тренировку" CTA. Reference image2 is the EXACT approved app cat mascot illustration: preserve this cat identity, don't redesign.
Change header: title "Кандзи дэс нэ" with gear at far right. Under title replace existing long slogan with a compact friendly greeting strip, about 80dp tall. Text at left EXACTLY "Мяу! Кандзи ждут!" split over two lines if needed. Cat at right at 72dp visual height, small welcoming wave, naturally sitting within header, no separate oversized illustration, no chat bubble, no illustration background, no clipped hat or tail. No additional copy or labels inside app.
Left theme: background #FAF8F1, text #263E36, muted #647168, card #FFFDF7, border #B9C2B5, primary #426B53, header greeting strip may use pale #DDEDD8.
Right theme: background #181E19, text #E7E9DF, muted #B7C3B6, card #202720 border #58665A primary #ADCDB0; greeting strip subtle #244F32.
Use same full-color ivory cat in BOTH themes, without recoloring. Enough separation from backgrounds. Fine dark-green outline.
Two flat screen crops only from title to CTA inclusive, no phone frame, high resolution Russian text, no explanatory annotations, small outside headings "Светлая тема" / "Тёмная тема". No gradients, no texture, no glow. Feasible 412dp Android width, understated typography. Stats and training remain dominant, mascot only small welcoming accent.

Change ONLY the slogan inside the green greeting banner in BOTH light and dark screens. Replace "Мяу! Кандзи ждут!" with EXACTLY "Кандзи дэс ня!" in one or two balanced lines. The top app name remains "Кандзи дэс нэ" with final нэ, but slogan ends ня with exclamation point. Preserve EXACTLY cat appearance, size, pose, banner, all dashboard content, typography scale, weekday labels and alignments. Do not add text. Two themes side by side, same crop. Flat fills no gradients.
```
