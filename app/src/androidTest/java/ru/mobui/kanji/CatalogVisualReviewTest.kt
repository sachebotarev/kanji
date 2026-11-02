package ru.mobui.kanji

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import java.io.File
import org.junit.Rule
import org.junit.Test

/** Device review artifacts. The title and tabs stay pinned; filters scroll with the list. */
class CatalogVisualReviewTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun waitTag(tag: String) {
        compose.waitUntil(20_000) {
            compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun screenshot(name: String) {
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        val image = compose.onRoot().captureToImage().asAndroidBitmap()
        val directory = File(compose.activity.filesDir, "catalog-review").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use {
            image.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test
    fun capturesCatalogAndRestoresToolbarOnReverseScroll() {
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
        compose.onNodeWithTag("catalog-back").assertIsDisplayed()
        compose.onNodeWithTag("catalog-tab-1").assertIsDisplayed()
        compose.onNodeWithTag("kanji-grid").performTouchInput { swipeUp() }
        compose.onNodeWithTag("catalog-back").assertIsDisplayed()
        compose.onNodeWithTag("catalog-tab-1").assertIsDisplayed()
        compose.onNodeWithTag("catalog-filter-radical").assertIsNotDisplayed()
        screenshot("scrolled")
        compose.onNodeWithTag("kanji-grid").performTouchInput { swipeDown() }
        compose.onNodeWithTag("catalog-filter-radical").assertIsDisplayed()
        compose.onNodeWithTag("catalog-tab-1").assertIsDisplayed()
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
        compose
            .onNodeWithTag("word-list")
            .onChildren()
            .filter(hasClickAction())
            .onFirst()
            .performClick()
        waitTag("word-written")
        screenshot("word-card")
    }
}
