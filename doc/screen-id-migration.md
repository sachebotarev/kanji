# Идентификаторы экранов по фичам

Перенумерации по запросу пользователя: 2026-11-05 (APP/CAT → фичи) и 2026-11-06 (`TRAIN` → `RECIPE`). Канонические ID одинаковы в [карте](project-map.md), [реестре](design-system/theme/screens.json) и HTML-просмотрщике. Согласование дизайна и готовность Android не меняются. Исторические отчёты, PNG/XML и их SHA не переписываются.

## Правила

- `BOOKS` — корень фичи, `BOOKS.01` — учебник, `BOOKS.01.03` — глава; дочерние номера отражают организацию интерфейса, а не обязательную последовательность Back.
- `DICT.02.W01` — виджет редактора, `DICT.04.D01` — диалог создания тега. Список/секция → карточка/строка → части карточки; list-level действия (ещё, undo, FAB, статус) остаются на уровне секции.
- `SHARED` — повторно используемые карточки, `SELECT` — общие селекторы. Контекстные варианты остаются в фиче-владельце, повторные применения перечислены в карте. `UI-…` по-прежнему обозначает реализацию/семейство компонента, не экран.
- После согласованной миграции ID стабильны: перестановка в меню не вызывает перенумерацию. Старые ID зарезервированы навсегда.
- Старые ссылки просмотрщика принимаются и переводятся на канонический маршрут. Для старых виджетов замена префикса наследуется: `APP-03.W03.W02` → `DICT.W03.W02`; `TRAIN.W04.W01` → `RECIPE.W04.W01`.

## Соответствие 2026-11-05 (APP/CAT → фичи)

| Прежний ID | Канонический ID | Фича |
|---|---|---|
| `CAT-01` | `BOOKS` | catalog |
| `CAT-18` | `REFS` | references |
| `CAT-02` | `BOOKS.01` | catalog |
| `CAT-03` | `BOOKS.01.01` | catalog |
| `CAT-04` | `BOOKS.01.02` | catalog |
| `CAT-05` | `BOOKS.01.03` | catalog |
| `CAT-06` | `BOOKS.01.03.01` | catalog |
| `CAT-07` | `SHARED.01` | shared |
| `CAT-08` | `SHARED.02` | shared |
| `CAT-09` | `SHARED.03` | shared |
| `CAT-10` | `SELECT.01` | shared |
| `CAT-11` | `SELECT.02` | shared |
| `CAT-12` | `SELECT.03` | shared |
| `CAT-13` | `REFS.01` | references |
| `CAT-14` | `REFS.02` | references |
| `CAT-15` | `SHARED.02.01` | shared |
| `CAT-16` | `SELECT.04` | shared |
| `CAT-17` | `SELECT.05` | shared |
| `APP-01` | `HOME` | home |
| `APP-02` | `PRACTICE` | practice |
| `APP-03` | `DICT` | dictionary |
| `APP-04` | `DICT.01` | dictionary |
| `APP-05` | `DICT.02` | dictionary |
| `APP-06` | `DICT.02.01` | dictionary |
| `APP-07` | `RECIPE` | recipe-builder |
| `APP-08` | `RECIPE.01` | recipe-builder |
| `APP-09` | `RECIPE.01.01` | recipe-builder |
| `APP-12` | `RECIPE.01.02` | recipe-builder |
| `APP-13` | `RECIPE.01.02.01` | recipe-builder |
| `APP-14` | `RECIPE.01.03` | recipe-builder |
| `APP-15` | `SELECT.01` | shared (бывший `TRAIN.01.02.02`) |
| `APP-16` | `SELECT.02` | shared (бывший `TRAIN.01.02.03`) |
| `APP-17` | `SELECT.06` | shared |
| `APP-20` | `SELECT.07` | shared |
| `APP-22` | `DICT.02.02` | dictionary |
| `APP-23` | `DICT.03` | dictionary |
| `APP-21` | `DICT.04` | dictionary |
| `APP-10` | `SETTINGS` | settings |
| `APP-11` | `SETTINGS.01` | settings |
| `DLG-RECIPE-EXIT` | `RECIPE.D01` | recipe-builder |
| `DLG-RECIPE-DELETE` | `RECIPE.D02` | recipe-builder |
| `DLG-WORD-EXIT` | `DICT.D01` | dictionary |
| `DLG-WORD-DELETE` | `DICT.D02` | dictionary |
| `DLG-TAG-CREATE` | `DICT.04.D01` | dictionary |
| `DLG-TAG-EDIT` | `DICT.04.D02` | dictionary |
| `DLG-TAG-DELETE` | `DICT.04.D03` | dictionary |
| `DLG-TRANSLATION` | `DICT.02.02.D01` | dictionary |
| `DLG-CHECK` | `PRACTICE.D01` | practice |
| `DLG-DEBUG` | `PRACTICE.D02` | practice |
| `DLG-CHECK-HELP` | `SETTINGS.01.D01` | settings |
| `DLG-ACTIVITY` | `HOME.D01` | home |
| `DLG-WORD-STATS` | `HOME.D02` | home |
| `DLG-OLD-TRAINING` | `LEGACY.01` | legacy |
| `APP-02.01` | `PRACTICE.01` | practice |
| `EXT-PRACTICE-RESULT` | `PRACTICE.01` | practice (исторический ярлык; в aliases — `APP-02.01`) |

