# Иконка «Кандзи дэс нэ»

## Текущий фон — мягкий голубовато-мятный, 21 октября 2026

Из-за неонового впечатления на устройстве насыщенность фона снижена: `#82E5B3` заменён на `#A9D9CA`. Кот, масштаб и центрирование сохранены. Более яркий мятный вариант сохранён в [предпросмотре](launcher-cat-bright-mint-masks-preview.png). Сборка debug и lint успешны.

## Предыдущий фон — яркий мятный, 21 октября 2026

После просмотра жёлтого варианта фон заменён на яркий мятно-зелёный `#82E5B3`: он поддерживает зелёную палитру приложения, сохраняя контраст с тёмной шапкой и светлым котом. Исходник кота v2, масштаб и центрирование сохранены. Изменены цветовой ресурс и фон упаковки обычных иконок; передний и монохромный слои сохраняются. Жёлтый вариант оставлен для сравнения: [предпросмотр](launcher-cat-yellow-masks-preview.png). [Мятный вариант](launcher-variants/cat/masks-preview.png). Сборка debug и lint успешны.

## Предыдущий фон — яркий кот v2, 21 октября 2026

По замечанию о мелком смещённом коте и болотном фоне подготовлен [прозрачный исходник v2](launcher-icon-cat-v2-source.png) встроенным image_gen. Фон заменён на солнечный жёлтый `#FFD766`. Сохранены светлый кот, открытые лапы, хвост-крючок и зелёная ученическая шапка с красной полосой и кокардой.

При упаковке радиус силуэта увеличен со 130 до 142px на адаптивном холсте 432px; горизонтальное положение учитывает массу непрозрачных пикселей, чтобы асимметричный хвост не смещал зрительный центр. Проверены круглая и скруглённая маски, малый размер и монохромный вариант. Цвет монохромной иконки задаёт системная тема Android, поэтому солнечный фон относится к обычной цветной иконке.

Ресурсы воспроизводятся `python3 doc/design/build_launcher_icons.py`. [Предпросмотр масок](launcher-variants/cat/masks-preview.png).

## Предыдущий вариант — кот в ученической шапке

## Согласованный персонаж — кот в ученической шапке

Пользователь выбрал кота с раскрытыми лапами, хвостом-крючком 子 и шапкой 宀 с красной полосой и жёлтой кокардой. [Прозрачный исходник](launcher-icon-cat-v1-source.png) сохранён из приложенного пользователем изображения. Подобран однотонный тёплый фисташковый фон `#C3D1AD`; [цветной вариант](launcher-icon-cat-background.png) подготовлен встроенным `image_gen`.

Отдельно упакованы адаптивный передний слой, монохромный контурный слой, фон и обычные размеры Android. Воспроизводимая упаковка: `python3 doc/design/package_launcher_variant.py cat`. [Проверка масок и малого размера](launcher-variants/cat/masks-preview.png), [предпросмотр](launcher-variants/cat/preview.png), ресурсы — `launcher-variants/cat/res/`. Упаковка использует точный исходный персонаж без перерисовки. Выбранная иконка подключена в ресурсах приложения. Пересборка ресурсов: `python3 doc/design/build_launcher_icons.py`.

Промпт выбора фона:

> Precise background-only compositing edit. Preserve the supplied approved cat mascot EXACTLY: same face, whiskers, ears, green 宀-shaped student hat with red stripe and gold cockade, welcoming open paws, hind paws, curved tail, outlines, proportions, color and position. Do not redraw, redesign or change any part of the character. Replace only the transparent background with a completely uniform flat warm pale pistachio green #C3D1AD. This muted warm green should contrast with the warm ivory cat and dark forest-green contours, support red and gold accents, and feel like a polished Japanese learning app icon. Full-bleed square background, absolutely no gradient, texture, vignette, shadows, glow, shapes, border, text or rounded tile. Preserve all existing negative-space openings around limbs and tail by filling them with the same pistachio background. One final app-icon image.

## Эскиз v13 — возвращение силуэта 字

Усилен намёк на 字: расширенная шапочка с центральным выступом напоминает 宀, горизонтально раскрытые руки и короткий загиб нижнего края — 子. Тело укорочено, добавлен второй сгиб ткани для узнаваемости призрака. Сохранены тёплая палитра и школьная эмблема. [Исходник v13](launcher-icon-v13-source.png). Создан встроенным `image_gen`, без замены иконки приложения.

Промпт v13:

