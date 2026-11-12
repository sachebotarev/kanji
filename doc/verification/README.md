# Проверка релиза

Сводка состояния и ограничений: [релиз 1.0.0](../releases/1.0.0.md).

[Проверка публичной страницы приложения в GitHub Pages](pages/README.md): публикация RU/EN, ссылки политик, адаптивность и кадры.

Действующие проверки: Gradle `check`, целевые JVM/Android-тесты, `check-input-shapes.py`, `check-project-map.py`, `check-recipe-fresh-install.py`, `check-recipe-terminology.py`. Скрипты UI-проверок сохранены; их снимки и результаты генерируются новым прогоном.

Спецификации проверки распознавания — [handwriting](handwriting/); каталог — [catalog](catalog/README.md). Воспроизводимые числовые корпуса и методика — [калибровка](../calibration/README.md). Старые отчёты не подтверждают текущее состояние приложения; архивная история хранится в Git.
