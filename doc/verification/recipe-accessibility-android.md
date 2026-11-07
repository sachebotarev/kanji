# Доступность списка рецептов и системный перенос

## Реализация

`RecipeLibraryScreen` объединяет неинтерактивное содержимое карточки через стабильный `semantics(mergeDescendants = true)`: название, материалы и сводка доступны вместе с именованными действиями «Переместить выше/ниже». Тело карточки открывает просмотр RECIPE.02 (именованное действие «Открыть рецепт…», цель не меньше 48 dp). Постоянной радиокнопки выбора и overflow-меню на карточке нет: рецепт для тренировки выбирается на `HOME.01`. Сама карточка не получает `OnClick` на корневом узле.

Выбор семантики сверён через Context7 с официальными разделами Compose [semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics), [merging](https://developer.android.com/develop/ui/compose/accessibility/merging-clearing) и [testing](https://developer.android.com/develop/ui/compose/accessibility/testing). Использованы стабильные API текущего UI/Foundation; версии и opt-in не менялись. Интерактивные дочерние элементы не очищаются через `clearAndSetSemantics`.

## Проверки

- `RecipeAccessibilityTest` (RU/EN): название в объединённой карточке, открытие телом карточки, локализованные команды переноса, отсутствие `library-select-*` / `library-menu-*`, размеры открывающей области ≥ 48 dp, отключение переноса при поиске и busy.
- `RecipeLibraryTest` покрывает поиск, системный drag, отмену без записи и автопрокрутку за первоначальный viewport.
- Полный TalkBack-проход всего конструктора не заявлен. Схема БД `is_active` остаётся для сводки главной и fallback запуска; UI списка её не переключает.

[Машиночитаемый итог прежнего прогона](recipe-accessibility-result.json) — исторический снимок; актуальные тесты без радиокнопки.
