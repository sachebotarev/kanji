# Уплотнение главной и статистика по нажатию

20 октября 2026. Предложение для согласования. [Макет](dashboard-refinement-v1.png), встроенный image_gen по референсу dashboard-review-current-pixel.png. Код не изменён.

- Аналитика примерно на 20% ниже; круговая диаграмма уменьшена. Легенда с иконками и числами на главной полностью удаляется, текстовая замена не добавляется.
- Нажатие на всю карточку «Проверено слов» открывает обычный Material 3 AlertDialog с общим числом и тремя строками: «Освоено», «С подсказкой», «С ошибками». Данные остаются демонстрационными. Кнопка «Закрыть», Back и нажатие снаружи закрывают диалог.
- Кнопка запуска сохраняет согласованный порядок: текст, кот, стрелка; кот выступает сверху и снизу. Стрелка имеет более тонкий штрих, подпись микса ближе к лапам, без пересечения.
- В тёмной теме предлагается тонкая приглушённая светло-зелёная обводка внешнего контура кота. Не белая наклейка, свечение или тень. В светлой теме исходный контур сохраняется.
- Заголовок словарей меньше и ближе по иерархии к содержимому.

Генерация не вводит новые стрелки у пока неактивных модулей и градиенты заливок. Сохраняются плоские цвета существующей темы и независимые виджеты. Подробности статистики оформляются отдельным stateless-компонентом, состояние открытия принадлежит экрану.

## Промпт

```text
Use case ui-mockup. Design a restrained refinement of this actual Russian Android dashboard. Output one image with THREE panels side by side: updated LIGHT dashboard, updated DARK dashboard, and DARK statistics-details dialog over dimmed dashboard. High resolution legible practical UI, no phone frames.
Preserve exact app character, content and navigation; change only:
1 Analytics cards become ~20 percent shorter. Left "Проверено слов": smaller donut center "1248". REMOVE entire inline legend below donut, including all 874/249/125 icons and numbers. Absolutely NO replacement legend text on dashboard. Card itself opens details. Right "Активность": keep 6-column7-row grid with Пн Ср Пт at rows1 3 5 and small flame "7 дней подряд"; fit same reduced card height as left. Both balanced and same height.
2 BELOW analytics keep the unified button in exact LEFT TO RIGHT order: "Ня! Начнём?" / waving cat / large arrow. Cat protrudes BOTH above and below button. Same existing exact ivory cat, cap red stripe gold badge. In dark theme ONLY add extremely thin muted light sage keyline around OUTER silhouette to separate cap and tail from dark background. No thick white sticker border, no glow, no shadow. Light theme retains dark outer silhouette.
3 Arrow remains40dp bounding but stroke becomes moderately thinner and lighter, visually matching UI outlines, not heavy.
4 Subtitle "Умный микс · учебники и мой словарь" remains a SEPARATE line below button, but closer to cat feet (8dp minimum safe gap). Don't overlap cat.
5 "Словари / 単語" heading smaller, around titleMedium20sp, closer to row hierarchy. Existing textbook and personal dictionary rows and history/builder stay.
Dark dialog panel: standard Material3 alert dialog titled "Проверено слов", total line "Всего 1248", then THREE clearly labeled rows with small colored circular marks, labels and right-aligned counts: "Освоено" 874 in sage, "С подсказкой" 249 in amber, "С ошибками"125 in salmon; bottom right text action "Закрыть". These category labels appear ONLY in this dialog. Dim dashboard behind dialog. No bottom sheet, no extra charts, no invented actions.
Colors strictly flat fills: light bg#FAF8F1 surface#FFFDF7 text#263E36 primary#426B53 light button#DDEDD8. Dark bg#181E19 surface#202720 text#E7E9DF primary#ADCDB0 button#244F32 borders#58665A. No gradients,textures,unrequested large typography. Small outside labels Светлая / Тёмная / По нажатию. Compact clean screenshot-like screen crops through dictionaries/history.
```
