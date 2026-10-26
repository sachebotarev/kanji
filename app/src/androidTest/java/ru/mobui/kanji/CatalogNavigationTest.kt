package ru.mobui.kanji

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class CatalogNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun waitTag(tag: String) {
        try { compose.waitUntil(30_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() } }
        catch (error: AssertionError) { compose.onRoot().printToLog("CatalogFailure"); throw error }
    }
    private fun openCatalog() {
        waitTag("open-catalog")
        compose.onNodeWithTag("open-catalog").performScrollTo().performClick()
        waitTag("source-1")
    }

    @Test fun combinesChapterAndRadicalOpensCardsAndRestoresFilters() {
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
        compose.onNodeWithTag("catalog-search").performTextInput("85")
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Ничего не найдено").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("catalog-search").performTextClearance()
        waitTag("radical-1")
        compose.onNodeWithTag("radical-1").performClick()
        waitTag("kanji-19968")
        compose.onNodeWithTag("kanji-19968").performClick()
        waitTag("kanji-symbol")
        compose.onNodeWithTag("kanji-key").performScrollTo().performClick()
        waitTag("kanji-grid")
        androidx.test.espresso.Espresso.pressBack()
        waitTag("kanji-symbol")
        androidx.test.espresso.Espresso.pressBack()
        waitTag("catalog-filter-chapter")
        compose.activityRule.scenario.recreate()
        waitTag("catalog-filter-chapter")
        compose.onNodeWithText("Глава: 3").assertExists()
        compose.onNodeWithText("Ключ: 一").assertExists()
        compose.onNodeWithTag("catalog-tab-2").performClick()
        waitTag("word-list")
        compose.onNodeWithTag("catalog-filter-kanji").performClick()
        waitTag("kanji-19968")
        compose.onNodeWithTag("kanji-19968").performClick()
        waitTag("catalog-filter-kanji")
        compose.onNodeWithText("Кандзи: 一").assertExists()
        compose.onAllNodesWithTag("word-list").onFirst().onChildren().filter(hasClickAction()).onFirst().performClick()
        waitTag("word-written")
        androidx.test.espresso.Espresso.pressBack()
        waitTag("word-list")
    }

    @Test fun referencesOpenAndBackReturnsToDashboard() {
        openCatalog()
        compose.onNodeWithTag("catalog-all-kanji").performScrollTo().performClick()
        waitTag("kanji-grid")
        compose.onNodeWithTag("catalog-search").performTextInput("月")
        waitTag("kanji-26376")
        compose.onNodeWithTag("kanji-26376").performClick()
        waitTag("kanji-symbol")
        androidx.test.espresso.Espresso.pressBack()
        waitTag("catalog-search")
        compose.onNodeWithTag("catalog-search").assertTextContains("月")
        androidx.test.espresso.Espresso.pressBack()
        waitTag("source-1")
        androidx.test.espresso.Espresso.pressBack()
        waitTag("dashboard-screen")
    }
}
