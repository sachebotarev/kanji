# Иконка приложения

Активный вариант: `dark` (cat-dark-v4), подобран по [скриншоту устройства](references/device-current-2026-11-11.png): фон `#0E1510`, изумрудная шапка, кремовый кот и коралловая лента, как у маскота на главном экране. Исходник — `launcher-icon-cat-dark-v4-source.png`.

Предыдущий вариант: `jade` (cat-jade-v3), сохранён без изменений. Кот сохранён, палитра согласована с нефритовым primary `#1E6A4B`: приглушённая зелёная шапка, кремовая шерсть, охристая лента и фон `#D6E8DC`. Исходник: `launcher-icon-cat-jade-v3-source.png`, создан редактированием cat-v2 через ImageGen. Текст и локализуемые надписи отсутствуют.

Вариант `cat` (cat-v2) сохранён: исходный `launcher-icon-cat-v2-source.png` не изменён. Точная копия прежних Android-ресурсов находится в `archive/cat-v2/res/`.

Сборка активного варианта:

```sh
python3 doc/design-system/assets/launcher/build_launcher_icons.py --variant dark
```

Возврат к прежнему варианту:

```sh
python3 doc/design-system/assets/launcher/build_launcher_icons.py --variant cat
```

Команда формирует adaptive foreground, monochrome, legacy-иконки всех пяти плотностей и фон. Сгенерированные предпросмотры находятся в `generated/<variant>/masks-preview.png` (обычная, круглая и монохромная маски); generated не хранится в Git. Проверены маски и читаемость миниатюр. Стандартные Android XML adaptive-icon и ссылки Manifest сохранены.
