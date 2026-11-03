# Цвета конструктора

Конструктор использует [общую тему Material 3](README.md), без собственной палитры. [Редактор цветов](../prototypes/index.html#theme), [конфигурация](../theme/theme-source.json).

| Смысл | Роли |
| --- | --- |
| Главное действие, FAB | PrimaryContainer / OnPrimaryContainer |
| Выбрана, лапка | Primary / OnPrimary |
| Учебник | SecondaryContainer / OnSecondaryContainer |
| Кандзи / JLPT | PrimaryContainer / OnPrimaryContainer |
| Мои слова | TertiaryContainer / OnTertiaryContainer |
| Смешанная, прочие слова | SurfaceContainerHigh / OnSurface |
| Включённый таймер | TertiaryContainer / OnTertiaryContainer |
| Удаление, ошибка | Error и ErrorContainer с парными On-ролями |

Тип подборки, выбор и время дополнены подписью и значком. Три базовых семейства общие для всего приложения; типы не получают независимых случайных оттенков. Light/dark-роли меняются вместе с темой. Все экраны не согласованы. [Контракт конструктора](../05-screens/training-builder.md).
