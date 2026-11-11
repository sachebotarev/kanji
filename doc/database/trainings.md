# Тренировки: модель хранения выполнения

Статус: **схема и host PRACTICE подключены**. Room 1 (`CatalogDatabase`); DDL — [trainings.sql](trainings.sql); экспорт — [1.json](../../data/catalogdb/schemas/ru.mobui.kanji.data.catalog.CatalogDatabase/1.json). Domain: `TrainingsRepository` в `:domain:practice`; реализация — `RoomTrainingsRepository` в `:data:catalogdb`. Очередь слов — `shuffledCycles` (`shuffled_cycles`) из рецепта; host пишет прогресс на старт / границу задания / interrupt / complete, без записи на каждый штрих (`TrainingPersistence` на IO). Resume UI и история тренировок — отдельные задачи. Все последующие изменения требуют миграций с сохранением данных.

Термины: [глоссарий](../glossary.md). Рецепт хранит конфигурацию ([recipes.md](recipes.md)); тренировка — конкретное выполнение. Экран одного задания: [practice-screen.md](../implementation/practice-screen.md). Проверки формы: [shape-checker.md](../implementation/shape-checker.md).

## Иерархия

```text
trainings
  ├─ trainings_recipe_translations
  └─ training_tasks
       ├─ training_task_translations
       └─ training_task_symbols
```

Имена таблиц и сущностей — `trainings` / `training_*` по глоссарию; префикс `practice_*` не использовать.

## Решения

- Незавершённая тренировка хранится со статусом `in_progress` для возобновления.
- Одновременно не больше одной `in_progress` (частичный UNIQUE-индекс).
- Старт новой тренировки в одной транзакции: текущую `in_progress` → `interrupted` с `ended_at`, затем insert новой.
- Рукописный ввод — только колонка `writing_sample_json` на символе. Отдельных таблиц штрихов/точек нет.
- Незавершённый жест не сохраняется.
- Счётчик подсказки по знаку — на символе (`hint_request_count`). Раскрытие чтения и перевода слова — флаги `reading_hint_used` и `translation_hint_used` на задании. Статус `assisted`, если счётчик > 0 у practice-символа или открыта подсказка в шапке.
- Учебные тексты и названия рецепта копируются снимками с `language_tag`; русские значения — fallback при чтении.

## `trainings`

| Field | SQL type | Назначение |
| --- | --- | --- |
| `id` | `TEXT` | PK, UUID тренировки. |
| `recipe_id` | `TEXT` | FK → `recipes(id)` ON DELETE SET NULL; может стать NULL, если рецепт удалили. |
| `recipe_revision` | `INTEGER` | Ревизия рецепта на старте. |
| `task_count` | `INTEGER` | Снимок параметра рецепта (число заданий или NULL). |
| `seconds_per_symbol` | `INTEGER` | Снимок бюджета секунд на practice-символ или NULL; лимит слова = значение × число practice-символов. |
| `status` | `TEXT` | `in_progress` \| `completed` \| `interrupted`. |
| `started_at` | `INTEGER` | Epoch ms начала. |
| `ended_at` | `INTEGER` | Epoch ms окончания; NULL только при `in_progress`. |

### `trainings_recipe_translations`

Снимок названий рецепта на старте. PK (`training_id`, `language_tag`). Нет колонок ru/en в `trainings`.

| Field | SQL type | Назначение |
| --- | --- | --- |
| `training_id` | `TEXT` | FK → `trainings(id)` ON DELETE CASCADE. |
| `language_tag` | `TEXT` | BCP 47. |
| `name` | `TEXT` | Название 1–120 символов. |

## `training_tasks`

Задание — слово, выражение или предложение в очереди тренировки.

| Field | SQL type | Назначение |
| --- | --- | --- |
| `id` | `TEXT` | PK, UUID задания. |
| `training_id` | `TEXT` | FK → `trainings(id)` ON DELETE CASCADE. |
| `position` | `INTEGER` | Порядок в очереди ≥ 0; UNIQUE(`training_id`, `position`). |
| `word_id` | `INTEGER` | FK → `words(id)` ON DELETE SET NULL. |
| `written_form` | `TEXT` | Снимок написания. |
| `reading` | `TEXT` | Снимок японского чтения. |
| `status` | `TEXT` | `pending` \| `in_progress` \| `correct` \| `assisted` \| `skipped` \| `timed_out`. |
| `started_at` | `INTEGER` | Начало работы над заданием или NULL. |
| `ended_at` | `INTEGER` | Окончание или NULL. |
| `elapsed_ms` | `INTEGER` | Накопленное время слова ≥ 0. |
| `reading_hint_used` | `INTEGER` | `0/1`: раскрыт spoiler произношения на этом задании. |

Терминальные статусы совпадают с доменным `AnswerStatus`; `pending` / `in_progress` нужны для возобновления. При `interrupted` у тренировки незакрытые задания остаются в последнем `pending` / `in_progress` (не авто-`skipped`). Оценка балла слова — [hint-scoring.md](../implementation/hint-scoring.md).

### `training_task_translations`

Снимок переводов условия на постановку в очередь. PK (`task_id`, `language_tag`).

