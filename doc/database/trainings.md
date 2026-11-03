# Тренировки: модель хранения и сессий

Статус: проект для согласования. [SQL](trainings.sql) описывает структуру таблиц в `learning.db` (рецепты, источники и история сессий). [Интерактивный макет](../design-system/prototypes/web/index.html#trainings) реализует конструктор в памяти браузера.

---

## 1. Концепция и состав тренировки

Тренировка представляет собой именованный **рецепт выборки** учебных записей (слов, выражений, предложений) и параметры проведения сессии:
- `task_count` (целевое число заданий сессии): целое положительное число или `NULL`. Значение `NULL` означает ровно один проход по всем уникальным словам набора. Заданное число может быть как меньше, так и существенно больше размера набора (слова повторяются).
- `seconds_per_word` (таймер на слово): целое положительное число секунд или `NULL` (без ограничения времени). Таймер действует на всё слово целиком (все иероглифы слова), а не сбрасывается для каждого знака.
- `selection_mode` (способ выборки при заданном числе заданий):
  - `shuffled_cycles` (по кругу): перемешивание набора, повторный проход в новом случайном порядке. Гарантируется отсутствие одинаковых соседних слов на стыке циклов (при размере набора > 1).
  - `random_with_replacement` (случайно с возвращением): независимый равновероятный выбор слова для каждого задания.

### Правила фильтрации источников

Рецепт содержит упорядоченный список источников (`training_sources`).
- Фильтры **внутри одного источника** соединяются логическим **И**.
- Разные источники объединяются через **ИЛИ** с последующей дедупликацией по `words.id`.

| Поле источника | Назначение |
| --- | --- |
| `source_id` | Учебник (`source_type = 'textbook'`), книга или личный словарь (`source_type = 'personal'`) из таблицы `sources`. `NULL` — все доступные источники (каталог + личный словарь). |
| `chapter_from`, `chapter_to` | Включительный диапазон глав внутри выбранного учебника. Применимо только для учебников (`source_id IS NOT NULL`). При `NULL` в обеих границах — все главы; при одной границе — открытый интервал. |
| `word_id` | Конкретное слово (из каталога или личного словаря); `NULL` — все подходящие. |
| `kanji_id` | Записи, в написании которых присутствует данный знак; `NULL` — любой знак. |
| `jlpt_level` | Записи, содержащие хотя бы один кандзи указанного уровня (1–5); `NULL` — любой уровень. |

JLPT относится к классификации **кандзи**, а не к словарному уровню. Совпадение определяется по иероглифам японского написания записи (`words.written_form`), а не только по связям `source_words`.

---

## 2. Схема базы данных

Полный DDL представлен в [`trainings.sql`](trainings.sql).

### Таблица рецептов `trainings`
- `id` (`TEXT PRIMARY KEY`): UUID рецепта.
- `task_count` (`INTEGER`, nullable): число заданий (`CHECK(task_count > 0)`).
- `selection_mode` (`TEXT NOT NULL DEFAULT 'shuffled_cycles'`): режим выборки.
- `seconds_per_word` (`INTEGER`, nullable): секунды на слово (`CHECK(seconds_per_word > 0)`).
- `revision` (`INTEGER NOT NULL DEFAULT 1`): монотонно растущая ревизия для защиты от параллельных правок.
- `is_archived` (`INTEGER NOT NULL DEFAULT 0 CHECK(is_archived IN (0,1))`): флаг мягкого удаления, единообразный с `words` и `sources`.
- `created_at`, `updated_at` (`INTEGER NOT NULL`): таймстемпы UTC в миллисекундах.
- `archived_at` (`INTEGER`, nullable): таймстемп архивации.

### Таблица переводов названий `trainings_translations`
- `training_id` (`TEXT NOT NULL REFERENCES trainings(id) ON DELETE CASCADE`).
- `language_tag` (`TEXT NOT NULL COLLATE NOCASE`): тег языка BCP 47 (`ru`, `en`).
- `name` (`TEXT NOT NULL CHECK(length(trim(name)) BETWEEN 1 AND 120)`).
- `PRIMARY KEY(training_id, language_tag)`.

**Правила разрешения названий:**
1. Предустановленные системные рецепты (каталог): `запрошенный язык → русский → плейсхолдер отсутствия перевода`.
2. Пользовательские рецепты: `запрошенный язык → русский → первое существующее название автора`. Пользовательское название, введённое на одном языке, не скрывается заглушкой «Нет перевода» при смене языка интерфейса.

### Таблица источников `training_sources`
- `id` (`TEXT PRIMARY KEY`): UUID фильтра.
- `training_id` (`TEXT NOT NULL REFERENCES trainings(id) ON DELETE CASCADE`).
- `position` (`INTEGER NOT NULL CHECK(position >= 0)`): порядок отображения в редакторе.
- `source_id` (`INTEGER REFERENCES sources(id) ON DELETE RESTRICT`).
- `chapter_from`, `chapter_to` (`INTEGER`): диапазон глав.
- `word_id` (`INTEGER REFERENCES words(id) ON DELETE RESTRICT`).
- `kanji_id` (`INTEGER REFERENCES kanji(id) ON DELETE RESTRICT`).
- `jlpt_level` (`INTEGER CHECK(jlpt_level BETWEEN 1 AND 5)`).
- `UNIQUE(training_id, position)`.
- Индексы: `idx_training_sources_training_id`, `idx_training_sources_source_id`, `idx_training_sources_word_id`, `idx_training_sources_kanji_id`.

### Таблицы выполнения сессий и истории
- `training_sessions`:
  - `id` (`TEXT PRIMARY KEY`): UUID сессии.
  - `training_id` (`TEXT REFERENCES trainings(id) ON DELETE SET NULL`): рецепт-источник (при удалении рецепта история остаётся).
  - `recipe_revision` (`INTEGER`): снимок ревизии на момент запуска.
  - `task_count` (`INTEGER NOT NULL CHECK(task_count > 0)`): зафиксированное число заданий.
  - `seconds_per_word` (`INTEGER`): лимит времени на слово.
  - `selection_mode` (`TEXT NOT NULL`): применённый способ выборки.
  - `status` (`TEXT NOT NULL CHECK(status IN ('in_progress','completed','abandoned'))`).
  - `started_at`, `completed_at` (`INTEGER`): таймстемпы начала и завершения.
- `training_session_items`:
  - `id` (`TEXT PRIMARY KEY`): UUID задания.
  - `session_id` (`TEXT NOT NULL REFERENCES training_sessions(id) ON DELETE CASCADE`).
  - `position` (`INTEGER NOT NULL CHECK(position >= 0)`): порядковый номер в сессии.
  - `word_id` (`INTEGER NOT NULL REFERENCES words(id) ON DELETE RESTRICT`): учебное слово.
  - `status` (`TEXT NOT NULL CHECK(status IN ('pending','correct','assisted','skipped','timed_out'))`).
  - `elapsed_ms` (`INTEGER NOT NULL DEFAULT 0`): фактически затраченное время в миллисекундах.
  - `hint_count` (`INTEGER NOT NULL DEFAULT 0`): количество использованных подсказок.
  - `UNIQUE(session_id, position)`.

---

## 3. Алгоритмы генерации сессии

1. **Разрешение пула слов**:
   - Читается согласованный снимок активных слов (`is_archived = 0`).
   - Для каждого источника вычисляется множество подходящих `word_id`.
   - Результаты источников объединяются (`Union`) с сохранением дедуплицированного списка слов $W = [w_1, \dots, w_N]$.
   - Если $|W| = 0$, запуск сессии блокируется с сообщением об пустом наборе.

2. **Формирование последовательности заданий**:
   - При `task_count == NULL`:
     - Сессия состоит из ровно $N$ заданий.
     - Порядок: случайная перестановка (shuffle) множества $W$ без повторений.
   - При `task_count == K` и `selection_mode == 'shuffled_cycles'`:
     - Если $N = 1$: слово повторяется $K$ раз.
     - Если $N > 1$: последовательно генерируются циклы $C_1, C_2, \dots$ (каждый — случайная перестановка $W$).
     - Для предотвращения повторения одного и того же слова на границе: если первый элемент $C_{m+1}[0]$ совпадает с последним элементом предыдущего цикла $C_m[N-1]$, $C_{m+1}[0]$ меняется местами со случайным элементом $C_{m+1}[1 \dots N-1]$.
     - Результирующая цепочка обрезается до $K$ заданий.
   - При `task_count == K` и `selection_mode == 'random_with_replacement'`:
     - Каждое из $K$ заданий выбирается независимо и равновероятно из $W$.

3. **Фиксация и изоляция**:
   - Сформированный список заданий сохраняется в `training_sessions` и `training_session_items` в рамках единой транзакции до начала показа первого экрана упражнения.
   - Последующие изменения рецепта или слов каталога не влияют на уже начатую сессию.

---

## 4. Архитектурный контракт в кодовой базе Android

1. **Domain (`:domain:practice`)**:
   - `TrainingRecipe(id, revision, limits, isArchived)` — чистые модели без Room и Android.
   - `TrainingSourceFilter(id, sourceId, chapterRange, wordId, kanjiId, jlptLevel)` — фильтры выборки.
   - `TrainingLimits(taskCount, secondsPerWord, selectionMode)` — параметры сессии.
   - `TrainingSessionPlan(sessionId, items, timeLimitMs)` — неизменяемый план сессии.
   - `ResolveTrainingPoolUseCase`, `GenerateTrainingSessionUseCase`, `ValidateTrainingRecipeUseCase`.

2. **Data (`:data:catalogdb`)**:
   - `TrainingDao` и Room-сущности `TrainingsRow`, `TrainingsTranslationsRow`, `TrainingSourcesRow`, `TrainingSessionsRow`, `TrainingSessionItemsRow`.
   - Репозиторий `TrainingRepository` с транзакционной записью, проверкой оптимистичной блокировки по `revision` и реактивными Flow-запросами активных рецептов.

3. **Presentation (`:feature:dashboard` и `:feature:practice`)**:
   - `feature:dashboard` управляет экранами списка «Мои тренировки» и редактора «Конструктор тренировки» через UDF-состояния `TrainingsListUiState` и `TrainingEditorUiState`.
   - Переход к сессии передаёт `sessionId` в `:feature:practice`.
   - Активный рецепт сохраняется в `PreferencesDataStore` (`:data:settings`).
