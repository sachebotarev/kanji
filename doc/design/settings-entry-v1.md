# Вход в настройки — v1

20 октября 2026. Согласовано пользователем и реализовано. См. [постоянные настройки](../implementation/persistent-settings.md).

[Макет двух тем](settings-entry-v1.png), встроенный image_gen, референс dashboard-v3-themes.png.

Маршрут: шестерёнка справа от «Главная» → «Настройки» → «Проверка кандзи» → существующий CheckerSettingsScreen. Подпись пункта: «Точность, допуски и правила». Других пунктов пока нет. Назад проходит цепочку в обратном порядке. Должен использоваться общий RecognitionSettingsViewModel: изменения из главной и из практики относятся к одним параметрам. При входе из главной не нужен preview текущего рисунка.

Макет иллюстрирует добавляемые элементы, не меняет согласованные размеры остальных блоков. Генерация обрезала нижнюю часть тёмных экранов; это не требование компоновки.

## Промпт

```text
Use case ui-mockup. Create a clean design review image with FOUR flat Android phone screens in a 2x2 grid, no device hardware. Top row light theme, bottom row dark theme. Left column home dashboard (same contents and composition as reference image), right column NEW general settings screen. Small outside labels "Главная · светлая", "Настройки · светлая", "Главная · тёмная", "Настройки · тёмная".
The only change to the home dashboard is a visible simple gear outline button at upper right aligned with "Главная", 48dp touch target. Preserve reference layout and all existing content.
General settings screen: Android status bar, top app bar back arrow and title "Настройки". Below at 20dp margins ONE full-width outlined rounded navigation row, leading sliders outline icon, primary title "Проверка кандзи", secondary text "Точность, допуски и правила", trailing chevron. Remaining screen empty clean theme background. No invented settings, switches, categories or extra text. This item opens an EXISTING kanji checking settings screen, which is NOT shown here. No arrows connecting screens outside, no explanatory callouts.
Material 3, flat solid fills, no gradients or textures. Light: background #FAF8F1 surface #FFFDF7 text #263E36 muted #647168 primary #426B53 border #B9C2B5. Dark: background #181E19 surface #202720 text #E7E9DF muted #B7C3B6 primary #ADCDB0 border #58665A. Same layout in both themes. Accurate legible Russian, practical Compose design, high resolution.
```