> Design refinement for the same original ghost kanji-study mascot. Use image 1 for the warm school cap and friendly expression, image 2 for the valuable 字-shaped silhouette. Restore the association with the exact kanji 字 through the silhouette while avoiding an elongated tail. A compact warm ivory floating ghost with narrow torso, open welcoming arms, and a JAPANESE SCHOOL CAP. The school cap is specially designed so its silhouette echoes 宀: a short upright center tab above its flat low crown, a wide dark-green visor extending horizontally past the head with short downturned ends, restrained navy band and small brass badge. Clearly a school cap, no pirate hat, no traditional scholar hat. Below, the ghost's two welcoming sleeve arms stretch horizontally at chest level to create the long cross stroke of 子. The compact draped torso descends just a SHORT distance below these arms, ending with a SHORT broad leftward folded hem like the hook of 子, plus a small secondary cloth notch so it is visibly a sheet ghost. No long tail, no thin appendage, no teardrop or comma silhouette. The overall arrangement must evoke 字: roof and top dot from cap, upper small bent stroke suggested by cheek/shoulder contour, arms horizontal, short central body with left hem hook. Face is small, gently happy, two simple dark oval eyes and tiny smile. Arms slightly curved forward to invite a hug. Thin graceful compact character rather than a round blob. Friendly original Japanese learning mascot, legible elegant silhouette, thick clean forest-green outlines, warm ivory fill, warm pale sage background, tiny muted gold cap badge. Flat polished colors, no gradients, textures, lighting, shadows or 3D. No printed kanji, no text, no props, seal, brush, sparkles or blush. Centered within middle 65 percent of square with generous margins. One finished app icon.

## Эскиз v11 — школьный призрак

По запросу пользователя возвращены тёплые оттенки, призрак заметно стройнее, руки направлены для объятия. Вместо широкой шапочки — японская школьная фуражка с коротким козырьком и круглой эмблемой. [Исходник v11](launcher-icon-v11-source.png). Создан встроенным `image_gen`, отдельный эскиз без замены ресурсов приложения.

Промпт v11:

> Edit the supplied ghost app mascot concept. Preserve the friendly little ghost identity, dark bold clean outline, small oval eyes and tiny smile, simple flat icon illustration. Apply these specific changes visibly: make the entire ghost including head and torso MUCH slimmer, about 30 percent narrower than reference, with an elongated graceful floating body and a small left-curved tail. Two long slim arms reach diagonally forward toward the viewer in an inviting hug, with softly rounded mitten-like ends curving inward as though about to wrap around the viewer. Make the welcoming embrace unmistakable, not two stubby sideways fins. Face warmly pleased and gentle, no angry eyebrow. Replace the wide old scholar/pirate-style hat completely with a recognizable traditional JAPANESE SCHOOLBOY CAP (gakuseibo): neat compact dark navy round cylindrical crown, small low flat top, short central glossy-free visor, narrow band and one tiny round brass school badge. No broad side wings, no pointed top, no pompom, no mortarboard. Cap at a slight jaunty tilt, school association clear, restrained simple shapes. Return to warm color palette: ivory cream ghost #FFF8E9, muted warm sage background #B8C8A5, dark deep green outlines #203D31, navy-green school cap, tiny warm ochre badge. Warm but avoid muddy brown cast. Soft inviting Japanese school mascot, original character, clean professional app icon, strong coherent silhouette. Maintain subtle resemblance to 字 through cap and outstretched arms plus hooked tail without printing a kanji anywhere. No text, no seal, no paintbrush, no blush, no extra props. Flat solid colors, no gradients, shadows, texture, 3D or lighting effects. Centered in square with generous quiet background margin. Single finished icon.

## Эскиз v10 — прохладная палитра и открытые объятия

Уточнение v9 по запросу пользователя: чуть более стройный корпус, открытые руки, белое тело, мятный фон, тёмная бирюзово-зелёная шапочка со светлой полосой вместо терракоты. [Исходник v10](launcher-icon-v10-source.png). Создан встроенным `image_gen`; пока отдельный эскиз, без замены установленной иконки.

Промпт v10:

> Edit this approved ghost-in-a-cap app icon, preserving the original identity, cap silhouette with top knob and wide brim, subtly 字-shaped overall silhouette, curved ghost tail, simple outlined illustration and small face. Make ONLY these refinements: ghost torso about 20 percent slimmer and slightly more elongated, graceful floating tapered body rather than a round belly. Make both arms a little longer, reaching out toward the viewer in a welcoming open embrace, gently curving forward, as if the ghost wants to give you a hug; maintain clear simple silhouette and keep limbs uncluttered, no fingers. Expression warmer and friendly, tiny relaxed smile, eyes simple and kind, preserve slight mischievous asymmetry without angry eyebrow. Eliminate brown, beige, yellow and terracotta undertones everywhere. Ghost body clean soft cool white #F7FCFC. Background fresh flat cool pale mint #B8DAD3. Hat muted deep teal-green #24564F, outlines darkest teal #163C38; replace orange hat band with desaturated light aqua #75BEB2. Keep the recognizable hat shape unchanged. Limited cohesive cool colors, clean flat fills, no gradients, textures, paper grain, shadows, 3D or extra details. No letters, no seal, no brush. Keep existing framing and safe padding for Android launcher. One single finished square icon.

