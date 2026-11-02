# Android-навыки проекта

Навыки доступны локально в `.agents/skills/`. Требования проекта: [architecture.md](architecture.md). При конфликте навыков с `architecture.md` побеждает архитектура проекта.

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

Источник базового Android-набора: [new-silvermoon/awesome-android-agent-skills](https://github.com/new-silvermoon/awesome-android-agent-skills), commit `82900eacc8dbe13de93c6310af27b9df4b2bd2f6`. Локальные инструкции адаптированы под проект, лицензии сохранены. Не перезаписывать адаптацию внешними шаблонами без проверки совместимости.

Для UI сначала читать [бриф и статус дизайна](design-system/README.md), [компоненты](design-system/04-components/README.md) и [спецификацию нужного модуля](design-system/05-screens/README.md). Источник действующих архитектурных требований — architecture.md. DI через конструкторы, чистый domain и stable API обязательны независимо от примеров навыка.

## MCP для устройства

Зарегистрирован глобально в `~/.cursor/mcp.json` сервер `adb` → `adb-mcp` v0.23.0 (`~/.local/bin/adb-mcp`, SDK `~/Library/Android/sdk`). После добавления перезапустите MCP / Cursor, подключите эмулятор или телефон (`adb devices`) и попросите агента сделать screenshot / `doctor`.

Обновление исходного набора не должно перезаписывать проектные настройки: сначала сравнить изменения, проверить stable API и совместимость с `doc/architecture.md`.

## Навыки дизайн-системы

Дизайн-навыки в `.agents/skills/`:

| Навык | Роль | Источник |
| --- | --- | --- |
| `mobile-ui-ux-designer` | Основная UX-методология: задачи пользователей, сценарии, состояния, доступность и передача спецификаций | [mdrmuhaimin/agentic-skills](https://github.com/mdrmuhaimin/agentic-skills/tree/main/skills/mobile-ui-ux-designer) |
| `material-3` | Стандарты Android: Material 3, Compose, токены, компоненты и адаптивная компоновка | [hamen/material-3-skill](https://github.com/hamen/material-3-skill/tree/master/skills/material-3) |
| `frontend-design` | Дополнительная визуальная экспертиза: композиция, типографика, палитра и визуальная индивидуальность | [anthropics/skills](https://github.com/anthropics/skills/tree/main/skills/frontend-design) |

В задачах дизайн-системы сначала применять UX-методологию, затем стандарты Material 3. `frontend-design` применять избирательно: веб-паттерны, CSS/HTML, hero-секции и hover-поведение не переносить механически в мобильное приложение. Приоритет имеют требования проекта, Android и Material 3, доступность и поддержка русского и японского текста.

Примеры версий, Expressive и experimental API из внешних навыков не являются разрешением на их использование. Перед реализацией проверять совместимость со стеком проекта; допускаются только стабильные API без opt-in согласно `architecture.md`. Исходные навыки сохранены без изменения.
