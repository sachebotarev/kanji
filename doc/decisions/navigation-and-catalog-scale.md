# Масштаб навигации и каталога

Статус: навигация верхнего уровня **реализована** (корневой type-safe `NavHost` в `:app`). Paging / `PagingSource` для каталога по-прежнему **отложены** до триггеров ниже.

## Навигация (действующее)

- Корневой граф в `AppDestinations.kt`: AndroidX Navigation Compose с `@Serializable` маршрутами (`Dashboard`, `TrainingPick`, `Practice`, `Settings`, `CheckerSettings`, `Catalog`, `References`, `Dictionary`, `RecipeBuilder(openCreate)`).
- Переходы и системный Back идут через стек `NavController` (`navigate` / `popBackStack`); форсированного `BackHandler` → Dashboard на хабах нет.
- Внутри фич остаются nested `NavHost` (каталог, словарь, конструктор рецептов) — тот же Navigation Compose, не отдельный механизм.
- PRACTICE держит `ActiveTraining` вне маршрута (in-memory); после recreate восстанавливается единственная `in_progress` из Room, иначе pop на предыдущий экран / Dashboard. Старт практики: `navigate(Practice) { popUpTo(TrainingPick) { inclusive = true } }`. Вызовы `NavController` после IO — строго на `Dispatchers.Main.immediate` (иначе lifecycle entry остаётся `INITIALIZED` и падает на destroy). Настройки чекеров открываются из Settings через отдельный корневой маршрут `CheckerSettings` (`SETTINGS.01`).
- `saveState` / `restoreState` хабов не используются: повторный заход в фичу стартует с корня.
- Deep links: custom scheme `kanji://mobui/...` на хабы корневого графа (`AppDeepLinks`, type-safe `navDeepLink`). Manifest VIEW/BROWSABLE; `singleTop` + `onNewIntent` / `handleDeepLink`. Маршруты: `/home`, `/training`, `/settings`, `/settings/checker`, `/catalog`, `/references`, `/dictionary`, `/recipes` (`?openCreate=`). PRACTICE без URI (нужен `ActiveTraining`). Не App Links (`https` / Digital Asset Links не подключены). Проверка: `adb shell am start -a android.intent.action.VIEW -d 'kanji://mobui/settings' ru.mobui.kanji`.
- App androidTest навигации (HOME, каталог, словарь, рецепты, settings, resolve deep links) приведены к корневому стеку и актуальным multi-select/редактору переводов.

Сводка в [architecture.md](../architecture.md) §4.3.

## Каталог (текущее устройство)

- `RoomCatalogRepository.load()` читает полный иммутабельный `CatalogSnapshot` один раз и кэширует под mutex; инвалидация при изменениях личного словаря.
- Фильтры, фасеты, поиск и `recipeWords` / `shuffledCycles` считаются in-memory на фоновом диспетчере.
- В Composable полный снимок не передаётся; presentation режет содержимое по сценариям — [catalog-presentation.md](catalog-presentation.md).

Сводка в [architecture.md](../architecture.md) §4.2.

## Потоковая / paged загрузка каталога

При текущем объёме (~1k слов) полный снимок и кэш осознанны. Наивный `PagingSource` на список слов ломает согласованный пул для `recipeWords`, фасетов и запуска тренировки, если не дублировать правила в SQL.

**Триггеры пересмотра**

- Измеренный cold `load()` / пик RSS неприемлемы на целевых устройствах.
- Словарь устойчиво порядка 10k+ записей (включая предложения) **и** появляются экраны бесконечного скролла без нужды в полном пуле.

**Форма будущего решения (не «просто Paging»)**

- Разделить browse/search UI (SQL + страницы) и recipe launch / facet math (компактный индекс или снимок пула материалов).
- Сохранить `language_tag` и русский fallback на границе запросов, не в UI.
- Не вводить Android Paging в `:domain:catalog`.

**Non-goals до триггера**

- Не дробить контракт `CatalogSnapshot` и не подключать paging «на вырост».
- При жалобах на скорость сначала замер `readSnapshot` и индексы Room по фактическим WHERE — до смены контракта.
- Не сплющивать маршруты каталога/рецептов в корневой граф `:app` без отдельного решения о границах модулей.
