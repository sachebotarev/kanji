# Разбор и исправления по единому handoff — 2026-11-03

Источник: [merged-handoff-2026-11-03.md](merged-handoff-2026-11-03.md). Приоритет — подтверждённые ошибки состояния каталога; визуальный контракт HTML и семантика domain-фильтров не менялись.

## Решения

| Пункт | Оценка | Исправление |
| --- | --- | --- |
| P0-A | Подтверждён: активный query копировался в отдельный MutableStateFlow до смены вкладки | Убрана изменяемая копия. Вход расчёта выбирает query по ключу текущей вкладки из потоков её собственного CatalogSearchState. Публичный search — производное состояние для UI. |
| P0-B | Подтверждён: языковые ключи создавали других владельцев SavedStateHandle | Ключи browse/detail/filter стабильны в пределах NavBackStackEntry. Язык передаётся в контекст существующего владельца и пересчитывает содержимое без очистки запросов/фильтров. |
| P0-C | Подтверждён: отмена кэша репозитория не обновляла снимок, захваченный ViewModel | После успешной транзакции репозиторий сбрасывает кэш и публикует локальную revision под mutex. App передаёт её в оба входа каталога; изменение контекста перечитывает снимок и отменяет предыдущий расчёт, сохраняя ввод. |

Для P0-C намеренно не добавлялся Flow в domain: согласно архитектуре domain не зависит от сторонних библиотек. Реактивная связь внедряется через `:app`; нет feature → feature зависимости. Ревизия не хранится в БД и не подменяет версию поставляемого контента. Ошибки валидации и откат транзакции не публикуют новую ревизию.

Пункты P1/P2 оставлены отдельным backlog: диагностика ошибок, необязательный visual parity, кэш ML Kit model и структурные рефакторинги не требуются для этих исправлений. Batch-установка, memoization и кэш штрихов требуют измеренной необходимости; атомарность импорта и механизм generation практики не затрагивались. Отклонённые handoff замечания не реализовывались.

## Регрессионные проверки

- `CatalogBrowseStateTest.tabChangesNeverPublishResultsForAnotherTabsQuery`: несовпадающие запросы двух вкладок, повторные переключения; проверяются все наблюдаемые опубликованные списки, не только итоговый.
- `CatalogBrowseStateTest.revisionReloadsEditedAndDeletedWordsWithoutClearingSearchOrFilters`: редактирование и удаление снимка с сохранением поиска и выбранной главы.
- `CatalogLocaleRestorationTest.languageChangePreservesBrowseAndFilterSearchAndSelectedStrokes`: действующий NavHost, русский → английский → неподдерживаемый немецкий с русским fallback; сохраняются вкладка, фильтр черт, запрос списка и запрос открытого chooser.
- `PersonalDictionaryStorageTest`: реальные операции Room проверяют ревизии, свежесть снимка после записи/архивации и отсутствие изменения ревизии при отклонённой записи.
- Существующие проверки восстановления владельца, отмены устаревшего расчёта, локализации и навигации сохранены. Ожидание асинхронного результата предшествует чтению производного search в тесте переключения вкладок.

## Результаты

На Pixel 10 Pro, Android 17/API 37:

```sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
ANDROID_HOME="$HOME/Library/Android/sdk" ANDROID_SERIAL='<PIXEL_SERIAL_FROM_ADB_DEVICES>' \
./gradlew check :app:assembleDebug :feature:catalog:connectedDebugAndroidTest \
  :data:catalogdb:connectedDebugAndroidTest --offline --no-parallel
```

`BUILD SUCCESSFUL`: общие JVM-тесты, detekt, Android Lint и debug-сборка; каталог **20/20**, хранилище **3/3**, без ошибок и пропусков.

Дополнительно:

```sh
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=ru.mobui.kanji.CatalogNavigationTest \
  --offline --no-parallel
node doc/design-system/prototypes/textbooks/test_content.cjs
```

С теми же переменными окружения навигация приложения на Pixel — **2/2**. HTML content — 17 экранов, все по-прежнему требуют согласования. `git diff --check` — без замечаний.

LSP вызван с `wait: full`, но Kotlin server недоступен (`kotlin: error`); успешная компиляция проверена Gradle, не LSP. Полный прежний 79-тестовый native-прогон всех модулей в этой правке не повторялся: изменённые каталог/хранилище и навигация проверены отдельно. Схема БД, зависимости, experimental opt-in, baseline и правила фильтрации не менялись. Коммит не создавался.
