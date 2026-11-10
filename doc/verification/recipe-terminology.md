# Терминология рецептов и тренировок

## Действующий контракт

[Глоссарий](../glossary.md) обязателен для кода, базы, Android UI, HTML и текущих спецификаций:

- **Recipe / рецепт** — сохранённая конфигурация: названия с `language_tag`, материалы, параметры, выбранный рецепт и порядок.
- **Training / тренировка** — выполнение по рецепту. Задание — отдельный шаг выполнения.
- Исполнитель рецептов, сохранение тренировок и статистика этим рефакторингом **не добавлены**. Существующий рукописный прототип использует `HandwritingTraining`; демонстрационная история не становится реальным хранилищем выполнений.

Домен использует `Recipe`, `RecipeMaterial`, `RecipeName`, `RecipesRepository`. Android использует `RecipeOverview`, `RecipeBuilderRoute`, `CurrentRecipeWidget`; количество слов задаётся на тренировку (`wordsPerTraining`). HTML использует `recipes`, `recipe-editor`, `recipe-detail`, `recipe-source`, `recipe-builder`, `recipes.js/css`; выбранный рецепт и новая тренировка различаются в RU/EN.

Технические `sessionStorage`, drag-and-drop-сеансы и временные сеансы выбора не являются бизнес-сущностью Training. Канонические идентификаторы экранов — `RECIPE.*` (миграция с `TRAIN.*` 2026-11-06). Исторические отчёты, изображения, манифесты и экспорты Room 1–5 сохраняют исходные свидетельства, а не изображают повторную проверку.

## Room 7: чистая установка

Единая база приложения — `kanji.db`, 29 таблиц. Имена агрегата рецептов:

- `recipes`, `recipes_translations`;
- `recipe_materials`;
- `recipe_materials_chapters`, `recipe_materials_kanji`, `recipe_materials_words`, `recipe_materials_tags`.

Исполнение тренировок (отдельно от рецептов): `trainings`, `trainings_recipe_translations`, `training_tasks`, `training_task_translations`, `training_task_symbols` — [trainings.md](../database/trainings.md).

`recipe_id` ссылается на рецепт; `material_id` — на материал. Настоящий `recipe_materials.source_id` остаётся ID каталожного источника, не переименовывается в ID материала. Переводы отделены и используют `language_tag`, русский остаётся языком подстановки. Entity/DAO/@Relation, PK/FK, индексы, проверки, транзакции и справочные DDL согласованы с этими именами. Миграций, старых таблиц и совместимых представлений нет. Сохранён существующий предрелизный destructive fallback; старая база не переносится.

[Экспорт схемы](../../data/catalogdb/schemas/ru.mobui.kanji.data.catalog.CatalogDatabase/7.json). Историческая [проверка чистой установки v6](recipe-terminology-fresh-install.json) описывает тогдашнюю версию и не переписывается.

## Проверки

- Сборка приложения, тестовых APK, JVM-тесты и lint catalogdb/dashboard/dictionary успешны.
- JVM: domain:catalog — 34, domain:practice — 13, dashboard — 4; всего 51, без ошибок.
- Android: хранилище — 9 тестов; словарь — 34 (96,975 с); выбранные 66 тестов конструктора — 66 (210,936 с). Последний набор явно исключает два неисправных drag-теста, это **не зелёный полный прогон**.
- Справочный SQL — 6 тестов; проверка общей формы ввода — 17 мест вызова; [терминологический валидатор](check-recipe-terminology.py) проверяет доменные имена, схему/связи и RU/EN.
- HTML: 39 канонических экранов и старые ссылки, RU/EN, shared selectors; каталог — 18 вариантов. [Отдельный браузерный сценарий](recipe-terminology.cjs) проверяет Recipes → Create recipe и Selected recipe → New training в RU/EN, без JS-ошибок. Используется имеющийся Playwright/Chrome, зависимости не обновлялись.

Существующие тесты адаптированы к уже изменённому отдельной доработкой графу: после редактирования возврат идёт в карточку рецепта, а заголовок расположен в дочерней кликабельной области. В новой карточке исправлено локальное нарушение lint: состояние меню запоминается, а не создаётся при каждой композиции.

## Незакрытые проверки рабочей копии

Два native drag-теста полного набора (68 тестов) остаются неисправны: `longPressDragCommitsAndCancellationDoesNotCommit` и `platformDragAutoScrollsBeyondInitialViewportAndCommitsOnce`. Источник штатного переноса расположен на родителе, а новая кликабельная область карточки потребляет Down; штатный long-press не начинает перенос. Обработка жестов не переписывалась ради терминологического рефакторинга, experimental API не вводились. Предыдущие успешные drag-отчёты не доказывают исправность текущей карточки.

Выведенные ID (`RECIPE.02`, `RECIPE.02.01`, бывшие `TRAIN.01.02.02`/`TRAIN.01.02.03` и виджеты) учитываются в `check-project-map.py` как история, а не как живые виджеты; `screens.json` допускает статус `retired`.

## Воспроизведение

```bash
python3 doc/verification/check-recipe-terminology.py
python3 doc/database/test_builder_schema.py
python3 doc/verification/check-input-shapes.py
# После чистой установки на выделенный эмулятор:
python3 doc/verification/check-recipe-fresh-install.py --serial emulator-5554
# Для браузерных проверок сервер должен обслуживать doc/ на 8873:
NODE_PATH=/Users/chebotarev/.npm/_npx/360550e4913b8759/node_modules node doc/verification/recipe-terminology.cjs
```
