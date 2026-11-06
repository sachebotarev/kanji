# Проверка конструктора рецептов

Действующий контракт: [спецификация](../../design-system/05-screens/recipe-builder.md), [передача в Android](../../implementation/approved-ui-handoff.md). Статусы дизайна определяет только [реестр](../../design-system/theme/screens.json). Канонические ID экранов — `RECIPE*` ([миграция](../screen-id-migration.md)).

## Проверки HTML

При запущенном `python3 -m http.server 8873 --directory doc` из корня:

```sh
node doc/design-system/prototypes/web/test_recipes.cjs http://127.0.0.1:8873/design-system/prototypes/web/ /tmp/kanji-recipe-check
node doc/design-system/prototypes/web/test_recipe_tags.cjs http://127.0.0.1:8873/design-system/prototypes/web/ /tmp/kanji-recipe-tags-check
node doc/design-system/prototypes/web/test_kanji_picker.cjs http://127.0.0.1:8873/design-system/prototypes/ /tmp/kanji-picker-check
node doc/design-system/prototypes/web/test_multiselect_filters.cjs http://127.0.0.1:8873/design-system/prototypes/ /tmp/kanji-filter-check
```

Требуются Node.js, Playwright и Chrome. Снимки и машинные отчёты прогонов направляются в `/tmp`, не подменяют текущую спецификацию. Базовый сценарий проверяет группы: CRUD рецептов, радио-выбор, открытие просмотра с карточки, правку/удаление с экрана детали, длинные строки, ошибки полей, наборы материалов, локализацию, узкие экраны и масштабирование. Overflow-меню на карточке списка отсутствует. Дополнительные тесты проверяют общий выбор, Apply/Back, OR/AND и независимость фильтра словаря от черновика материала. [Общие теги](shared-tags.md), [матрица фильтров](../catalog-multiselect-filters.md), [унификация](../design-unification.md).

## Проверка схемы

`python3 doc/database/test_builder_schema.py`: 6 SQLite-тестов добавочного DDL поверх экспортированной Room v4. [Результат и ограничения](../database-approved-features.md). Сессии и статистика исключены.

HTML не подтверждает TalkBack, нативную клавиатуру, Compose lifecycle или реальную миграцию. Android-конструктор использует Room v6 и `RecipeBuilderRoute`; границы проверки — в [recipe-library-android.md](../recipe-library-android.md) и связанных отчётах.