## Эскиз v9 — призрак в шапочке

По предложению пользователя создан оригинальный призрак в шапочке, общий силуэт напоминает 字: шапочка с верхним выступом отсылает к 宀, короткие руки и загнутый хвост — к 子. Палитра связана с приложением: зелёный, кремовый, небольшой терракотовый акцент. Вдохновение — дружелюбная маскотная графика renshuu, персонаж самостоятельный. [Исходник v9](launcher-icon-v9-source.png). Создан встроенным `image_gen`, пока отдельный эскиз; ресурсы приложения не заменены.

Промпт v9:

> Create a distinctive polished mobile app icon: an original little ivory GHOST wearing a dark forest-green scholar's CAP, with the combined silhouette subtly resembling the Japanese kanji 字. This resemblance comes from the SHAPES, not printed lettering. Cap has a short upright rounded top knob and a broad flat slightly tilted brim with downturned ends, echoing the 宀 roof radical. Beneath the cap a compact floating ivory ghost with two short horizontally outstretched sleeve-like arms and a single elegant small left-curving hooked tail, subtly echoing 子. It must first read as a charming ghost in a jaunty cap, and only then reveal the 字 silhouette. NOT a literal white kanji with a face drawn into it; NOT a blob, melting puddle or layered pile. Clear separation between cap and face; simple rounded head and compact broad upper body, one tapering tail, no feet. Tiny dark oval eyes, one slightly narrowed with quiet mischievous confidence, and a tiny curved smile. Clean bold dark forest-green outlines, restrained warm ivory fill, flat muted sage #ADC4A6 background. A single small muted terracotta cap band detail. Sophisticated indie Japanese game mascot icon with renshuu-like clarity and warmth but an entirely original design. Thick beautifully controlled outlines, strong readable silhouette, flat matte solid fills, no texture, gradients, shading, 3D or shadows. No printed kanji, no text, no paintbrush, no seal, no blush, no sparkles, no cat ears, no headband or bandana. Keep design simple enough to draw from memory and legible at 48px. Full bleed square sage background, no border or outer rounded tile. All important subject within central 65% of canvas for Android mask. One finished icon.

## Новый эскиз v6 (отдельный вариант)

Создан встроенным `image_gen` 19 октября 2026 года. Каллиграфический 字 тушью и маленькая киноварная печать. [Исходник](launcher-icon-v6-source.png), [предпросмотр на тёплом фоне](launcher-icon-v6-preview.png). Сохранён отдельно; в установленном приложении восстановлен исходный персонаж.

Промпт v6:

> Use case: logo-brand. Design a new sophisticated Android icon for kanji handwriting study. Use supplied first concept only as inspiration for its expressive black calligraphy, quiet wit and warm Japanese stationery character; do NOT replicate a cartoon mascot. Main subject: exactly the kanji 字 in masterful bold Japanese brush calligraphy, highly legible, with generous internal negative space, rich controlled thick-to-thin stroke rhythm and just a few decisive dry-brush edges. Distinctive compact asymmetric composition, editorial Japanese graphic-design sensibility, mature, stylish, quietly witty, elegant. The roof is assertive and slightly tilted, lower 子 has a beautiful sweeping hooked finish. A tiny vermilion square seal at lower right, blank interior with no invented letters, is the only color accent. No face, eyes, smile, limbs, blush, paintbrush prop or cute character. No Google colors, rainbow, rounded geometric tubes, generic tech logo, gradient, 3D or shadow. Ink near-black, restrained vermilion accent, transparent background for placing on warm ivory. Keep the mark boldly readable at launcher size, not overly intricate or distressed. Single finished foreground with generous transparent padding, no tile, no frame, no words. A handsome personal calligraphic monogram, crafted rather than corporate.

### Восстановление исходника

По запросу пользователя восстановлена самая первая иконка: подмигивающий 字 с кистью, тушь и тёплый бумажный фон. Используется исходный `launcher-icon-source.png`, без перерисовки. Скрипт упаковки, цвет фона и ресурсы приложения возвращены к исходной концепции. [Предпросмотр](launcher-icon-restored-preview.png).


