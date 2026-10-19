---
name: compose-performance-audit
description: Investigate slow Compose rendering, excessive recomposition or handwriting latency.
---

# compose-performance-audit

Modified for the Kanji project on 2026-10-19 from new-silvermoon/awesome-android-agent-skills, commit 82900eacc8dbe13de93c6310af27b9df4b2bd2f6 (Apache-2.0).

Start with the affected code and reproducible symptom. Read doc/architecture.md. Inspect state reads in composition/layout/drawing and repeated path allocation. For handwriting keep active stroke updates scoped to drawing and avoid rebuilding the screen per point. Measure frame timing in release/profileable builds when needed; debug inspection is useful for diagnosis but not release timing. Do not blanket-remember modifiers/lambdas or add Stable/Immutable unless their contracts are actually true. Strong skipping behavior depends on the installed compiler; inspect it. Make targeted fixes with evidence, no experimental dependencies.
