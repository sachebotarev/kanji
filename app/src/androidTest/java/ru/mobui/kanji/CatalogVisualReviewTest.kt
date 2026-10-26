package ru.mobui.kanji

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import android.graphics.Bitmap
import java.io.File
import org.junit.Rule
import org.junit.Test

/** Device review artifacts plus the enter-always interaction requested for long catalog lists. */
class CatalogVisualReviewTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun waitTag(tag: String) {
        compose.waitUntil(20_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun screenshot(name: String) {
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        val image = compose.onRoot().captureToImage().asAndroidBitmap()
        val directory = File(compose.activity.filesDir, "catalog-review").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    @Test fun capturesCatalogAndRestoresToolbarOnReverseScroll() {
        waitTag("open-catalog")
        compose.onNodeWithTag("open-catalog").performScrollTo().performClick()
        waitTag("source-1")
        screenshot("home")
        compose.onNodeWithTag("source-1").performClick()
        waitTag("chapter-3")
        screenshot("chapters")
        compose.onNodeWithTag("chapter-3").performClick()
        waitTag("kanji-grid")
        compose.onNodeWithTag("catalog-tab-2").performClick()
        waitTag("word-list")
        screenshot("chapter-words")
        androidx.test.espresso.Espresso.pressBack()
        waitTag("chapter-3")
        compose.onNodeWithTag("catalog-tab-1").performClick()
        waitTag("kanji-19968")
        screenshot("kanji")
        compose.onNodeWithTag("kanji-grid").performTouchInput { swipeUp() }
        compose.onNodeWithTag("catalog-search").assertIsNotDisplayed()
        screenshot("scrolled")
        compose.onNodeWithTag("kanji-grid").performTouchInput { swipeDown() }
        compose.onNodeWithTag("catalog-search").assertIsDisplayed()
        screenshot("restored")
        compose.onNodeWithTag("kanji-grid").performScrollToNode(hasTestTag("kanji-19968"))
        compose.onNodeWithTag("kanji-19968").performClick()
        waitTag("kanji-symbol")
        screenshot("kanji-card")
        compose.onNodeWithTag("kanji-key").performScrollTo().performClick()
        waitTag("kanji-grid")
        screenshot("radical-card")
        androidx.test.espresso.Espresso.pressBack()
        waitTag("kanji-symbol")
        androidx.test.espresso.Espresso.pressBack()
        waitTag("catalog-tab-2")
        compose.onNodeWithTag("catalog-tab-2").performClick()
        waitTag("word-list")
        screenshot("words")
        compose.onNodeWithTag("word-list").onChildren().filter(hasClickAction()).onFirst().performClick()
        waitTag("word-written")
        screenshot("word-card")
    }
}
