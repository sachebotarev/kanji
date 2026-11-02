# Ресурсы согласованного каталога

| Ресурс | Источник | Целевое применение |
| --- | --- | --- |
| Книги Tobira / Basic Kanji Book | [textbook-icons](textbook-icons/README.md) | Каталог, декоративные PNG 48 × 48 |
| Все кандзи / ключи Канси | [reference-icons](reference-icons/README.md) | Значки 36 в слоте 48 |
| Кот чтения, загрузки и ошибки | [mascot](mascot/README.md) | Каталог/состояния 156, конец списка 120 |
| Иконки search / search_off | [material-icons](material-icons/README.md) | Рабочие иконки 24 в области 48 |
| KanjiVG | [Android assets](../../../data/kanjivg/src/main/assets/kanjivg/) | Эталон черт, геометрия и лицензии сохраняются |
| Шрифты HTML | `../prototypes/textbooks/assets/NotoSansCJK-JP.woff2`, `../prototypes/web/assets/Roboto.ttf` | Воспроизведение эталонов; в Android подобрать/подключить эквивалентные JP-глифы |

[Цветокоррекция и масштаб](illustration-style.md) обязательны для сравнения тем. Картинки не несут выбранности, прогресса или JLPT. Декоративные изображения исключаются из доступных имён, смысл передаётся текстом.

Автономные копии для HTML — `prototypes/textbooks/assets`, исходники выбранной графики — здесь и в ресурсах Android. Лицензии шрифтов/Material Icons/KanjiVG и NOTICE учебного контента не удаляются. Промпты выбранной авторской графики фиксируют её происхождение, не являются альтернативными спецификациями.

## Ресурсы остальных модулей

Иллюстрации главной находятся в `feature/dashboard/src/main/res/drawable-nodpi`, общие стикеры — `core/designsystem/src/main/res/drawable-nodpi`. Их отдельное согласование не заменяется каталогом.

Launcher: выбранный исходник [кот](launcher/launcher-icon-cat-v2-source.png), фон упаковки #A9D9CA. Инструменты `launcher/package_launcher_variant.py` и `launcher/build_launcher_icons.py` сохранены; упаковка и изменение Android-ресурсов в рамках документации не выполнялись.