| Field | SQL type | Назначение |
| --- | --- | --- |
| `task_id` | `TEXT` | FK → `training_tasks(id)` ON DELETE CASCADE. |
| `language_tag` | `TEXT` | BCP 47. |
| `translation` | `TEXT` | Локализованное условие задания. |

## `training_task_symbols`

Символ позиции в написании задания.

| Field | SQL type | Назначение |
| --- | --- | --- |
| `id` | `TEXT` | PK, UUID символа. |
| `task_id` | `TEXT` | FK → `training_tasks(id)` ON DELETE CASCADE. |
| `position` | `INTEGER` | Порядок в написании ≥ 0; UNIQUE(`task_id`, `position`). |
| `occurrence_id` | `TEXT` | Стабильный id позиции внутри задания (как в PRACTICE). |
| `role` | `TEXT` | `practice` — нужен ввод; `display` — без ввода (кана, показанный кандзи). |
| `script` | `TEXT` | `kanji` \| `hiragana` \| `katakana` \| `other`. |
| `glyph` | `TEXT` | Отображаемый глиф. |
| `kanji_id` | `INTEGER` | FK → `kanji(id)` ON DELETE SET NULL при наличии в реестре. |
| `kanji_key` | `TEXT` | Ключ эталона/чекера (например `u56f3`). |
| `status` | `TEXT` | `empty` \| `editing` \| `correct` \| `incorrect` \| `prefilled`. |
| `hint_request_count` | `INTEGER` | Число **засчитанных** запросов подсказки по символу ≥ 0; каждое платное включение +1, выключение и свободный peek на `correct` не уменьшают/не увеличивают. |
| `elapsed_ms` | `INTEGER` | Время на символ ≥ 0. |
| `readability_verdict` | `TEXT` | Вердикт читаемости или NULL. |
| `readability_glyph` | `TEXT` | Распознанный знак или NULL. |
| `shape_verdict` | `TEXT` | Вердикт формы или NULL; `accepted_with_penalty` — форма принята с фиксированным штрафом 20%. Новое значение разрешено только для формы. |
| `shape_similarity_percent` | `REAL` | **Сходство** итогового рисунка с эталоном, %. Не вероятность правильного ответа. Код: `matchPercent`. |
| `shape_budget_percent` | `REAL` | **Общий расход бюджета** компенсаций, %. Степень применённых поправок формы. Код: `compensation.budget.totalPercent`. |
| `stroke_writing_verdict` | `TEXT` | Вердикт порядка/направления черт или NULL. |
| `stroke_expected` | `INTEGER` | Ожидаемое число черт или NULL. |
| `stroke_actual` | `INTEGER` | Фактическое число завершённых черт или NULL. |
| `writing_sample_json` | `TEXT` | JSON-снимок `WritingSample`; NULL у `display` или пустого ввода. |
| `check_report_json` | `TEXT` | Последний применённый развёрнутый отчёт проверки или NULL. |

Вердикты проверок: `correct` \| `incorrect` \| `incomplete` \| `unavailable`. `NULL` — проверка выключена, не применимо (`display`) или результата ещё не было.

Канон терминов формы — [shape-checker.md](../implementation/shape-checker.md): среди кандидатов выбираются меньший **бюджет**, затем большее **сходство**.

### Формат `writing_sample_json`

Координаты в квадрате 0…1 (начало сверху слева, X вправо, Y вниз), как в `:domain:handwriting`. Массив штрихов в порядке написания:

```json
[
  {
    "tool": "finger",
    "points": [
      { "x": 0.2, "y": 0.3, "elapsed_ms": 0 },
      { "x": 0.8, "y": 0.3, "elapsed_ms": 100 }
    ]
  }
]
```

`tool`: `finger` \| `stylus` \| `mouse` \| `unknown`. У первой точки штриха `elapsed_ms = 0`, далее не убывает. Пустой массив — пустой ввод. Реляционных таблиц точек нет: статистика по знакам берётся из колонок вердиктов, процентов и времени.

## Жизненный цикл

1. Создание: очередь `training_tasks` со `status=pending`, полный набор символов (`display` → `prefilled`, `practice` → `empty`), снимки переводов и названий рецепта.
2. Resume: единственная `in_progress`; восстановить текущее задание/символ и ink из JSON.
3. Старт другой тренировки: прежняя → `interrupted`; незакрытые задания не переводятся в `skipped`.
4. Успешный проход всех заданий → `completed` с `ended_at`.
5. Статистика опирается на снимки и вердикты; живые `word_id` / `recipe_id` / `kanji_id` могут стать NULL.

## Границы этапа

Room-хранилище, генератор очереди (`shuffled_cycles`) и персистентный host PRACTICE подключены. Очистка всей истории — `TrainingsRepository.clearHistory` из настроек (SETTINGS.W04 / SETTINGS.D01). Список истории и восстановление подключены; Room-проверки — инструментальные тесты `TrainingsStorageTest`.

Исходная схема 1 содержит ограничения вердиктов и частичные индексы; будущие изменения требуют миграций. [Решение](../decisions/shape-soft-acceptance.md).
