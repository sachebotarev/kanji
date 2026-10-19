---
name: android-gradle-logic
description: Configure compatible stable Gradle modules, version catalogs and convention plugins.
---

# android-gradle-logic

Modified for the Kanji project on 2026-10-19 from new-silvermoon/awesome-android-agent-skills, commit 82900eacc8dbe13de93c6310af27b9df4b2bd2f6 (Apache-2.0).

Read doc/architecture.md and existing settings/catalog before editing. Use Kotlin DSL and centralized versions, align Compose through BOM. Verify JDK/Gradle/AGP/Kotlin compatibility against official documentation. Do not copy old version numbers or apply org.jetbrains.kotlin.android blindly to AGP with built-in Kotlin. Use Kotlin JVM for pure domain modules. Introduce convention plugins when real module setup repeats; avoid speculative build infrastructure. Use implementation by default, api only when public signatures require transitivity. No prereleases, experimental flags, kapt or unrelated upgrades. Run module build and lint.
