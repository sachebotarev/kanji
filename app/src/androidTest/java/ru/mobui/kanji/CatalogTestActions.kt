package ru.mobui.kanji

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.performScrollTo

/** Fitted detail pages intentionally have no scroll action; visible targets need no scroll. */
internal fun SemanticsNodeInteraction.bringIntoViewIfNeeded(): SemanticsNodeInteraction =
    if (isDisplayed()) this else performScrollTo()
