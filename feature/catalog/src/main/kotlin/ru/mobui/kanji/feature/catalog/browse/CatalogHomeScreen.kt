package ru.mobui.kanji.feature.catalog.browse

import ru.mobui.kanji.feature.catalog.R
import ru.mobui.kanji.feature.catalog.components.CatalogArtwork
import ru.mobui.kanji.feature.catalog.components.CatalogIllustratedCard
import ru.mobui.kanji.feature.catalog.components.SourceCard
import ru.mobui.kanji.feature.catalog.components.catalogMaterialSummary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun CatalogHomeScreen(
    sources: List<SourceSummary>,
    language: String,
    navigate: (BrowseNavigation) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberCatalogListState()
    LazyColumn(
        modifier,
        state = listState,
        userScrollEnabled = listState.canScrollForward || listState.canScrollBackward,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                stringResource(R.string.catalog_sources),
                Modifier.padding(start = 4.dp, bottom = 4.dp).semantics { heading() },
                style =
                    MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        lineHeight = 24.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    ),
            )
        }
        items(sources, key = { it.source.id }) { summary ->
            val source = summary.source
            SourceCard(
                source,
                language,
                catalogMaterialSummary(
                    summary.counts.kanji,
                    summary.counts.words,
                ),
                { navigate(BrowseNavigation.Source(source.id)) },
            )
        }
        item {
            Text(
                stringResource(R.string.catalog_reference),
                Modifier.padding(start = 4.dp, top = 24.dp, bottom = 4.dp).semantics { heading() },
                style =
                    MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        lineHeight = 24.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    ),
            )
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
        item {
            Box(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp), contentAlignment = Alignment.Center) {
                CatalogArtwork(R.drawable.catalog_reading, Modifier.size(156.dp), mascot = true)
            }
        }
    }
}
