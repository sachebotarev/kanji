package ru.mobui.kanji.feature.catalog.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ru.mobui.kanji.core.designsystem.KanjiListContentPadding
import ru.mobui.kanji.core.designsystem.KanjiSectionTitle
import ru.mobui.kanji.feature.catalog.CatalogSection
import ru.mobui.kanji.feature.catalog.R
import ru.mobui.kanji.feature.catalog.components.CatalogArtwork
import ru.mobui.kanji.feature.catalog.components.CatalogIllustratedCard
import ru.mobui.kanji.feature.catalog.components.SourceCard
import ru.mobui.kanji.feature.catalog.components.catalogMaterialSummary

@Composable
internal fun CatalogHomeScreen(
    sources: List<SourceSummary>,
    language: String,
    navigate: (BrowseNavigation) -> Unit,
    modifier: Modifier = Modifier,
    section: CatalogSection = CatalogSection.Combined,
) {
    val listState = rememberCatalogListState()
    val visibleSources = if (section == CatalogSection.Textbooks) {
        sources.filter { it.source.sourceType == "textbook" || it.source.sourceType == "book" }
    } else sources
    LazyColumn(
        modifier,
        state = listState,
        userScrollEnabled = listState.canScrollForward || listState.canScrollBackward,
        contentPadding = KanjiListContentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (section != CatalogSection.Combined) item {
            Column {
                CatalogHomeIntro(section)
                Spacer(Modifier.height(12.dp))
            }
        }
        if (section != CatalogSection.References) {
            if (section == CatalogSection.Combined) item {
                HomeSectionHeading(stringResource(R.string.catalog_sources))
            }
            items(visibleSources, key = { it.source.id }) { summary ->
                SourceCard(
                    summary.source,
                    language,
                    catalogMaterialSummary(summary.counts.kanji, summary.counts.words),
                    { navigate(BrowseNavigation.Source(summary.source.id)) },
                )
            }
        }
        if (section != CatalogSection.Textbooks) {
            if (section == CatalogSection.Combined) item {
                HomeSectionHeading(stringResource(R.string.catalog_reference), Modifier.padding(top = 24.dp))
            }
            item {
                CatalogIllustratedCard(
                    stringResource(R.string.catalog_all_kanji),
                    stringResource(R.string.catalog_reference_kanji_description),
                    R.drawable.catalog_reference_kanji,
                    { navigate(BrowseNavigation.AllKanji) },
                    Modifier.testTag("catalog-all-kanji"),
                )
            }
            item {
                CatalogIllustratedCard(
                    stringResource(R.string.catalog_all_radicals),
                    stringResource(R.string.catalog_reference_radical_description),
                    R.drawable.catalog_reference_radical,
                    { navigate(BrowseNavigation.AllRadicals) },
                    Modifier.testTag("catalog-all-radicals"),
                )
            }
        }
        if (section == CatalogSection.Combined) item {
            Box(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp), contentAlignment = Alignment.Center) {
                CatalogArtwork(R.drawable.catalog_reading, Modifier.size(156.dp), mascot = true)
            }
        }
    }
}

@Composable
private fun HomeSectionHeading(title: String, modifier: Modifier = Modifier) {
    KanjiSectionTitle(title, modifier.padding(start = 4.dp, bottom = 4.dp).semantics { heading() })
}
