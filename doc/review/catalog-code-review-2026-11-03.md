# Код-ревью каталога (для агента-разработчика)

Дата: 2026-11-03. Область: модуль каталога (`:domain:catalog`, `:data:catalogdb`, `:feature:catalog`, точки сборки в `:app`). Остальные feature-модули не разбирались как готовые.

**Эталон поведения и UI:** HTML-макет Material 3 в `doc/design-system/prototypes/textbooks/` (`app.js`, `screens.js`, данные `assets/catalog.js`). Markdown в `doc/design-system/05-screens/catalog/` и `doc/implementation/catalog-ui.md` на момент ревью **устарели** — не использовать как источник истины до обновления.

**Контракты макета:**

- `node doc/design-system/prototypes/textbooks/test_content.cjs`
- `node doc/design-system/prototypes/textbooks/test_browser.cjs` (нужен просмотрщик на `http://127.0.0.1:8767/design-system/prototypes/`)

**Архитектура проекта:** `doc/architecture.md`, `doc/implementation/catalog.md`.

---

## Краткий вердикт

Каталог в целом согласован с макетом по данным, фильтрам, навигации и основным сценариям. Clean Architecture выдержана. Есть несколько **реальных дефектов** в presentation/data и **parity-расхождений** с макетом, которые стоит закрыть по приоритету ниже.

---

## Приоритет P0 — исправить в коде

### P0-1. Гонка при смене вкладки учебника (Кандзи / Слова / Главы)

**Симптом:** на один кадр список может пересчитаться с запросом от другой вкладки.

**Причина:** в `CatalogBrowseViewModel.tab()` сначала обновляется общий `mutableSearch`, затем `currentTab`. `combine` в `inputs()` успевает эмитить пару «старая вкладка + новый query».

**Файл:** `feature/catalog/src/main/kotlin/ru/mobui/kanji/feature/catalog/browse/CatalogBrowseViewModel.kt` (метод `tab`).

**Эталон макета:** `app.js`, обработчик `data-action="tab:…"` — атомарно сохраняет query/chapter по вкладкам и переключает tab в одном синхронном шаге.

**Направление fix (минимальный diff):**

- Собирать `BrowseInput` из `currentTab` и соответствующего `CatalogSearchState` без промежуточного зеркала `mutableSearch`, **или**
- Менять `currentTab` до публикации query, **или**
- Использовать один `combine(currentTab, …)` с map на search-state выбранной вкладки.

**Регрессия:** JVM или androidTest — переключение вкладки при непустом query на соседней вкладке не меняет результат списка до завершения переключения.

---

### P0-2. Смена языка UI пересоздаёт ViewModel и теряет фильтры/поиск

**Симптом:** смена locale (ключ `browse-$language` / `detail-$language` / `filter-$language`) создаёт новый `SavedStateHandle` для экрана.

**Файл:** `feature/catalog/src/main/kotlin/ru/mobui/kanji/feature/catalog/presentation/CatalogEntries.kt` (`viewModel(..., key = "browse-$language")` и аналоги).

**Эталон макета:** смена `#locale` в просмотрщике не сбрасывает маршрут и фильтры (`test_browser.cjs`, цикл `ru/en/de`).

**Направление fix:**

- Убрать `language` из ключа ViewModel; язык только в параметрах пересчёта `content(snapshot, input)` и в Compose.
- Либо явно сохранять фильтры в `NavBackStackEntry` / route-level state, не привязанном к locale.

**Ограничение проекта:** многоязычность и fallback на `ru` в домене не менять.

---

### P0-3. Устаревший `CatalogSnapshot` после изменений личного словаря

**Симптом:** после `saveWord` / `deleteWord` кэш репозитория сбрасывается, но открытые экраны каталога держат старый снимок в памяти ViewModel.

**Файл:** `data/catalogdb/.../RoomCatalogRepository.kt` (`snapshot = null` после мутаций); потребители — `CatalogStateViewModel` и наследники.

**Направление fix (на выбор архитектора, минимум — один из):**

- Инвалидировать состояние экранов через общий сигнал (Flow версии снимка / `contentVersion`).
- Вызывать `retry()` на активных catalog ViewModel при успешной записи словаря (сборка в `:app`).
- Не кэшировать снимок бессрочно в ViewModel после первого `load()` без подписки на версию.

---

## Приоритет P1 — качество и parity с макетом

### P1-1. `CatalogStateViewModel` — бездиagnosticный `Error`

**Файл:** `feature/catalog/.../presentation/CatalogStateViewModel.kt` — `catch (_: Exception)`.

**Рекомендация:** логировать причину (BuildConfig.DEBUG / Logger), опционально различать отмену и ошибки данных; не ломать контракт UI (`CatalogLoad.Error`).

---

### P1-2. Сетка кандзи — ужатие подписей значений