## Вариант v5 — 字 (отклонён)

Согласованный цветной геометрический стиль v4 сохранён, знак заменён на 字 по запросу пользователя. Коралловая точка, синяя крыша 宀 и горизонталь, бирюзовая нижняя часть 子. Создано встроенным `image_gen` 19 октября 2026 года.

[Исходник v5](launcher-icon-v5-source.png), [предпросмотр v5](launcher-icon-v5-preview.png). Скрипт упаковки и ресурсы приложения переключены на v5, включая адаптивный, монохромный и обычные варианты.

Промпт v5:

> Edit the supplied approved kanji app logo: change ONLY the character from 文 to the exact Japanese kanji 字. Preserve the same minimal colorful geometric product-icon style, thick rounded strokes, vivid blue, teal and coral palette, compact balanced composition and transparent background. Correct structure of 字 is essential: upper 宀 roof consisting of a short coral top dot, left short blue descending tick and broad blue roof with short downturned right end; lower 子 consisting of a short angular teal top stroke, central teal vertical curving hook, and a wide blue horizontal crossbar. It must read unmistakably as 字, not 文, 学, 安 or a made-up symbol. Keep big open spaces between components, optically balanced stroke weights and rounded terminals. Flat color fields, clean crisp edges, no texture, no shadow, no new decoration. No face, no mascot, no brush, no extra text, no tile, no border. One single finished logo foreground centered with transparent padding.

## Предыдущий вариант v4

Новая концепция от 19 октября 2026 года, созданная встроенным `image_gen` без референсов прежнего персонажа. Абстрактный 文 из крупных синих, бирюзовых и коралловых штрихов на светлом холодном фоне `#F7F8FC`. Лицо и кисть удалены по запросу пользователя полностью сменить направление.

[Исходник v4](launcher-icon-v4-source.png), [предпросмотр v4](launcher-icon-v4-preview.png). Ресурсы приложения и скрипт упаковки переключены на v4; предыдущие исходники сохранены.

Промпт v4:

> Use case: logo-brand. Create a completely new modern Android launcher icon for a Japanese kanji learning app. A minimal abstract typographic symbol inspired by the Japanese character 文, built from four broad precisely drawn geometric strokes with softly rounded ends and elegant optical balance. Upper small dash in warm coral, strong horizontal bar in vivid blue, two beautifully balanced crossing diagonal strokes below in blue and teal, with a clean deliberate negative-space cut at the crossing. A simple distinctive harmonious brand mark, not a literal illustration. The composition should feel confident, warm, playful and memorable, using the disciplined simplicity of major technology product icons without reproducing any existing logo. Exactly flat solid colors, no gradients, no shadows, no texture, no 3D, no outlines. NO mascot, NO face, NO eyes, NO smile, NO hands or feet, NO paintbrush, NO stationery, NO extra symbols, NO lettering besides the single abstract kanji-inspired mark. Transparent background, no tile or border. Compact centered mark with generous padding. Strong readability at 32px. One finished icon foreground asset. Start entirely fresh; no resemblance to a winking character.

## Отклонённый вариант v3

Упрощён 19 октября 2026 года встроенным `image_gen` по запросу пользователя: ориентир — лаконичность Google, Nintendo и Microsoft. Сохранены 字, подмигивание и коралловая кисть. Удалены руки, ноги, блики глаза и детали кисти. Персонаж стал компактным знаком с крупными открытыми просветами.

[Исходник v3](launcher-icon-v3-source.png), [предпросмотр v3](launcher-icon-v3-preview.png). Ресурсы Android, включая адаптивный и монохромный слои, заменены на v3. Скрипт `build_launcher_icons.py` теперь использует исходник v3; v1 и v2 сохранены.

Промпт v3:

> Use case: logo-brand. Radically simplify the supplied kanji app icon into a minimal flat geometric brand symbol. Reference is concept only, NOT illustration style. Retain recognizable Japanese 字 and a playful wink and a very short coral calligraphy brush. Aim for the distilled simplicity and confident geometric construction of Google product icons, Microsoft product marks and Nintendo's playful clarity, without copying their logos. This must be a LOGOMARK, not a cartoon illustration. Remove ALL arms, legs, feet, hands, eyebrows, cheek decorations, textures, highlights, outlines, bevels, shadows and gradients. Main shape is a compact upright 字 built from approximately six thick uniform geometric rounded strokes; upper roof and lower 子 structure clearly legible. In the central negative space, a tiny solid dot, one simple wink arc, and small smile convey personality. One short coral brush beside lower right, formed from a single rounded coral handle and one solid black teardrop nib, no hand holding it. Two solid opaque colors only: near black #202124 and coral #F45B46. Transparent background. No tile, text, border, mockup. Large open negative spaces; instantly readable at 32 pixels. Perfectly crisp smooth flat edges. Centered compact balanced silhouette with generous transparent padding. One final icon foreground.

