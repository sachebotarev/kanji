package ru.mobui.kanji

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.printToLog
import org.junit.Rule
import org.junit.Test

class CatalogNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun waitTag(tag: String) {
        try {
            compose.waitUntil(30_000) {
                compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
            }
            compose.waitForIdle()
        } catch (error: androidx.compose.ui.test.ComposeTimeoutException) {
            compose.onRoot().printToLog("CatalogFailure")
            throw error
        }
    }

    private fun openCatalog() {
        waitTag("open-catalog")
        compose.onNodeWithTag("open-catalog").bringIntoViewIfNeeded().performClick()
        waitTag("source-1")
    }

    @Test
    fun combinesChapterAndRadicalOpensCardsAndRestoresFilters() {
        openCatalog()
        compose.onNodeWithTag("source-1").performClick()
        waitTag("chapter-3")
        compose.onNodeWithTag("catalog-tab-1").performClick()
        waitTag("kanji-19968")
        compose.onNodeWithTag("catalog-filter-chapter").performClick()
        waitTag("chapter-3")
        compose.onNodeWithTag("chapter-3").performClick()
        waitTag("catalog-filter-radical")
        compose.onNodeWithTag("catalog-filter-radical").performClick()
        waitTag("radical-1")
        compose.onNodeWithTag("catalog-toggle-search").performClick()
        compose.onNodeWithTag("catalog-search").performTextInput("999999")
        compose.waitUntil(5_000) {
            compose
                .onAllNodesWithText(
                    compose.activity.getString(
                        ru.mobui.kanji.feature.catalog.R.string.catalog_empty,
                    ),
                )
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        compose.onNodeWithTag("catalog-search").performTextClearance()
        waitTag("radical-1")
        compose.onNodeWithTag("radical-1").performClick()
        waitTag("kanji-19968")
        compose.onNodeWithTag("kanji-19968").performClick()
        waitTag("kanji-symbol")
        compose.onNodeWithTag("kanji-key").bringIntoViewIfNeeded().performClick()
        waitTag("kanji-grid")
        androidx.test.espresso.Espresso.pressBack()
        waitTag("kanji-symbol")
        androidx.test.espresso.Espresso.pressBack()
        waitTag("catalog-filter-chapter")
        compose.activityRule.scenario.recreate()
        waitTag("catalog-filter-chapter")
        compose
            .onNodeWithText(
                compose.activity.getString(
                    ru.mobui.kanji.feature.catalog.R.string.catalog_chapter_filter,
                    3,
                ),
            )
            .assertExists()
        compose
            .onNodeWithText(
                compose.activity.getString(
                    ru.mobui.kanji.feature.catalog.R.string.catalog_radical_filter,
                    "一",
                ),
            )
            .assertExists()
        compose.onNodeWithTag("catalog-tab-2").performClick()
        waitTag("word-list")
        compose.onNodeWithTag("catalog-filter-kanji").performClick()
        waitTag("kanji-19968")
        compose.onNodeWithTag("kanji-19968").performClick()
        waitTag("catalog-filter-kanji")
        compose
            .onNodeWithText(
                compose.activity.getString(
                    ru.mobui.kanji.feature.catalog.R.string.catalog_kanji_filter,
                    "一",
                ),
            )
            .assertExists()
        compose
            .onAllNodesWithTag("word-list")
            .onFirst()
            .onChildren()
            .filter(hasClickAction())
            .onFirst()
            .performClick()
        waitTag("word-written")
        androidx.test.espresso.Espresso.pressBack()
        waitTag("word-list")
    }

    @Test
    fun referencesOpenAndBackReturnsToDashboard() {
        waitTag("open-references")
        compose.onNodeWithTag("open-references").bringIntoViewIfNeeded().performClick()
        waitTag("catalog-all-kanji")
        compose.onNodeWithTag("catalog-all-kanji").bringIntoViewIfNeeded().performClick()
        waitTag("kanji-grid")
        compose.onNodeWithTag("catalog-toggle-search").performClick()
        compose.onNodeWithTag("catalog-search").performTextInput("月")
        waitTag("kanji-26376")
        compose.onNodeWithTag("kanji-26376").performClick()
        waitTag("kanji-symbol")
        androidx.test.espresso.Espresso.pressBack()
        waitTag("catalog-search")
        compose.onNodeWithTag("catalog-search").assertTextContains("月")
        compose.onNodeWithTag("catalog-search").performImeAction()
        compose.waitForIdle()
        androidx.test.espresso.Espresso.pressBack()
        waitTag("catalog-all-kanji")
        androidx.test.espresso.Espresso.pressBack()
        waitTag("dashboard-screen")
    }
}
