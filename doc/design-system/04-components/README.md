# Компоненты текущего макета каталога

Компоненты согласованы в составе соответствующих экранов. Текущие статусы находятся в единственном [реестре](../theme/screens.json); контракт переноса — в [спецификации Android](../../implementation/approved-ui-handoff.md).

Виджет принимает неизменяемое состояние, callbacks и Modifier. Не получает ViewModel, репозиторий, NavController или DI-контейнер. События используют id; тексты разрешаются с учётом language_tag до отображения. Размеры и цвета — [токены](../03-foundations/README.md).

| Контракт | Атомарные элементы | Предполагаемое размещение |
| --- | --- | --- |
| [Каркас](catalog-shell.md) | CatalogAppBar, CatalogTabs, CatalogSearchField, CatalogSectionHeader, CatalogCount | feature/catalog; универсальные основы в core/designsystem |
| [Учебник](textbook-card.md) | TextbookCard | feature/catalog |
| [Справочник](reference-row.md) | ReferenceCard | feature/catalog |
| [Глава](chapter-card.md) | ChapterCard, ChapterPreview | feature/catalog |
| [Плитки](textbook-kanji-tile.md) | KanjiTile, RadicalTile, SymbolGrid | feature/catalog |
| [Строка слова](textbook-word-row.md) | CatalogWordRow, WordChapterHeader | core/wordui, предметная группировка в feature/catalog |
| [Фильтры](textbook-filter-bar.md) | CatalogFilterChip, CatalogFilterBar | feature/catalog |
| [Выбор](catalog-selection.md) | FilterOptionRow, ClearFilterButton, SelectionMark | feature/catalog |
| [Подробности](catalog-details.md) | InformationPanel, WordIdentity, UsagePanel, ReadingPartsPanel, KanjiIdentity, KanjiReadings, KanjiClassification, RadicalIdentity, RelatedMaterialRow | feature/catalog и общие части слова в core/wordui |
| [Состояния](catalog-status.md) | CatalogLoadStatus, CatalogEmptyState, CatalogEndMarker | feature/catalog, универсальные основы по реальному переиспользованию |

Названия — целевые контракты, не утверждение существования таких Kotlin-классов. Сначала использовать существующие WordWidgets и CatalogWidgets, меняя их контракты без дублирования. Не выделять модуль на каждую карточку.

Общие состояния: normal, pressed, focused, selected там, где есть выбор. Disabled не имитируется слабым цветом активной кнопки. В каталоге нет согласованных locked/paywall/изучено-состояний. Декоративные картинки не озвучиваются. Кликабельная карточка — один фокус; плашка с открытием и сбросом — два самостоятельных действия.

[План реализации и примеры состояния](../../implementation/catalog-ui.md). Виджеты тренировки имеют отдельный [контракт](practice.md), не утверждённый этой спецификацией каталога.
