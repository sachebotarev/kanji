# Дашборд — макет v2

20 октября 2026. Предложение для согласования. [Картинка](dashboard-v2.png). Создана встроенным image_gen на основе v1. Реализация не начата.

- Активность: убраны пояснение периода, дни недели и шкала интенсивности; остались сетка за шесть недель и стрик.
- Проверенные слова: вместо трёх строк легенды — одна строка с галочкой, лампочкой и крестиком. Для реализации предлагается открывать расшифровку нажатием и предоставить полные названия для TalkBack.
- Встроенные учебники: показаны количество всех учебников и число активных, без произвольного выделения JLPT N5. Прогресс конкретного учебника находится внутри менеджера.
- Все значения демонстрационные. Карточки аналитики стали компактнее.

## Промпт

```text
Edit the provided Android dashboard mockup into version 2. Preserve Russian language, overall style, warm ivory and forest green palette, exact lower navigation cards and button text. Make the top analytics cards significantly shorter and balanced, moving all subsequent content upward, with comfortable spacing.
1 Activity card: title "Активность", compact authentic 6-column 7-row contribution grid, and small flame "7 дней подряд". REMOVE subtitle "Последние 6 недель", all weekday labels and entire intensity legend "Меньше / Больше". No explanatory text.
2 Checked words card: title "Проверено слов", a smaller three-color donut with center "1 248". REMOVE three stacked legend rows. Replace with ONE compact horizontal row below donut: green checkmark "874", amber lightbulb icon "249", terracotta cross "125". Use a recognizable LIGHTBULB for hints, not a question mark. Tiny icons and legible numbers, all three fit on one line. This yields same compact height as activity.
3 Built-in textbooks row in dictionaries: title "Встроенные учебники"; secondary line "12 учебников · 4 активны". Remove "JLPT N5 · 86 из 120" and remove progress bar entirely. A neutral overview of the entire library, no singled-out book or fake aggregate progress. Keep personal dictionary row unchanged.
Preserve everything else: heading, settings gear, wide CTA and smart mix subtitle, history, constructor, coffee. Single flat full screen portrait, no device frame, no notes outside UI. No excessive whitespace introduced by shrinking cards. Crisp professional practical UI.
```
