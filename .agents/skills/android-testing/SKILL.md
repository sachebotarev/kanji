---
name: android-testing
description: Add minimal meaningful JVM or Compose interaction tests for changed Android behavior.
---

# android-testing

Modified for the Kanji project on 2026-10-19 from new-silvermoon/awesome-android-agent-skills, commit 82900eacc8dbe13de93c6310af27b9df4b2bd2f6 (Apache-2.0).

Read doc/architecture.md and the changed contract. Honor the requested test scope. Domain rules use JVM tests; widget gestures use Compose UI tests with fake state and callbacks, without DI or recognizer infrastructure. Verify observable behavior, not private implementation or wording. UI-PAD essentials: normalized completed stroke and cancellation/disabled input. Add tests for stale async results when implementing the state owner, not the drawing widget. No screenshot framework, Hilt or new test stack without a concrete need. Run affected tests, build and lint; report device-dependent tests that could not execute.
