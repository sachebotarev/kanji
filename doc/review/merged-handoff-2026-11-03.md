# Единый handoff после код-ревью (2026-11-03)

Результат проверки и исправлений P0-A/P0-B/P0-C: [разбор handoff](merged-handoff-resolution-2026-11-03.md). Остальные пункты ниже остаются рекомендациями/backlog.

Документ объединяет и выверяет:

- `doc/review/catalog-code-review-2026-11-03.md` — каталог, эталон HTML-макета;
- `doc/review-2026-11-03.md` — обзор всего репозитория.

**Аудитория:** coding agent (worker).  
**Приоритет продукта:** модуль **каталог** (остальные feature-модули реализованы не полностью).

**Эталон UI/поведения каталога:** `doc/design-system/prototypes/textbooks/` (`app.js`, `screens.js`, `assets/catalog.js`). Markdown в `doc/design-system/05-screens/catalog/` и `doc/implementation/catalog-ui.md` на момент ревью **устарели** — не использовать до обновления.

**Архитектура:** `doc/architecture.md`, `doc/implementation/catalog.md`.

---

## Как читать приоритеты

| Уровень | Смысл |
| --- | --- |
| **P0** | Реальный баг или расхождение с макетом / потеря UX-состояния; чинить первым делом в каталоге |
| **P1** | Качество, производительность, maintainability; после P0 или отдельным PR |
| **P2** | Улучшения без срочности |
| **Отклонено** | Не подтверждено кодом или завышена severity — **не делать** |

---

## P0 — каталог (сделать в первую очередь)

### P0-A. Вкладки учебника + поиск: гонка и дублирование state

**Объединяет:** catalog-review P0-1 + review-2026-11-03 п.9.

**Симптом:** при смене вкладки (Главы / Кандзи / Слова) список может на кадр пересчитаться с query другой вкладки.

**Причина:**

1. `CatalogBrowseViewModel.tab()` обновляет `mutableSearch` до `currentTab`.
2. `mutableSearch` дублирует `activeSearch().state` — два источника правды.

**Файлы:**

- `feature/catalog/.../browse/CatalogBrowseViewModel.kt`
- при необходимости `feature/catalog/.../presentation/CatalogEntries.kt` (collect search)

**Эталон макета:** `app.js`, action `tab` — атомарно `queries[tab]`, `chaptersByTab`, `current.tab`, `current.query`.

**Fix (минимальный diff, выбрать один подход):**

- Убрать `mutableSearch`; в `inputs()` комбинировать `currentTab` с Flow search **активной** вкладки.
- Либо менять `currentTab` раньше публикации query.
- Не ломать отдельные `CatalogSearchState` per tab в `SavedStateHandle`.

**Тест:** androidTest или JVM — переключение вкладки при непустом query на соседней вкладке не меняет состав списка «чужим» query.

---

### P0-B. Смена языка UI сбрасывает фильтры и поиск каталога

**Источник:** catalog-review P0-2.

**Симптом:** ключ ViewModel `browse-$language` / `detail-$language` / `filter-$language` пересоздаёт `SavedStateHandle`.

**Файл:** `feature/catalog/.../presentation/CatalogEntries.kt`.

**Эталон:** `test_browser.cjs` — смена locale не сбрасывает маршрут и фильтры.

**Fix:** убрать `language` из ключа ViewModel; язык только для пересчёта `content()` и строк UI. Fallback `ru` в domain не трогать.

---

### P0-C. Stale `CatalogSnapshot` в открытом каталоге после личного словаря

**Источник:** catalog-review P0-3 (в review-2026-11-03 не выделено).

**Симптом:** после `saveWord` / `deleteWord` репозиторий обнуляет кэш (`snapshot = null`), ViewModel продолжает фильтровать старый снимок.

**Файлы:**

- `data/catalogdb/.../RoomCatalogRepository.kt`
- `feature/catalog/.../presentation/CatalogStateViewModel.kt`
- wiring `:app` (словарь ↔ каталог)

**Fix (один из):**

- Flow версии / `contentVersion` и `retry()` или пересборка state в ViewModel;
- явная инвалидация catalog screens после успешной записи словаря.

