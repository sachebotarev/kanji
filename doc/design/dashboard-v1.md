# Дашборд — макет v1

20 октября 2026. Статус: предложение для согласования, не реализовано.

Картинка: [dashboard-v1.png](dashboard-v1.png). Создана встроенным image_gen.

Состав: статистика и активность за шесть недель в одном ряду; широкая кнопка запуска умного микса; единая карточка учебников и личного словаря; история сессий; конструктор с активным шаблоном; внешняя ссылка поддержки. Значения демонстрационные. Палитра основана на UI-STYLE. При реализации на небольших экранах нужна вертикальная прокрутка. Подписи дней сетки требуют корректного выравнивания по строкам: понедельник — первая, среда — третья, пятница — пятая. Сгенерированный растр не является точной спецификацией размеров.

Переход к реализации — после согласования изображения пользователем согласно decisions/ui-design-review.md.

## Промпт генерации

```text
Use case: ui-mockup
Create a polished high fidelity Android app HOME DASHBOARD mockup for a kanji learning app, Russian interface. One single flat straight-on screen, portrait 9:19.5, no device frame, no perspective, no surrounding presentation, no annotations. Reference logical width 412dp, readable Russian typography, realistic Material 3 spacing. Warm paper #FAF8F1, ink #263E36, forest green #426B53, white ivory cards #FFFDF7, subtle thin sage borders, restrained rounding, no shadows, no gradients, no illustrations.
Layout top to bottom:
Android status bar 9:41. Header large "Главная", small subtitle "Каждый день — ближе к японскому", settings gear at right.
ONE horizontal row of two equal-width analytics cards. Left title "Проверено слов", clear donut with three sectors green 70%, amber 20%, terracotta 10%, center big "1 248", compact labeled legend beneath "Освоено 874" / "С подсказкой 249" / "С ошибками 125". Right title "Активность", subtitle "Последние 6 недель", authentic GitHub contribution grid EXACTLY 6 columns by 7 rows tiny rounded squares in four green intensities, day labels Пн Ср Пт on left, then a flame icon and "7 дней подряд", miniature low/high intensity legend.
Full-width green primary button play icon "Начать тренировку". Below it small centered text "Умный микс · учебники и мой словарь".
Section card "Словари / 単語" with chevron, divided internally by a hairline into two distinct rows: book icon "Встроенные учебники" and second line "JLPT N5 · 86 из 120" with slim green progress track; personal cards icon "Мой словарь", second line "Всего 156 · изучено 92", third small line "18 с ошибками · 12 с подсказкой".
Compact navigation card history icon "История сессий", secondary "Результаты и архив ошибок", right chevron.
Compact navigation card sliders icon "Конструктор тренировок", secondary "Активный шаблон: Умный микс", right chevron.
Near bottom quieter outlined small coffee action "Купить автору кофе" and external link icon. Android gesture pill bottom.
Everything fits comfortably with breathing room, clear hierarchy, no bottom tab bar. Accurate crisp Russian text, professional feasible Compose interface. All numbers illustrative. Do not insert extra decorative Japanese writing or extra widgets.
```

