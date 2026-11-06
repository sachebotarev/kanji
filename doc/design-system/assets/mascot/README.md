# Маскот каталога

| Назначение | Итоговый ресурс |
| --- | --- |
| Верхний блок «Учебники»: кот с японской книгой | `mascot_sticker_jp_bound_book.png` |
| Верхний блок «Справочники»: кот с кистью и тушечницей | `mascot_sticker_jp_inkstone_puzzle.png` |
| Читающий кот в конце списка | `../../prototypes/textbooks/assets/cat-reading.png` |
| Пустой поиск | `../../prototypes/textbooks/assets/cat-magnify.png` |
| Загрузка | `cat-shelving-books.png` и автономная копия в assets прототипа |
| Ошибка | `cat-catalog-error-v2.png` и автономная копия в assets прототипа |

Кот статичен, декоративен. Слоты: верхние блоки учебников/справочников 96, loading/error 156, конец списка 120. [Стиль и темы](../illustration-style.md). Файлы промптов относятся к происхождению выбранных ресурсов. Android-ресурсы хранятся в core/designsystem/src/main/res/drawable-nodpi; соответствие финальной цветокоррекции проверяется при переносе.

## Кот-мастер конструктора

`cat-crafting-cards.png` — отдельная иллюстрация для шапки конструктора, 1254 × 1254 RGBA, настоящий прозрачный фон. Кот собирает учебные карточки за столом; образец — читающий кот каталога. В макете отображается 96 × 96, статично и декоративно. [Происхождение и описание](cat-crafting-cards.prompt.json). Это ресурс черновика для согласования, он не заменяет читающего кота каталога.

## Дополнительные стикеры

Общие PNG хранятся в `core/designsystem/src/main/res/drawable-nodpi/`. Это отдельные статичные ресурсы с прозрачным фоном; добавление файла не означает подключения сцены к экрану.

| Сцена | Имя ресурса |
| --- | --- |
| Кот заглядывает в пустую коробку | `mascot_sticker_peek_empty_box.png` |
| Перевод не найден | `mascot_sticker_translation_not_found.png` |
| Исправление ошибки ластиком | `mascot_sticker_retry_eraser.png` |
| Завершённая работа с печатью-лапкой | `mascot_sticker_completed_sheet.png` |
| Повторение: сортировка карточек | `mascot_sticker_sorting_cards.png` |
| Размышление над сложным кандзи | `mascot_sticker_difficult_kanji.png` |
| Возвращение к книге на закладке | `mascot_sticker_bookmark_return.png` |
| Перерыв: кот потягивается | `mascot_sticker_stretch_break.png` |

Сцены не содержат языковых подписей. Штрихи на учебных листах и карточках декоративные и не служат эталоном написания кандзи. Промпты и параметры генерации сохранены рядом в файлах `cat-*.prompt.json`.

## Японская канцелярия

Варианты сохраняют внешность и шапочку маскота; японское направление передают предметы. Все шесть файлов — PNG 1254 × 1254 RGBA с прозрачным фоном в общей папке Android-стикеров.

| Сцена | Имя ресурса |
| --- | --- |
| Новая попытка с кистью для сёдо | `mascot_sticker_jp_shodo_retry.png` |
| Результат с печатью-лапкой | `mascot_sticker_jp_hanko_result.png` |
| Сортировка карточек с растительными рисунками | `mascot_sticker_jp_karuta_review.png` |
| Размышление над листом рядом с тушечницей | `mascot_sticker_jp_inkstone_puzzle.png` |
| Чтение книги с декоративным ниточным переплётом | `mascot_sticker_jp_bound_book.png` |
| Потягивание на подушке дзабутон | `mascot_sticker_jp_zabuton_break.png` |

## Мемные сценки

Самостоятельные вариации интернет-мемов и шуток об учёбе с тем же персонажем. PNG без встроенных подписей хранятся в общей папке Android-стикеров. Названия ниже описывают идею, а не являются текстом внутри изображения.

| Идея | Имя ресурса |
| --- | --- |
| «Всё под контролем»: чай среди горящих карточек | `mascot_sticker_meme_this_is_fine.png` |
| Новая карточка отвлекает от повторения старых | `mascot_sticker_meme_distracted.png` |
| «Но я же тебя учил!»: возмущённый и невозмутимый коты | `mascot_sticker_meme_you_were_learned.png` |
| Знакомый знак другим почерком вызывает растерянность | `mascot_sticker_meme_other_font.png` |
| «Сел на пять минут»: башня карточек | `mascot_sticker_meme_five_minutes.png` |
| «Слово вылетело из головы»: пустая карточка | `mascot_sticker_meme_blank_memory.png` |

Визуальные отсылки: [This Is Fine](https://knowyourmeme.com/memes/this-is-fine), [Distracted Boyfriend](https://knowyourmeme.com/memes/distracted-boyfriend), [Woman yelling at a cat](https://en.wikipedia.org/wiki/Woman_yelling_at_a_cat). Изображения не подключены к экранам приложения. Если появятся пользовательские подписи, их нужно хранить в локализации отдельно от PNG; русский — основной и резервный язык.
