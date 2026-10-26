# Единая форма иконок и лапки в аналитике

20 октября 2026. Предложение, ожидает согласования перед реализацией по doc/decisions/ui-design-review.md.

Макет: dashboard-paw-icons-v4.png. Генерация встроенным image_gen.

Шестерёнка сохраняется по прямому пожеланию пользователя. Учебник закрытый компактный с закладкой сверху. Формы и толщины линий четырёх иконок одинаковые между темами; меняется палитра. Тёмные детали насыщеннее. В углах двух аналитических карточек маленькие одноцветные отпечатки лап вместо стрелок; основной запуск сохраняет стрелку. Лапа визуально поддерживает маскота, но хуже стрелки сообщает о переходе — это осознанный компромисс предложения.

Сгенерированный макет ориентировочный: реальные размеры, сетка активности 6×7 и компоновка приложения сохраняются, декоративные градиенты изображения не переносятся.

## Промпт

Use case: ui-mockup. Refine the existing Russian Android kanji dashboard shown in the two reference screenshots. Produce a clean side-by-side LIGHT and DARK mockup of the SAME screen, no device frames, labels Светлая and Тёмная above. Keep exact current layout, typography scale, spacing, texts, numbers, two analytics cards, cat CTA and dictionary card with two rows only, history and builder outside the card. Keep the cat-ear settings gear EXACTLY as reference; user loves it. Keep original mascot and its overhang, the main right-arrow remains an arrow.
Changes: replace ONLY small northeast arrows in the upper right corners of both analytics cards with a tiny simple cat PAW PRINT, 4 rounded toe pads plus one main pad, about 15dp, flat muted forest green in light theme and muted sage in dark. No circle container, no badge, no additional paws elsewhere, not a cat hand. Quiet decorative interactive affordance. Keep header text clear.
Redesign textbook icon: compact nearly square CLOSED upright ivory textbook, rounded corners, coral bookmark peeking from TOP edge and subtle green spine. A modern flat friendly illustration, not old desktop GNOME icon, no open spread, no horizontal stretch, perspective, glossy shading, bevel or volume.
All four row icons must have IDENTICAL geometry, orientation, proportions and stroke weights between light and dark: textbook just described; two cards in a small holder with gold paw; upright scroll with gold clock; three tiles with coral pencil. Only palette changes between themes. Consistent medium contour weight, broad simple details. Dark contour muted medium sage #819F87 single stroke no outer pale rim, no sticker effect; dark fills cream/sage with richer coral and gold details visibly legible. Light contour forest green. All row icons equal visual size 34dp. Dark internal colors slightly richer than reference, not washed out. Flame stays muted terracotta/gold.
Flat app colors: light bg #FAF8F1, surface #FFFDF7, ink #263E36. Dark bg #181E19, surface #202720, text #E7E9DF, CTA #244F32. No gradients, shadows, glow, double outline, added borders around mascot. Text verbatim from screenshots. Preserve layout.
