---
name: android-accessibility
description: Review and implement Compose accessibility semantics, touch targets and scaling.
---

# android-accessibility

Modified for the Kanji project on 2026-10-19 from new-silvermoon/awesome-android-agent-skills, commit 82900eacc8dbe13de93c6310af27b9df4b2bd2f6 (Apache-2.0).

Read the component specification. Ensure controls have at least 48dp touch targets, actionable labels, logical focus order and meaningful state descriptions. Decorative icons have null descriptions. Color must not be the only status signal. Check text scaling and contrast. Canvas requires explicit semantics; do not announce pointer moves or timer seconds. Never expose an unrevealed kanji answer through accessibility labels. Verify relevant semantics through focused Compose tests or device inspection.