**Примечание:** это **не** «load внутри транзакции saveWord» (см. раздел «Отклонено»).

---

## P1 — каталог и data (после P0)

### P1-A. `CatalogStateViewModel` — Error без диагностики

**Файл:** `feature/catalog/.../presentation/CatalogStateViewModel.kt` (`catch (_: Exception)`).

Логировать причину в DEBUG; `CancellationException` пробрасывать (уже так). UI-контракт `CatalogLoad.Error` сохранить.

---

### P1-B. Parity с макетом (опционально)

| Тема | Макет | Android | Действие |
| --- | --- | --- | --- |
| Подписи в сетке кандзи | `fitWorkingKanjiPreviews` | первый вариант, 2 строки | parity или зафиксировать упрощение |
| Поиск на «Главах» | поле под вкладками | toggle в app bar | UX, не блокер |

---

### P1-C. `DigitalInkKanjiChecker` — кэш `DigitalInkRecognitionModel`

**Источник:** review-2026-11-03 п.2 (severity снижена с 🔴 до P1).

**Факт:** `recognizer` кэшируется; `DigitalInkRecognitionModel.builder(...).build()` создаётся на каждый `checkWithDetails()`.

**Файл:** `checker/readability/.../DigitalInkKanjiChecker.kt`.

**Fix:** кэшировать model (как `japaneseModel()` / single instance), не плодить builder на hot path.

---

### P1-D. `CatalogInstaller` — длинная транзакция обновления каталога

**Источник:** review-2026-11-03 п.6 (частично).

**Факт:** полная переустановка связей в одной транзакции — **by design** (целостность). `ensureActive()` в installer **уже есть**.

**Действие:** только при измеренных проблемах на большом каталоге — batch/инкремент; не ломать атомарность без ADR.

---

### P1-E. Производительность фильтров (backlog)

`availableChapters()` и подобные пересчёты на каждый символ поиска — ок для текущего объёма; при росте данных — memoization в snapshot.

---

## P1 — app / practice (отдельные PR, не блокируют каталог)

### P1-F. `MainActivity` — монолит навигации и demo graph

**Источник:** review-2026-11-03 п.4–5.

Вынести routing/models в отдельные файлы `:app`; унифицировать создание чекеров; demo `createTask()` — в test/debug source set.

---

### P1-G. `PracticeViewModel` / `CheckCoordinator`

**Источник:** review-2026-11-03 п.7.

Работает через `generation`; рефакторинг на `StateFlow` — по желанию, не срочно.

---

## P2 — backlog

| ID | Тема | Файл |
| --- | --- | --- |
| P2-A | Лимит `history` в `PracticeSession` | `domain/practice/.../HandwritingTraining.kt` |
| P2-B | Кэш `Path` по stroke в `KanjiWritingPad` | `core/writingpad/.../KanjiWritingPad.kt` |
| P2-C | Разделить `RecognitionSettings` | `domain/practice/.../RecognitionSettings.kt` |
| P2-D | Упростить callback SQL в `CatalogDatabase` | `data/catalogdb/.../CatalogDatabase.kt` |
| P2-E | Комментарий к `persistenceScope` в Application | `KanjiApplication.kt` |

---

## Отклонено (не implementировать по мотивам review-2026-11-03)

| Утверждение | Вердикт |
| --- | --- |
| `saveWord()` вызывает `load()` **внутри** `withTransaction` | **Ложно.** `load()` вызывается **до** `mutex.withLock { withTransaction { … } }` в `RoomCatalogRepository.kt`. |
| Top priority «вынести load() из транзакции» | **Уже выполнено** в текущем коде. |
| `persistenceScope` — 🔴 утечка | **Завышено.** Scope на lifetime Application — норма; максимум P2-E (документация). |
| «Добавить ensureActive в CatalogInstaller» | **Частично ложно** — вызовы уже есть. |
| Кот «конец главы» на `chapter/…` — баг | **Ложно** относительно HTML: `w-chapter-end` только при `!fixedChapter`. Android совпадает. |
| 〆 в «все кандзи» без поиска — баг | **Ложно** относительно HTML: та же формула, что `kanjiFor` в `app.js`. |

