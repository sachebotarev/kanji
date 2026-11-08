# java.time на minSdk 24

**Решение (2026-11-08):** оставить `minSdk = 24` и включить `coreLibraryDesugaring` (`desugar_jdk_libs` 2.1.5) вместо подъёма minSdk до 26.

## Контекст

`ActivityOverview`, история тренировок и связанные модели используют `java.time.*`. Эти классы есть в Android только с API 26; без desugaring на Android 7.0–7.1 возможен краш `NoClassDefFoundError`.

## Выбор

| Вариант | Итог |
| --- | --- |
| `coreLibraryDesugaring` | Сохраняет Android 7.x; рекомендуемый путь платформы |
| `minSdk = 26` | Проще конфигурация, но отрезает API 24–25 |

Выбран первый вариант: поддержка API 24 актуальна.

## Где включено

- `:app` и `:feature:dashboard` — `compileOptions.isCoreLibraryDesugaringEnabled = true` и зависимость `coreLibraryDesugaring(libs.desugar.jdk.libs)`.
- Версия зафиксирована в `gradle/libs.versions.toml` (`desugarJdkLibs = "2.1.5"`, совместима с AGP 8+/9.x).

Справка: [Java 8+ API desugaring](https://developer.android.com/studio/write/java8-support#library-desugaring), [desugar_jdk_libs CHANGELOG](https://github.com/google/desugar_jdk_libs/blob/master/CHANGELOG.md).
