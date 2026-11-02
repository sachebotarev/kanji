# Документация «Кандзи дэс нэ»

## Начать здесь

- [Дизайн-система](design-system/README.md): актуальный макет каталога; все 17 экранов ожидают повторного согласования.
- [Каталог Android](implementation/catalog-ui.md), [интерактивный макет](design-system/prototypes/textbooks/index.html), [обзор экранов](design-system/05-screens/catalog/review/index.html), [проверки и снимки](verification/catalog/refactor-2026-11-02/README.md).
- [Архитектура](architecture.md): Compose, Clean Architecture, модули и DI.
- [Обязательная многоязычность](implementation/localization.md): русский основной и fallback, переводы по language_tag.
- [Навыки разработки](development-skills.md): назначение и маршрутизация локальных skills.

## Разделы

| Каталог | Назначение |
| --- | --- |
| [design-system](design-system/README.md) | Единственный актуальный раздел дизайна, UX и экранных требований |
| [implementation](implementation/practice-screen.md) | Контракты и устройство Android/domain/data, без истории визуальных итераций |
| [decisions](decisions/handwriting-verification.md) | Действующие технические и продуктовые решения |
| [database](database/words.md) | Модели кандзи, ключей и слов/выражений/предложений |
| [imports](imports/catalog/README.md) | Воспроизводимая подготовка учебного содержимого |
| [dictionaries](dictionaries/local-reference-materials.md) | Источники, сверка и правила локального хранения |
| [calibration](calibration/README.md) | Данные, сценарии и измерения проверки рукописного ввода |
| [verification](verification/README.md) | Технические свидетельства проверок алгоритмов, отдельно от текущего дизайна |

## Основные контракты реализации

- [Тренировка одного задания](implementation/practice-screen.md), [поле письма](implementation/ui-pad.md), [подсказка](implementation/hints.md), [таймер](implementation/task-timer.md).
- [Три проверки](decisions/three-checkers.md), [форма](implementation/shape-checker.md), [Procrustes](implementation/procrustes.md), [словарные правила](implementation/kanji-rule-dictionary.md), [диагностика](implementation/debug-inspector.md).
- [Хранение настроек](implementation/persistent-settings.md), [японская модель](implementation/japanese-model.md).
- [Каталог](implementation/catalog.md), [личный словарь](implementation/personal-dictionary.md), [кандзи](implementation/kanji-catalog.md), [ключи](implementation/radicals-catalog.md).
- Схемы: [слова и выражения](database/words.md), [кандзи](database/kanji.md), [ключи](database/radicals.md).

Документ описывает действующее состояние или явно помеченное предложение. Не дублировать в нём хронологию обсуждений, старые макеты и отменённые указания. Датированные снимки, происхождение данных, лицензии и результаты измерений сохраняются как проверяемые свидетельства. Порядок обновления дизайна — [процесс](design-system/workflow.md).