---

## Не трогать без причины (подтверждено макетом и кодом)

- Раздельные query и фильтры по вкладкам учебника; `kanji.chapter` vs `words.chapter`.
- Плоский список слов учебника; группировка только у related words с `sourceId`.
- Счётчики слов («13 слов», без «13 из 13»); объём в заголовке группы только при нескольких группах (`test_browser.cjs`).
- JLPT exact + Missing; фасеты без самоисключения; fallback переводов на `ru`.
- Reading 16/24 sp в строке слова; radical grid — 4 тона по чертам.
- `enterAlwaysScrollBehavior` — разрешён в `doc/decisions/experimental-api.md`.
- SHA-256, версия каталога, запрет downgrade, триггеры origin слов (`CatalogInstaller`).
- Clean Architecture, DI конструкторами, immutable domain models.

---

## Что сохранить из обоих ревью (архитектура)

- Domain без Android; `CatalogSnapshot` + in-memory фильтрация на `Dispatchers.Default`.
- Типизированная навигация каталога, `FilterResult` + `CatalogResultKind`.
- Atomic Compose widgets (state + callbacks, без ViewModel внутри виджетов).
- `CheckCoordinator.generation` для отмены проверок практики.
- Android точнее макета в Unicode (`codePoints` vs UTF-16 в JS) — не деградировать.

---

## План работ для coding agent

### Фаза 1 — каталог (обязательно)

1. **P0-A** — search + tab (убрать дублирование, атомарность).
2. **P0-B** — ключ ViewModel без `language`.
3. **P0-C** — инвалидация snapshot после словаря.
4. Тест на P0-A.
5. Прогон проверок (ниже).

### Фаза 2 — по запросу / отдельный PR

6. P1-A, P1-B (если нужен visual parity).
7. P1-C (ML Kit model cache) — если трогается практика.
8. P1-F — рефакторинг `MainActivity`.

### Фаза 3 — backlog

9. P2-A … P2-E по приоритету продукта.

**Ограничения проекта:**

- Не добавлять experimental API без `doc/decisions/experimental-api.md`.
- Не менять семантику фильтров без сверки с `app.js` (`kanjiFor`, `wordsFor`, `chaptersFor`).
- Коммит — только по явной просьбе пользователя.

---

## Проверка после изменений

```sh
# Контракты HTML-макета каталога
node doc/design-system/prototypes/textbooks/test_content.cjs

# JVM
./gradlew :feature:catalog:test :domain:catalog:test :data:catalogdb:testDebugUnitTest

# Инструментальные каталога (эмулятор/устройство)
./gradlew :feature:catalog:connectedDebugAndroidTest

# Опционально: браузерные контракты макета (нужен сервер :8767)
node doc/design-system/prototypes/textbooks/test_browser.cjs
```

При правках `:app` / practice дополнительно:

```sh
./gradlew :feature:practice:test :app:assembleDebug
```

---

## Карта файлов

| Задача | Основные файлы |
| --- | --- |
| P0-A | `CatalogBrowseViewModel.kt`, `CatalogBrowseContent.kt` |
| P0-B | `CatalogEntries.kt`, `CatalogRoute.kt` |
| P0-C | `RoomCatalogRepository.kt`, `CatalogStateViewModel.kt`, `:app` wiring |
| P1-A | `CatalogStateViewModel.kt` |
| P1-B | `CatalogMaterialCards.kt`, `CatalogBrowseScreen.kt` |
| P1-C | `DigitalInkKanjiChecker.kt` |
| P1-F | `MainActivity.kt`, новые `app-*.kt` в `:app` |
| P2-A | `PracticeSession.kt` |

---

## Источники (история)

| Файл | Роль |
| --- | --- |
| `doc/review/merged-handoff-2026-11-03.md` | **Актуальный единый handoff** |
| `doc/review/catalog-code-review-2026-11-03.md` | Детали каталога + макет |
| `doc/review-2026-11-03.md` | Исходный широкий обзор (часть пунктов отклонена после верификации) |
