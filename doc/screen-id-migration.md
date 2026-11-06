# Идентификаторы экранов по фичам

Однократная перенумерация по запросу пользователя 2026-11-05. Канонические ID одинаковы в [карте](project-map.md), [реестре](design-system/theme/screens.json) и HTML-просмотрщике. Согласование дизайна и готовность Android не меняются. Исторические отчёты, PNG/XML и их SHA не переписываются.

## Правила

- `BOOKS` — корень фичи, `BOOKS.01` — учебник, `BOOKS.01.03` — глава; дочерние номера отражают организацию интерфейса, а не обязательную последовательность Back.
- `DICT.02.W01` — виджет редактора, `DICT.04.D01` — диалог создания тега. `.Wxx` и вложенные номера виджетов сохранены; изменился префикс экрана.
- `SHARED` — повторно используемые карточки, `SELECT` — общие селекторы. Контекстные варианты остаются в фиче-владельце, повторные применения перечислены в карте. `UI-…` по-прежнему обозначает реализацию/семейство компонента, не экран.
- После этой согласованной миграции ID стабильны: перестановка в меню не вызывает перенумерацию. Старые ID зарезервированы навсегда.
- Старые ссылки просмотрщика принимаются и переводятся на канонический маршрут. Для старых виджетов замена префикса наследуется: `APP-03.W03.W02` → `DICT.W03.W02`.

## Соответствие

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
| `APP-07` | `TRAIN` | training-builder |
| `APP-08` | `TRAIN.01` | training-builder |
| `APP-09` | `TRAIN.01.01` | training-builder |
| `APP-12` | `TRAIN.01.02` | training-builder |
| `APP-13` | `TRAIN.01.02.01` | training-builder |
| `APP-14` | `TRAIN.01.03` | training-builder |
| `APP-15` | `SELECT.01` | shared (бывший `TRAIN.01.02.02`) |
| `APP-16` | `SELECT.02` | shared (бывший `TRAIN.01.02.03`) |
| `APP-17` | `SELECT.06` | shared |
| `APP-20` | `SELECT.07` | shared |
| `APP-22` | `DICT.02.02` | dictionary |
| `APP-23` | `DICT.03` | dictionary |
| `APP-21` | `DICT.04` | dictionary |
| `APP-10` | `SETTINGS` | settings |
| `APP-11` | `SETTINGS.01` | settings |
| `DLG-RECIPE-EXIT` | `TRAIN.D01` | training-builder |
| `DLG-RECIPE-DELETE` | `TRAIN.D02` | training-builder |
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
| `EXT-PRACTICE-RESULT` | `PRACTICE.01` | practice |

Машиночитаемая [таблица](design-system/theme/screen-id-aliases.json). Источники, области влияния и описания — в карте; таблица не создаёт новых экранов.
