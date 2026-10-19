# Android-навыки проекта

Установлены локально в `.agents/skills/`; доступны для автоматического выбора со следующего сообщения. В текущей работе инструкции читаются непосредственно. Требования проекта: [architecture.md](architecture.md).

| Навык | Когда применять |
| --- | --- |
| `android-architecture` | Границы слоёв, модули, DI |
| `android-viewmodel` | Состояние экрана, восстановление, lifecycle |
| `compose-ui` | Атомарные виджеты и Compose-экраны |
| `android-coroutines` | Фоновая работа, Flow, отмена проверки |
| `compose-performance-audit` | Задержка ввода и проблемы отрисовки |
| `android-testing` | Минимальные поведенческие тесты |
| `android-accessibility` | Semantics, TalkBack и масштабирование |
| `android-gradle-logic` | Gradle-модули, каталог версий, toolchain |

Источник: [new-silvermoon/awesome-android-agent-skills](https://github.com/new-silvermoon/awesome-android-agent-skills), commit `82900eacc8dbe13de93c6310af27b9df4b2bd2f6`. Навыки установлены через skill-installer и существенно переработаны для проекта 19 октября 2026 года. В каждом каталоге сохранена Apache-2.0 лицензия и отметка об изменении.

Настройки: обязательный чистый domain, независимые виджеты, DI через конструкторы с ручной сборкой по умолчанию, Hilt опционален. Убраны экспериментальные backing fields, legacy-примеры, жёстко заданные версии и обязательная инфраструктура Hilt/screenshot-тестов. Оптимизации Compose выполняются по необходимости, без механического добавления remember и аннотаций стабильности.

Обновление исходного набора не должно перезаписывать проектные настройки: сначала сравнить изменения, проверить stable API и совместимость с `doc/architecture.md`.
