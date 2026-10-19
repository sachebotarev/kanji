---
name: android-viewmodel
description: Implement screen state and ViewModels with stable Kotlin and lifecycle-aware Compose integration.
---

# android-viewmodel

Modified for the Kanji project on 2026-10-19 from new-silvermoon/awesome-android-agent-skills, commit 82900eacc8dbe13de93c6310af27b9df4b2bd2f6 (Apache-2.0).

Read doc/architecture.md and the relevant screen specification. Expose read-only StateFlow using private MutableStateFlow and asStateFlow; do not introduce experimental backing-field syntax or upgrade Kotlin as a side effect. Collect using collectAsStateWithLifecycle. Delegate business rules to domain scenarios. Keep business-significant outcomes in state, not replay=0 SharedFlow. Use viewModelScope and model cancellation and restoration explicitly. Pass state and callbacks to widgets, never ViewModels. Keep active drawing local and persist completed strokes outside the widget.