## Соответствие 2026-11-06 (`TRAIN` → `RECIPE`)

Префикс `TRAIN` отражал устаревшее имя «тренировка» для сохранённой конфигурации. Канон — `RECIPE` по [глоссарию](glossary.md). Алиасы `TRAIN*` и `APP-*` сохранены.

| Прежний ID | Канонический ID | Примечание |
|---|---|---|
| `TRAIN` | `RECIPE` | Список рецептов |
| `TRAIN.01` | `RECIPE.01` | Новый рецепт |
| `TRAIN.01.01` | `RECIPE.01.01` | Редактор |
| `TRAIN.01.02` | `RECIPE.01.02` | Добавить материал |
| `TRAIN.01.02.01` | `RECIPE.01.02.01` | Изменить материал |
| `TRAIN.01.03` | `RECIPE.01.03` | Слова в рецепте |
| `TRAIN.02` | `RECIPE.02` | Просмотр рецепта |
| `TRAIN.02.01` | `RECIPE.02.01` | Retired |
| `TRAIN.D01` | `RECIPE.D01` | Выход без сохранения |
| `TRAIN.D02` | `RECIPE.D02` | Удаление рецепта |
| `TRAIN.01.02.02` | `SELECT.01` | Уже слит (2026-11-05) |
| `TRAIN.01.02.03` | `SELECT.02` | Уже слит (2026-11-05) |

Вложенность виджетов карточки списка (те же суффиксы под `RECIPE`):

| Прежний ID | Канонический ID |
|---|---|
| `TRAIN.W04.W02` | `RECIPE.W04.W01.W01` |
| `TRAIN.W04.W03` | `RECIPE.W04.W01.W02` |
| `TRAIN.W04.W04` | `RECIPE.W04.W01.W03` |
| `TRAIN.W04.W05` | `RECIPE.W04.W01.W04` |
| `TRAIN.W04.W07` | `RECIPE.W04.W01.W05` |
| `TRAIN.01.W03.W02` | `RECIPE.01.W03.W01.W01` |
| `TRAIN.01.W03.W03` | `RECIPE.01.W03.W01.W02` |
| `TRAIN.01.W03.W04` | `RECIPE.01.W03.W02` |
| `TRAIN.01.W03.W05` | `RECIPE.01.W03.W03` |
| `TRAIN.W06` | выведен (удаление через `RECIPE.02` → `RECIPE.D02`) |

Зеркально для `TRAIN.01.01.W03.*` → `RECIPE.01.01.W03.*`.

Машиночитаемая [таблица](design-system/theme/screen-id-aliases.json). Источники, области влияния и описания — в карте; таблица не создаёт новых экранов.
