# Дашборд v3 — светлая и тёмная темы

20 октября 2026. Макет для согласования, без реализации.

[Сравнение тем](dashboard-v3-themes.png). [Скриншот Pixel 10 Pro](dashboard-reference-pixel-2026-10-20.png) снят через ADB: на устройстве открыт экран практики в тёмной теме. Светлая палитра взята из KanjiTheme.kt, тёмная сверена с ним и реальным скриншотом. Системная тема устройства не менялась.

Сохранены изменения v2: компактные иконки статистики, активность без пояснений, сводка всех учебников. Компоновка обеих тем одинаковая. Растр — визуальное предложение; при реализации точные цвета берутся из MaterialTheme. Генерация слегка варьирует ячейки активности и оттенки заливок, эти различия не являются требованиями. Статистика демонстрационная.

Создано встроенным image_gen. Референсы: dashboard-v2.png (содержание), dashboard-reference-pixel-2026-10-20.png (оформление тёмной темы).

## Промпт

```text
Use case ui-mockup. Prepare one comparison image showing TWO identical Russian Android kanji HOME DASHBOARD screens side by side, light theme LEFT and dark theme RIGHT. Each screen full portrait Pixel 10 Pro aspect 1080:2412, both fully visible, no phone hardware frames, no perspective, no decorative backdrop. Small labels outside screens "Светлая" and "Тёмная". High resolution crisp Russian UI.
Image 1 (dashboard) is layout and content reference. Image 2 (real Pixel screenshot) is the authoritative DARK theme appearance reference only; do not copy its practice-screen content.
Preserve exact dashboard contents and arrangement from image 1: Главная with subtitle and gear; paired cards Проверено слов donut 1 248 and compact icon counts check 874 / lightbulb 249 / cross 125, Активность 6 columns x7 rows green squares and flame 7 дней подряд with NO weekday or intensity explanations; wide Начать тренировку button; smart mix caption; Словари / 単語 two rows Встроенные учебники / 12 учебников · 4 активны and Мой словарь / Всего 156 · изучено 92 / 18 с ошибками · 12 с подсказкой; История сессий / Результаты и архив ошибок; Конструктор тренировок / Активный шаблон: Умный микс; Купить автору кофе. Same coordinates, shapes and sizes in both themes. Use normal Android sans serif and modest Material 3 typography; heading less oversized than image 1, closer to Pixel screenshot. Flat fills, no gradients, no textures, no shadows.
EXACT LIGHT theme color tokens: background #FAF8F1, surface #FFFDF7, main text #263E36, secondary text #647168, primary #426B53 and white on primary button, outline #B9C2B5, surface variant #EDEAE2, hint icon muted gold, error #A64935.
EXACT DARK tokens matching Pixel screenshot: background #181E19 (green-black), surfaces #202720, main text #E7E9DF, secondary text #B7C3B6, borders #58665A, primary #ADCDB0, button fill #ADCDB0 with dark green text #173822; surface variant #354038. Donut success #ADCDB0, hint soft gold, error #FFB59F. Activity from #354038 to #ADCDB0. No neon colors, no pure black or blue background. Borders visible in both themes like practice field screenshot. Compact useful readable design feasible with Compose. Do not add new UI sections.
```
