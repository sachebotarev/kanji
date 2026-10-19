---
name: android-architecture
description: Design module boundaries, Clean Architecture and dependency injection for this Android project.
---

# android-architecture

Modified for the Kanji project on 2026-10-19 from new-silvermoon/awesome-android-agent-skills, commit 82900eacc8dbe13de93c6310af27b9df4b2bd2f6 (Apache-2.0).

Read doc/architecture.md first. Use feature-first modules with presentation depending on pure Kotlin domain contracts; data/checker implementations depend on domain. App assembles implementations. Keep internals internal and module dependencies acyclic. Domain has no Android or DI annotations. Constructor injection with manual composition is the initial choice; Hilt is optional after a documented decision. No service locators. Do not create empty modules or pass-through use cases. A widget has no ViewModel, repository or navigation. Verify affected module builds and dependency direction.