**Макет:** `screens.js` — `fitWorkingKanjiPreviews()`, несколько вариантов перевода, подбор по высоте.

**Android:** `CatalogMaterialCards.kt` — первый вариант перевода, `maxLines = 2`, ellipsis.

**Действие:** только если нужен visual parity с макетом; иначе зафиксировать осознанное упрощение в будущей спецификации.

---

### P1-3. Поиск на вкладке «Главы»

**Макет:** отдельный `toggle-chapter-search`, поле под вкладками, numeric input.

**Android:** общий toggle в app bar; numeric для `SourceTab.Chapters` уже есть (`CatalogBrowseScreen` → `SearchUi(..., numeric = tab == Chapters)`).

**Действие:** UX-parity, не блокер; перенос поля под вкладки — по желанию продукта.

---

## Не является багом (снято с ревью по markdown)

Сверка с `screens.js` / `app.js`:

| Было в markdown-ревью | Факт по HTML-макету |
| --- | --- |
| Кот «конец главы» на экране `chapter/…` | Блок `w-chapter-end` только при `!fixedChapter` (полный учебник). Android: `showEnd && !fixedChapter` — **совпадает**. |
| 〆 скрыт в «все кандзи» без поиска — баг | Та же логика, что `kanjiFor`: `(query \|\| radical_id \|\| scoped)`. Знаменатель «с ключом» в макете и Android — **совпадает**. |

---

## Подтверждённое соответствие макету (не трогать без причины)

- Раздельные query по вкладкам учебника (`queries[tab]` в JS ↔ `CatalogSearchState` per tab в Android).
- Раздельные фильтры глав для кандзи vs слов (`kanji.chapter` / `words.chapter`).
- Плоский список слов учебника без заголовков глав; группировка у related words с `sourceId`.
- Объём «13 слов» без «13 из 13»; объём в заголовке группы только при нескольких группах (`test_browser.cjs`).
- JLPT как точный уровень + Missing; фасеты без самоисключения.
- Fallback переводов на `ru` в domain.
- Типографика reading 16/24 sp в строке слова (`CatalogWidgets.kt` / проверка в `test_browser.cjs`).
- Radical grid: 4 тональные заливки по числу черт (`RadicalAppearance.kt`).
- `enterAlwaysScrollBehavior` — разрешён в `doc/decisions/experimental-api.md` для `CatalogScreen.kt`.

---

## Замечания по архитектуре (информационно)

- **Плюсы:** `CatalogStateViewModel`, типизированные маршруты, `FilterResult` + `CatalogResultKind`, транзакционный `CatalogInstaller`, тесты модуля catalog.
- **Производительность (не срочно):** `availableChapters()` пересчитывает wordCount по главам на каждый ввод; при росте каталога — мemoization или агрегаты в snapshot.
- **Макет vs Android (лучше на Android):** разбор слов по code points в domain (`literalKanji`, длина строк); в JS местами итерация по UTF-16 code units.

---

## Файлы для фокуса агента

| Задача | Файлы |
| --- | --- |
| P0-1 | `CatalogBrowseViewModel.kt`, `CatalogBrowseContent.kt`, тесты browse |
| P0-2 | `CatalogEntries.kt`, `CatalogRoute.kt` |
| P0-3 | `RoomCatalogRepository.kt`, wiring в `:app`, при необходимости контракт `CatalogRepository` |
| P1 | `CatalogStateViewModel.kt`, `CatalogMaterialCards.kt`, `CatalogBrowseScreen.kt` |

---

## Проверка после правок

```sh
# Контракты макета (обязательно при UI-изменениях каталога)
node doc/design-system/prototypes/textbooks/test_content.cjs

# JVM модуля catalog
./gradlew :feature:catalog:test :domain:catalog:test

# Инструментальные (эмулятор/устройство)
./gradlew :feature:catalog:connectedDebugAndroidTest
```

При изменении фильтрации — убедиться, что логика по-прежнему совпадает с `kanjiFor` / `wordsFor` / `chaptersFor` в `doc/design-system/prototypes/textbooks/app.js` (эталон in-memory поведения).

---

## Явно вне scope этого задания

- Полное ревью `feature/practice`, `feature/dashboard`, `feature/dictionary`, `feature/settings`.
- Обновление markdown-спецификаций в `doc/design-system/05-screens/catalog/` (ожидается отдельно).
- Новые `@OptIn` experimental API без записи в `doc/decisions/experimental-api.md`.

---

## Handoff для coding agent

1. Исправить **P0-1**, **P0-2**, **P0-3** в указанном порядке (можно P0-2 и P0-3 параллельно после P0-1).
2. Добавить минимальный тест на P0-1.
3. Прогнать `test_content.cjs` и `:feature:catalog:test`.
4. Не менять доменную семантику фильтров без сверки с `app.js`.
5. Коммит только по запросу пользователя; сообщение — фокус на «why» (parity с макетом / корректность lifecycle).
