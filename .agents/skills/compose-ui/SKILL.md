---
name: compose-ui
description: Implement atomic Compose widgets and screens with explicit state and event contracts.
---

# compose-ui

Modified for the Kanji project on 2026-10-19 from new-silvermoon/awesome-android-agent-skills, commit 82900eacc8dbe13de93c6310af27b9df4b2bd2f6 (Apache-2.0).

Read doc/architecture.md and relevant doc/components and doc/screens specifications. Use stable Compose and Material 3 APIs. Public widgets accept state, callbacks and Modifier as first optional parameter applied at the root. No DI, navigation, recognition or persistence in widgets. Local gesture/focus/animation state is allowed. Use theme tokens and resource strings. Add representative previews. Do not add remember, derivedStateOf or stability annotations mechanically. For UI-PAD read doc/implementation/ui-pad.md; preserve ordered original points in 0..1 coordinates independently of rendering.
