# Цвета конструктора

Конструктор использует [общую тему Material 3](README.md), без собственной палитры. [Редактор цветов](../prototypes/index.html#theme), [конфигурация](../theme/theme-source.json).

| Смысл | Роли |
| --- | --- |
| Главное действие, FAB | PrimaryContainer / OnPrimaryContainer |
| Выбранная карточка | SurfaceContainer / OnSurface, без контура; заполненный переключатель Primary |
| Обычная карточка | SurfaceContainer / OnSurface, без обводки |
| Переключатель выбора | Primary |
| Метка учебника | SecondaryContainer / OnSecondaryContainer |
| Метка кандзи / JLPT | PrimaryContainer / OnPrimaryContainer |
| Метка личного словаря | TertiaryContainer / OnTertiaryContainer |
| Метка главы, записи, общего источника | SurfaceContainerHighest / OnSurface |
| Количество и время (`RECIPE.W04.W01.W04`) | Текст OnSurface; иконки OnSurfaceVariant — спокойные, но контрастные к SurfaceContainer |
| Внутренняя линия | OutlineVariant для обычной и выбранной карточки |
| Удаление, ошибка | Error и ErrorContainer с парными On-ролями |

Метки отражают существующие поля источников и фильтров рецепта, выбор отмечен заполненным переключателем, параметры количества и времени дополнены подписью и спокойными значками OnSurfaceVariant. Три базовых семейства общие для всего приложения; типы не получают независимых случайных оттенков. Light/dark-роли меняются вместе с темой. Все экраны не согласованы. [Контракт конструктора](../05-screens/recipe-builder.md).
