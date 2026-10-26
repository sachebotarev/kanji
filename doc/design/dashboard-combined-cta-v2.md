# Единая кнопка запуска — v2

20 октября 2026. Согласовано пользователем и реализовано.

[Картинка двух тем](dashboard-combined-cta-v2.png), встроенный image_gen. Референсы: dashboard-combined-cta-v1.png и dashboard-mascot-wave-v1.png.

Порядок: заголовок → статистика и активность → объединённая кнопка → отдельная подпись «Умный микс · учебники и мой словарь» → словари и остальные модули. Внутри кнопки слева направо: «Ня! Начнём?», кот, крупная стрелка. Второго текста «Начать тренировку» нет. Вся кнопка вызывает существующий запуск, имеет единый accessibility label «Начать тренировку».

Кот выступает над верхней и под нижней границей фоновой плашки. Пространство под выступы резервируется, соседние элементы не перекрываются. Ориентиры: тело кнопки 64 dp, видимый кот 100 dp, область стрелки 40 dp с рисунком около 32 dp. Стрелка одна, без отдельной кнопки или кружка. Точные размеры адаптируются к экрану и системному шрифту.

Сохраняется предложение исправить читаемость цельных иконок статистики и активности. Растр не задаёт новые стрелки у неактивных словарей и градиенты; используются существующие сплошные цвета темы.

## Промпт

```text
Revise this dashboard proposal precisely according to these requirements. TWO themes side by side. Keep existing cat identity and app palette.
New vertical order in BOTH screens:
1 app title "Кандзи дэс нэ" and gear.
2 the two analytics cards side by side, exactly as existing (statistics donut and activity with weekdays).
3 BELOW analytics, ONE horizontal start BUTTON shaped like a shallow rounded pill/rectangle, around 64dp colored body, NOT a tall card. Its contents LEFT TO RIGHT on ONE line: text "Ня! Начнём?" at left; the waving cat in middle-right; LARGE right arrow at far right. No other text inside button. NO "Начать тренировку" label anywhere. Button full width.
Cat visual height about 100dp, button colored body about64dp. Cat is centered vertically across button body so hat sticks out ABOVE the colored top edge about18dp and feet/tail stick out BELOW bottom edge about18dp. Both top and bottom protrusion MUST be clearly visible, not clipped. Reserve enough space around button to avoid touching charts or subtitle. Cat should NOT sit fully inside rectangle. Don't crop head/feet. Do not put cat at far right, arrow must be to its right with breathing room.
Right arrow: confident thick rounded Material arrow with shaft, graphic height around32dp, bounding box40dp. Visually substantial and harmonious beside the100dp cat; not a tiny text arrow or chevron. Arrow remains within colored button body and optically centered. In light theme use dark green arrow and text; dark theme use pale sage arrow/text against deep green button. Avoid separate circular arrow button.
4 BELOW button and cat feet, separate centered subtitle EXACTLY "Умный микс · учебники и мой словарь", small muted text. NOT inside button. Same subtitle as original app.
5 vocabulary card unchanged below.
Keep stats footer symbols readable and proportionate, counts874/249/125, total1248 and activity7дней. Light #FAF8F1 / #FFFDF7 / #263E36 / #DDEDD8 button. Dark #181E19 / #202720 / #E7E9DF / #244F32 button. Flat solid colors, NO gradients or shadows. No duplicated greeting card above statistics, no separate start button elsewhere. Crisp Russian typography. One comparison image of realistic Android layout. Small outside labels Светлая / Тёмная.
```