## Предыдущий вариант v2

Обновлён 19 октября 2026 года встроенным `image_gen`. Сохранён подмигивающий 字 с кистью; убраны бумажная фактура, румянец, брови и мелкие декоративные детали. Гладкие плотные штрихи и короткие конечности дают более цельный силуэт. Цвета: почти чёрный, коралловый акцент и однотонный тёплый фон.

[Прозрачный исходник v2](launcher-icon-v2-source.png), [предпросмотр v2](launcher-icon-v2-preview.png). Предыдущий исходник сохранён ниже. Ресурсы приложения заменены на v2.

Упаковка воспроизводится командой `python3 doc/design/build_launcher_icons.py` (Pillow). Скрипт сохраняет прозрачность, масштабирует передний слой, создаёт монохромную маску и ресурсы mdpi–xxxhdpi. Силуэт помещён в центральную безопасную окружность 66dp на холсте 108dp согласно [Android adaptive icons](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive).

Промпт v2:

> Use case: style-transfer / logo-brand. Redesign this Android kanji-learning launcher mascot into a polished contemporary mobile app icon foreground. Preserve idea: recognizable Japanese 字 as a playful winking character holding a calligraphy brush. Dramatically simplify illustration into a bold compact geometric mascot, clean smooth rounded ink-black strokes, playful confident wink and small smile, just one restrained coral accent on simple brush handle. Remove paper texture, scratchy edges, blush texture, eyebrows, motion marks, little dangling brush loop, long spindly legs and excessive detail. Short integrated limbs, chunky readable silhouette, carefully balanced negative spaces; premium friendly flat vector-like app brand aesthetic with a very subtle dimensional bevel at most, no shiny plastic. Single centered character, transparent background, no tile, no backdrop, no words, no border, no drop shadow. All black strokes must remain opaque black. Generous transparent padding; subject bounds approximately central 65 percent of square canvas so Android masks have safe margins. Character is not a generic face: maintain the recognizable structure of 字. Output one finished icon foreground asset.

## Предыдущий вариант v1

Создана встроенным `image_gen` 19 октября 2026 года. Концепция: довольный подмигивающий 字 с кистью, с юмором обыгрывающий название приложения. Тёплая бумага, чёрная тушь, коралловые акценты; без мелкой надписи.

Исходник: [launcher-icon-source.png](launcher-icon-source.png). [Предпросмотр круглой адаптивной маски](launcher-icon-preview.png).

В приложении используются адаптивная цветная и монохромная версии, а также обычные со скруглёнными углами и круглые растровые ресурсы mdpi–xxxhdpi для Android 7. Адаптивный передний слой имеет прозрачные внешние поля. Объект уменьшен и расположен в центре с запасом под адаптивную маску. Монохромная версия использует маску непрозрачности рисунка без бумажного фона. Manifest уже ссылается на `ic_launcher` и `ic_launcher_round`.

Промпт генерации:

> Use case: logo-brand. Create a beautiful polished humorous Android launcher icon for a Japanese kanji handwriting learning app named «Кандзи дэс нэ» (a playful Russian transliteration of 'kanji desu ne', 'kanji, isn't it?'). No words or lettering. A charming anthropomorphic bold ink-black Japanese character 字, drawn accurately and recognizably, acting as a proudly pleased slightly goofy little calligraphy student: one playful winking eye and small cheeky smile integrated delicately into the central negative space, holding a tiny wooden calligraphy brush at the lower right. The humor is quiet self-satisfied 'nailed it!' confidence. Strong simple silhouette, premium friendly editorial cartoon illustration, crisp black ink strokes with subtle natural brush texture, very restrained warm coral red accent on brush handle and blush, warm ivory paper background. Square full bleed flat ivory background, no outer rounded square or circle border, no lettering, no mockup, no shadow outside icon. All important art including brush inside central 60 percent of canvas so Android adaptive masks do not crop it. Tiny-size legibility, minimal details, contemporary Japanese stationery charm, original character design. Single final icon.

Проверка v2: сборка debug и Android Lint успешны, обновлённое приложение установлено на Pixel. Визуально просмотрены маски и малый размер в предпросмотре упаковки.
