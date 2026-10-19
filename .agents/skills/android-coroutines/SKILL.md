---
name: android-coroutines
description: Implement cancellation-safe background processing and Flow in Android.
---

# android-coroutines

Modified for the Kanji project on 2026-10-19 from new-silvermoon/awesome-android-agent-skills, commit 82900eacc8dbe13de93c6310af27b9df4b2bd2f6 (Apache-2.0).

Read doc/architecture.md. Use structured concurrency, main-safe suspend operations and read-only Flow for observations. Inject dispatchers where blocking or CPU work needs explicit dispatch. Preserve CancellationException and cooperate with cancellation in long comparisons. Avoid GlobalScope and lifecycle-unaware collection. UI-PAD itself performs no background recognition. External check requests carry content versions; discard stale responses even if cancellation races. Test concurrency with a shared test scheduler when behavior requires it.
