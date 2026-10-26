# Дашборд — полировка v5

20 октября 2026. Согласовано пользователем и реализовано. [Макет](dashboard-polish-v5.png). Встроенный image_gen.

По замечаниям после просмотра dashboard-v4-review-current-pixel.png:

- У карточки «Наши успехи» небольшой знак раскрытия в правом верхнем углу. Новых текстов и легенды на главной нет.
- В диалоге единица счётчика явно названа: «Выполнено заданий: 1248», подпись «Практика кандзи и слов». Это описание демонстрационных данных; реальные правила агрегации будут определяться при подключении статистики. Категории и действие закрытия сохранены.
- Светлая внешняя обводка кота в тёмной теме тоньше и менее контрастна, без эффекта наклейки. Увеличен зазор от видимой поднятой лапы до стрелки. Основная композиция кнопки, тело80dp, две строки слева и выступы персонажа сохраняются.
- Словари остаются основной карточкой. История и конструктор — компактные плоские строки, с разделителем между ними, без отдельных крупных рамок. Эти заглушки не получают новых действий.
- Поддержка — тихая строка с маленькой чашкой, без фона, рамки и жирного текста.
- Пламя заменяется отдельным прозрачным PNG [streak-flame-v1.png](streak-flame-v1.png), созданным встроенным image_gen. Пример в малом размере показан на обеих темах макета. Ориентир отображения16dp с учётом оптического размера и системного масштаба текста. Без tint, сохраняются кораллово-оранжевый и золотистый цвета.

Изображение не задаёт градиенты и случайные изменения цвета; сохраняются принятые плоские токены темы. Бизнес-логика модулей остаётся за пределами задачи.

## Промпт PNG-пламени

```text
Use case illustration-story. Asset type tiny Android daily-streak PNG icon. Create ONE small friendly flame symbol on a genuinely transparent background. It must be legible when displayed at16dp alongside text. Soft simple rounded silhouette, warm coral-orange outer flame and warm golden-yellow small inner droplet. Exactly TWO broad flat color shapes, with a subtly darker orange contour if necessary, no dark green/black heavy outline. A gently leaning main flame tip with one modest rounded side curl, compact balanced upright proportions, no spikes, no jagged cuts, no sinister shape. No face, eyes, smile, letters, numbers, text, background, tile, circle, glow, gradients, shadows or texture. Polished cozy flat mascot-adjacent UI illustration for a Japanese-learning app with ivory-cat mascot, forest-green themes. Must contrast cleanly on BOTH warm ivory #FAF8F1 and dark green #181E19. Centered icon fills about80 percent of square with even transparent margin. Not an emoji render, not3D, not photoreal. One final standalone alpha PNG asset.
```

## Промпты макета

```text
Use case ui-mockup. Refine this exact real Android dashboard screenshot, preserving accepted layout and mascot identity. Create THREE panels side by side: light dashboard, dark dashboard, dark details dialog over dimmed dashboard. Screenshot-like, no device frames.
Make ALL the following targeted changes:
A Main stats card "Наши успехи" keeps title, donut and total1248, NO LEGEND on dashboard. Add a tiny understated northeast expand arrow icon at top-right of this card header, visually indicating it opens details. Do NOT add category labels or inline legend. Same compact equal analytics card heights, streak flame+"7 дней подряд" in activity header, weekdays Пн Ср Пт on rows1 3 5 and 6x7grid.
B Main CTA stays beneath analytics,80dp colored body, left text "Ня! Начнём?" and smaller "Умный микс" inside, same waving ivory cat in middle-right protruding both TOP and BOTTOM, arrow at far right. Add clear breathing room12dp between visible raised paw and arrow; avoid them touching or overlapping. Keep text comfortable. Cat may move slightly left or be5percent smaller if needed. Arrow40dp and thin stroke. Dark mascot outer keyline must become almost imperceptible, about0.5dp muted sage with low contrast, smooth and consistent, NO white sticker border, NO double-outline effect, no glow. Light mascot outline unchanged. Preserve exact cap, red band, gold badge and all character colors.
C Hierarchy of lower modules: Dictionary remains the primary outlined full card with book and personal rows and existing numbers. HISTORY and BUILDER become smaller simpler flat rows below, with their leading icons and title/subtitle and a fine divider BETWEEN the two rows. Remove separate oversized rounded outline cards around these two rows. Do NOT add chevrons or pretend these placeholder rows navigate. Keep content "История сессий" / "Результаты и архив ошибок", "Конструктор тренировок" / "Активный шаблон: Умный микс".
D Project support becomes a quiet centered text-style row at bottom with small coffee outline icon and "Купить автору кофе". NO enclosing card, NO large border, no fill, not primary bold, secondary text color. Less prominent than learning modules.
E Dialog pane: title "Наши успехи"; total line EXACTLY "Выполнено заданий: 1248"; small explanation "Практика кандзи и слов". Three category rows with colored dots: "Освоено"874, "С подсказкой"249, "С ошибками"125; action "Закрыть". No new main-screen labels.
Keep app title"Кандзи дэс нэ", gear and accepted sizes; no other redesign. Colors solid: light bg#FAF8F1 surface#FFFDF7 ink#263E36 primary#426B53 button#DDEDD8; dark bg#181E19 surface#202720 text#E7E9DF primary#ADCDB0 button#244F32 borders#58665A. No gradients, textures or shadows. High fidelity Russian typography, realistic412dp width. Small outside labels Светлая / Тёмная / Подробнее.

Refine image1 with THREE exact fixes only.
1 Replace the tiny flame in every streak header with the EXACT standalone orange/gold flame icon from image2, displayed small at16dp alongside "7 дней подряд", optically centered. Preserve shape and colors, do not turn into emoji or recolor. Show its appearance on light AND dark backgrounds.
2 Restore accepted LIGHT CTA palette: pale sage #DDEDD8 background with dark green #263E36 text and arrow. No dark saturated green light-theme button. DARK CTA remains #244F32 with pale text.
3 Simplify the dialog back to the accepted compact Material AlertDialog. REMOVE the EXTRA donut chart inside dialog entirely. Title "Наши успехи"; line "Выполнено заданий: 1248"; small subtitle "Практика кандзи и слов"; three rows colored dots Освоено874 / С подсказкой249 / С ошибками125. One bottom-right text-only action "Закрыть", NOT full-width filled button. No extra X close icon in header. Keep dashboard donut unchanged and only present behind dialog.
Everything else unchanged: small expand icon on stats, NO legend on dashboard, cat with very subtle keyline and more space to arrow, no outlined cards for history/builder, understated coffee row. Same three panels light/dark/details, solid fills no gradients, high fidelity Russian text.
```
