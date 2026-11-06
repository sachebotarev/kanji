# Перенос карточек: штатный Android Drag and Drop

## Правило выбора API

Для любой задачи сначала рассматриваются штатные возможности соответствующей платформы; собственная реализация допустима только для отсутствующего поведения и с обоснованием. Возможности и актуальное состояние библиотек проверяются через Context7 по официальной документации, затем сопоставляются с подключённой версией. Эти обязательные правила закреплены в `AGENTS.md`. Для переноса приоритет — штатные механизмы Android/Compose, а не собственное распознавание жеста и имитация переноса.

Обновление зависимостей и experimental/preview API с opt-in допускаются по предварительному явному согласованию с пользователем. Без согласования они запрещены; сама проверка документации таким согласованием не является. Для experimental API согласование относится к конкретному API и области использования и фиксируется в [реестре](experimental-api.md).

## Проверка 2026-11-05

- Проект: Compose BOM `2026.02.01`; разрешённые Gradle артефакты Foundation/UI — `1.10.5`; minSdk 24. Новые зависимости не требуются.
- Context7: `/websites/developer_android_develop_ui_compose`, `/websites/developer_android_reference_kotlin_androidx_compose`.
- [Официальное руководство](https://developer.android.com/develop/ui/compose/touch-input/user-interactions/drag-and-drop): `Modifier.dragAndDropSource`, `dragAndDropTarget`, lifecycle `DragAndDropTarget`, данные `DragAndDropTransferData`.
- [Стабильная перегрузка источника](https://developer.android.com/reference/kotlin/androidx/compose/foundation/draganddrop/package-summary): `dragAndDropSource(transferData)` доступна с 1.8.0, создаёт системную drag shadow и использует стандартное распознавание начала переноса. Не использовать устаревшую experimental-перегрузку с самостоятельным detector.
- [Android-данные события](https://developer.android.com/reference/kotlin/androidx/compose/ui/draganddrop/package-summary): `toAndroidDragEvent()` позволяет прочесть локальное состояние и координаты.
- [LazyListState](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/LazyListState), [ScrollScope.scrollBy](https://developer.android.com/reference/kotlin/androidx/compose/foundation/gestures/ScrollScope): штатные операции прокрутки. Документация этих API не обещает автоматическую перестановку модели LazyColumn или краевую автопрокрутку при Android DragEvent.
- Дополнительная проверка по исходникам Foundation/UI `1.10.5` из Gradle source JAR подтвердила доступность стабильных перегрузок без opt-in. Проверка подключённой версии важнее предположения по наиболее свежему примеру.

## Выбор

Системе передаются долгий захват, drag session, shadow, доставка move/drop/end и отмена. Используются Compose source/target; данные принимаются только из текущего экземпляра списка через Android localState, без глобального экспорта данных.

Прикладной слой отвечает только за порядок UUID, область допустимого сброса, сохранение единожды на drop, откат при завершении без drop и запрос штатной прокрутки у края. Соседи анимируются `LazyColumn.animateItem`. Радиокнопка, меню и раскрытие не являются источниками переноса. Поиск отключает перенос и доступные команды перестановки.

## Статус

Реализовано в `RecipeReorderState.kt` и списке рецептов. Собственный pointer detector удалён. На Pixel 10 Pro Android 17 проверены реальные системные drag sessions, отмена, исключённые элементы управления и автопрокрутка вниз/вверх за пределы исходного viewport. Полный dashboard: 59 тестов без ошибок. [Отчёт и границы проверки](../verification/recipe-library-android.md). Синтетический Compose gesture сам по себе не считается доказательством работы Android drag session; тест использует Android UiAutomation.
