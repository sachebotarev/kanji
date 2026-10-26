# Android-навыки проекта

Установлены локально в `.agents/skills/`; доступны для автоматического выбора со следующего сообщения. В текущей работе инструкции читаются непосредственно. Требования проекта: [architecture.md](architecture.md). При конфликте навыков с `architecture.md` побеждает архитектура проекта.

| Навык | Когда применять |
| --- | --- |
| `android-architecture` | Границы слоёв, модули, DI |
| `android-viewmodel` | Состояние экрана, восстановление, lifecycle |
| `android-data-layer` | Repository, Room, offline-first (без навязывания Hilt/сети) |
| `compose-ui` | Атомарные виджеты и Compose-экраны |
| `android-coroutines` | Фоновая работа, Flow, отмена проверки |
| `kotlin-concurrency-expert` | Разбор проблем с корутинами и потоками |
| `compose-performance-audit` | Задержка ввода и проблемы отрисовки |
| `gradle-build-performance` | Медленная сборка, build scan, узкие места |
| `android-testing` | Минимальные поведенческие тесты |
| `android-emulator-skill` | Скрипты эмулятора/adb (рядом с MCP `adb`) |
| `android-accessibility` | Semantics, TalkBack и масштабирование |
| `android-gradle-logic` | Gradle-модули, каталог версий, toolchain |
| `coil-compose` | Загрузка изображений в Compose, если появится сеть/Coil |
| `android-retrofit` | HTTP/Retrofit, только если сеть явно нужна |

Источник базового набора: [new-silvermoon/awesome-android-agent-skills](https://github.com/new-silvermoon/awesome-android-agent-skills), commit `82900eacc8dbe13de93c6310af27b9df4b2bd2f6`. Первые восемь навыков установлены через skill-installer и существенно переработаны для проекта 19 октября 2026 года. 21 октября 2026 добавлены недостающие skills из того же набора **без перезаписи** кастомных. Navigation 3 skill **не** подключали: в upstream — alpha API, это запрещено `architecture.md`; в проекте уже Navigation Compose.

В каждом каталоге сохранена Apache-2.0 лицензия и отметка об изменении / project note.

Настройки: обязательный чистый domain, независимые виджеты, DI через конструкторы с ручной сборкой по умолчанию, Hilt опционален. Убраны экспериментальные backing fields, legacy-примеры, жёстко заданные версии и обязательная инфраструктура Hilt/screenshot-тестов. Оптимизации Compose выполняются по необходимости, без механического добавления remember и аннотаций стабильности.

## MCP для устройства

Зарегистрирован глобально в `~/.cursor/mcp.json` сервер `adb` → `adb-mcp` v0.23.0 (`~/.local/bin/adb-mcp`, SDK `~/Library/Android/sdk`). После добавления перезапустите MCP / Cursor, подключите эмулятор или телефон (`adb devices`) и попросите агента сделать screenshot / `doctor`.

Обновление исходного набора не должно перезаписывать проектные настройки: сначала сравнить изменения, проверить stable API и совместимость с `doc/architecture.md`.
