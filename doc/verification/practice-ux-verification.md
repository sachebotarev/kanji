# Верификация экрана тренировки и правил помощи

## Назначение и функциональность

Документ фиксирует итоговое поведение и верификацию экрана тренировки (`PRACTICE`), индикатора помощи, правил автоперехода и расчёта оценок.

### 1. Индикатор и диалог помощи
- В строке проверок `PRACTICE.W06` размещены 4 нажимаемых показателя фиксированной высоты: **Знак**, **Форма**, **Черты** и **Помощь** (`CheckerTestingStatuses.kt`).
- Колонка «Помощь» отображает общий расход подсказок задания: `used / max` (например, `0/3…3/3`), где значение берётся из `PracticeState.totalHintCount`.
- **Общий счётчик подсказок:** объединяет раскрытие подсказок на словах заголовка (`readingHintUsed`, `translationHintUsed` под спойлером) и обращения за подсказкой знака/черты (`cells.sumOf { it.strokeHintCount }`).
- **Исчерпание подсказок:** при использовании всего лимита подсказки просто заканчиваются (`remaining = 0`). Кнопка помощи деактивируется, ввод не блокируется, красный эталон ошибки не выводится — учащийся продолжает писать знак самостоятельно.
- **Цветовая индикация:** до использования помощи — нейтральный цвет; после принятия без помощи — зелёный; при частичном расходе — жёлтый; при полном расходе лимита — красный (`PracticeFeedbackTone.kt`). При лимите 0 отображается `0/0` нейтрального цвета (не считается расходом).
- **Диалог `HintDetailsDialog`:** открывается по клику на показатель «Помощь». Отображает остаток подсказок, расчётный балл при верном ответе и краткий понятный перечень причин снижения оценки (без технического описания формул):
  - `• Подсказки: использовано N (–20% за каждую)`
  - `• Форма принята с неточностями (–20%)`

### 2. Оценка верного знака и мягкое принятие формы
- Балл верного знака: экспоненциальная формула `0.8^k × shapeFactor`, где `k` — число обращений за подсказкой, а `shapeFactor = 0.8` при мягком принятии формы.
- Лимит `maxNextStrokeHints` ограничивает доступность помощи, но не определяет её стоимость.
- При недоборе строго меньше настроенного допуска (по умолчанию 10 п.п., настройка в калибровке `SETTINGS.01.W04.W01`) форма принимается со штрафом 20% (`accepted_with_penalty`). Форма, миниатюра знака и кнопка «Далее» отображаются жёлтым цветом.

### 3. Правила автоперехода
- Настройки автоперехода: к следующему знаку (`autoAdvanceKanjiOnSuccess`) и к следующему слову (`autoAdvanceOnSuccess`) **включены по умолчанию**.
- **Помощь не блокирует автопереход:** использование подсказок необратимо, пользователь не может их исправить.
- **Автопереход приостанавливается только исправимыми неточностями (`hasFixablePenalty`):**
  - Мягко принятая форма со штрафом оставляет текущий знак выбранным, давая пользователю возможность перерисовать его.
  - Завершение слова с мягко принятой формой требует явного нажатия кнопки «Далее».
- Ошибки написания, дедлайн таймера и ручной пропуск слова также требуют ручного нажатия «Далее» / «Пропустить».

### 4. Значения по умолчанию
- Режим подсказки в задании для рецептов (`Recipe.hintMode`, `Training.clueMode`): «Без подсказок» (`RecipeHintMode.None` / `'none'`). Чтение и перевод отображаются сразу, спойлеры скрытия отсутствуют, штраф не применяется.
- Автопереход к следующему слову (`RecognitionSettings.autoAdvanceOnSuccess`): включён (`true`).

---

## Архитектурная реализация

