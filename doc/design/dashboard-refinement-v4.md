# Дашборд v4 — «Наши успехи» и микс внутри кнопки

20 октября 2026. Согласовано пользователем и реализовано. [Макет](dashboard-refinement-v4.png), встроенный image_gen, референс dashboard-refinement-v3.png. 

Дополняет и заменяет описание v3 в двух местах:

- Название виджета статистики и диалога: «Наши успехи», согласовано пользователем.
- «Умный микс» — небольшая вторая строка внутри кнопки под «Ня! Начнём?», в левой текстовой колонке. Внешняя подпись удаляется целиком. Полное пояснение про учебники и личный словарь на главной не дублируется. Кот и стрелка остаются справа; тело кнопки 80 dp, выступы кота сверху/снизу сохраняются.

Остальные уточнения v3 сохраняются: компактная аналитика, без легенды на главной, подробности по нажатию в AlertDialog, стрик в заголовке сетки без подписи снизу, более тонкая стрелка, меньший заголовок словарей, тонкий светлый внешний контур кота в тёмной теме. Расстояние до словарей задаётся от видимой нижней границы лап, без лишнего промежутка от удалённой подписи. Растр не задаёт новые действия на неактивных модулях или градиенты.

## Промпт

```text
Edit this three-panel Android dashboard mockup preserving layout and styles. Make these TWO specific changes in ALL panels, including dimmed dashboard:
1 Rename stats title from "Проверено слов" to EXACTLY "Наши успехи". Also rename details dialog title to "Наши успехи". Preserve total1248 and category rows874/249/125. No inline legend on dashboard.
2 Move the smart mix subtitle INSIDE the main start button, as a small second line directly under "Ня! Начнём?" in its LEFT text column. Exact secondary text inside button: "Умный микс". Remove the entire separate line "Умный микс · учебники и мой словарь" below button, without replacing it anywhere. Main greeting remains larger primary line. Left-to-right arrangement: TWO-LINE TEXT COLUMN ("Ня! Начнём?" then "Умный микс"), cat, arrow. Keep button body80dp high. The ivory waving cat MUST still stick outside both top and bottom of button, do not clip it or put it inside text. Keep slight sage silhouette separator in dark theme. Keep arrow scale and thin stroke. Cat and arrow to the right, not behind the text. Reserve safe vertical room for cat feet; move vocabulary card up to use space freed by removing external subtitle.
Preserve flame+"7 дней подряд" as activity HEADER only, 6x7 grid, weekdaysПнСрПт aligned rows1/3/5. Same reduced equal card heights. Existing app name, vocabulary, history and builder unchanged. Preserve details dialog category rows and close action, no new explanatory text on main. Flat solid accepted palette, no gradients. One updated three-panel comparison with light, dark and dialog.
```