| Модуль | Компоненты | Ответственность |
| --- | --- | --- |
| `:domain:practice` | `HandwritingTraining.kt`, `HintScoring.kt`, `ShapeAcceptance.kt`, `RecognitionSettings.kt` | Доменная модель состояния тренировки, свойство `totalHintCount`, флаги `hasFixablePenalty`, расчёт баллов и настройки по умолчанию. |
| `:domain:catalog` | `Recipes.kt` | Модель рецепта с `RecipeHintMode.None` по умолчанию. |
| `:data:catalogdb` | `RecipeEntities.kt`, `TrainingEntities.kt`, `CatalogDatabase.kt` | Room-сущности (схема 13), миграция 12→13 для мягкого вердикта формы, дефолтные значения `none`. |
| `:data:settings` | `DataStoreRecognitionSettingsRepository.kt` | Хранение настроек в DataStore Preferences 1.1.7 с дефолтами `autoAdvanceOnSuccess = true`. |
| `:feature:practice` | `PracticeViewModel.kt`, `PracticeScreen.kt`, `PracticeScreenContext.kt`, `CheckerTestingStatuses.kt`, `HintDetailsDialog.kt`, `PracticeFeedbackTone.kt` | UI тренировки, колонка «Помощь», публикация состояния при раскрытии спойлеров, логика деактивации подсказок и автоперехода. |
| `:feature:settings` | `SettingsScreen.kt` | Экран настроек с локализованной справкой автоперехода и параметров помощи. |
| `:feature:dashboard` | `RecipeEditorScreen.kt` | Конструктор рецептов с трёхсегментным выбором подсказки в задании («Без подсказок» по умолчанию). |

---

## Результаты проверок

### JVM модульные тесты
- `:domain:practice:test` — успешно. Проверены `totalHintCount`, формула `0.8^k`, границы допуска формы (до 9.99 п.п. принимается, 10 п.п. нет), независимость стоимости от лимита, отсутствие штрафа за лимит 0.
- `:domain:catalog:test` — успешно. Дефолтный `RecipeHintMode.None`, парсинг ключей.
- `:feature:practice:testDebugUnitTest` — успешно. Цвета индикатора `hintUsageTone`, отклик кнопки «Далее» `completionTone`, расчёт остатка подсказок `HintBadgeTest`.
- `:feature:dashboard:testDebugUnitTest` — успешно.

### База данных и миграции
- `TrainingsStorageTest#softShapePenaltySurvivesDatabaseReopen` — успешно. Недеструктивная миграция Room 12→13 сохраняет записи, восстанавливает 4 частичных индекса и поддерживает вердикт `accepted_with_penalty`.
- `RecipesStorageTest#rawSqlGuardsAndForeignKeysAreInstalledOnOpen` — успешно.

### Инструментальные тесты (Pixel 10 Pro / Android 17)
- `PracticeScreenTest#nextStrokeWhenAllStrokesDrawnIncorrectlyPlaysFullDrawingAndDisappearsAfterConfiguredDelay` — при всех введённых чертах вызов подсказки запускает ускоренную анимацию знака, увеличивает общий счётчик `totalHintCount` до 1 и обновляет `check-indicator-hint` до `1 из 3`.
- `PracticeScreenTest#gestureHintPillIsShownOnlyForFirstWordAndHiddenForSubsequentWords` — плашка жестов поля письма (`pad-gesture-hint`) отображается только на первом слове тренировки (`isFirstWord = true`) и скрыта для всех последующих слов (`isFirstWord = false`).
- `PracticeScreenTest#gestureHintPillIsHiddenOnFirstWordWhenSettingIsDisabled` — при выключенной настройке «Описание жестов на поле» (`showPadGestureHints = false`) плашка жестов скрыта даже на первом слове тренировки.
- `PracticeScreenTest#tappingReadingSpoilerRevealsPronunciationAndCountsAsHint` — раскрытие чтения увеличивает общий счётчик `totalHintCount` до 1 и обновляет `check-indicator-hint` до `1 из 3`.
- `PracticeScreenTest#translationClueModeHidesTranslationAndShowsReading` — раскрытие перевода увеличивает общий счётчик `totalHintCount` до 1 и обновляет `check-indicator-hint` до `1 из 3`.
- `PracticeScreenTest#autoAdvanceSubmitsWhenHintUsedWithoutFixablePenalty` — использование подсказок не блокирует автопереход между знаками и завершение слова.
- `PracticeScreenTest#softShapeShowsPenaltyAndYellowNextWithoutConsumingHints` — мягкая форма оставляет знак выбранным, не расходует подсказки и требует ручного «Далее».
- `PracticeScreenTest#autoAdvanceSubmitsSuccessfulWordWithoutNextTap` — безошибочный ввод без штрафов автоматически отправляет задание.
- `RecipeEditorTest#hintModeSegmentedButtonChangesAndSavesSelectedMode` — переключение и сохранение режима подсказок рецепта.

### Системные скрипты
- `python3 doc/verification/check-project-map.py` — OK (41 экран, 665 виджетов, уникальные ID).
- `python3 doc/verification/check-input-shapes.py` — OK (единый радиус текстовых полей 16 dp).
