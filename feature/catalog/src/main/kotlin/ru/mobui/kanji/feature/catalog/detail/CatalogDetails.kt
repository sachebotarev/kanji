package ru.mobui.kanji.feature.catalog.detail

import ru.mobui.kanji.feature.catalog.R
import ru.mobui.kanji.feature.catalog.browse.rememberCatalogColumnState
import ru.mobui.kanji.feature.catalog.components.CatalogEmptyState
import ru.mobui.kanji.feature.catalog.components.CatalogMaterialCount
import ru.mobui.kanji.feature.catalog.components.CatalogPanel
import ru.mobui.kanji.feature.catalog.components.CatalogRow
import ru.mobui.kanji.feature.catalog.components.DetailColumn
import ru.mobui.kanji.feature.catalog.components.KanjiGrid
import ru.mobui.kanji.feature.catalog.components.NumberingHelpButton
import ru.mobui.kanji.feature.catalog.components.numbers
import ru.mobui.kanji.feature.catalog.components.radicalDisplayName
import ru.mobui.kanji.feature.catalog.filter.jlptLabel
import ru.mobui.kanji.feature.catalog.presentation.MaterialKind
import ru.mobui.kanji.feature.catalog.theme.CatalogJapaneseColor
import ru.mobui.kanji.feature.catalog.theme.CatalogJapaneseStyle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.mobui.kanji.domain.catalog.CatalogRadical
import ru.mobui.kanji.domain.catalog.JlptFilter
import ru.mobui.kanji.domain.catalog.SchoolStage
import ru.mobui.kanji.domain.catalog.meaning
import ru.mobui.kanji.domain.catalog.name

@Composable
internal fun KanjiDetail(
    state: DetailContent.Kanji,
    language: String,
    onRadical: (Int) -> Unit,
    onWords: () -> Unit,
) {
    val kanji = state.kanji
    if (kanji == null) {
        Text(stringResource(R.string.catalog_not_found))
        return
    }
    DetailColumn {
        Column {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                KanjiDrawing(
                    kanji.symbol,
                    state.drawing,
                    Modifier.testTag("kanji-symbol").fillMaxWidth(),
                )
                Text(
                    kanji.meaning(language) ?: stringResource(R.string.catalog_missing),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    pluralStringResource(R.plurals.catalog_strokes, kanji.strokes, kanji.strokes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (kanji.on.isNotEmpty() || kanji.kun.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (kanji.on.isNotEmpty())
                    CatalogReadings(stringResource(R.string.catalog_on), kanji.on)
                if (kanji.kun.isNotEmpty())
                    CatalogReadings(stringResource(R.string.catalog_kun), kanji.kun)
            }
        }
        val hasClassification = listOf(
            kanji.schoolStage != null,
            kanji.jlptLevel != null,
            kanji.joyo,
            kanji.kyoiku,
            kanji.jinmeiyo,
        ).any { it }
        if (hasClassification)
            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                kanji.schoolStage?.let { stage ->
                    ClassificationTag(
                        when (stage) {
                            SchoolStage.Elementary ->
                                kanji.schoolYear?.let {
                                    stringResource(R.string.catalog_school_elementary, it)
                                } ?: stringResource(R.string.catalog_school_elementary_unknown)
                            SchoolStage.JuniorHigh -> stringResource(R.string.catalog_school_junior)
                            SchoolStage.HighSchool -> stringResource(R.string.catalog_school_high)
                        },
                    )
                }
                run {
                    val labels =
                        listOfNotNull(
                            kanji.jlptLevel?.let { jlptLabel(JlptFilter.Exact(it)) },
                            if (kanji.joyo) stringResource(R.string.catalog_joyo) else null,
                            if (kanji.kyoiku) stringResource(R.string.catalog_kyoiku) else null,
                            if (kanji.jinmeiyo) stringResource(R.string.catalog_jinmeiyo) else null,
                        )
                    labels.forEach { ClassificationTag(it) }
                }
            }
        state.radical?.let { radical ->
            CatalogRow(
                stringResource(R.string.catalog_radical),
                radical.symbol +
                    " · " +
                    radicalDisplayName(radical, language),
                { onRadical(radical.id) },
                Modifier.testTag("kanji-key"),
                inlineSubtitle = true,
            )
        }
        CatalogRow(
            stringResource(R.string.catalog_related_words),
            pluralStringResource(R.plurals.catalog_word_count, state.wordCount, state.wordCount),
            onWords,
            inlineSubtitle = true,
        )
    }
}

@Composable
private fun ClassificationTag(label: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(
            label,
            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun CatalogReadings(label: String, readings: List<String>) {
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            readings.joinToString("、"),
            style =
                CatalogJapaneseStyle.copy(
                    fontSize = 28.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.Medium,
                ),
        )
    }
}

@Composable
internal fun RadicalDetail(
    state: DetailContent.Radical,
    language: String,
    query: String,
    onKanji: (Int) -> Unit,
) {
    val radical = state.radical
    if (radical == null) {
        Text(stringResource(R.string.catalog_not_found))
        return
    }
    val numbers = state.rows.numbers
    if (state.rows.entries.isEmpty()) {
        val scroll = rememberCatalogColumnState()
        Column(Modifier.fillMaxSize().verticalScroll(scroll, enabled = scroll.maxValue > 0)) {
            RadicalIntro(radical, language, inset = true)
            CatalogMaterialCount(0, 0, MaterialKind.Kanji)
            CatalogEmptyState(query.isNotBlank(), reportScroll = false)
        }
    } else
        KanjiGrid(
            state.rows.entries,
            language,
            onKanji,
            Modifier.fillMaxSize(),
            numbers = numbers,
            header = {
                Column {
                    RadicalIntro(radical, language, inset = false)
                    CatalogMaterialCount(
                        state.rows.entries.count(),
                        state.rows.entries.count(),
                        MaterialKind.Kanji,
                        inset = false,
                        action =
                            if (numbers.isNotEmpty()) {
                                { NumberingHelpButton(stringResource(R.string.catalog_placement_legend)) }
                            } else null,
                    )
                }
            },
        )
}

@Composable
private fun RadicalIntro(radical: CatalogRadical, language: String, inset: Boolean) {
    Column(
        Modifier.padding(
            start = if (inset) 18.dp else 0.dp,
            end = if (inset) 18.dp else 0.dp,
            top = 12.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        CatalogPanel {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    radical.symbol,
                    style = CatalogJapaneseStyle.copy(fontSize = 64.sp, lineHeight = 76.sp),
                    color = CatalogJapaneseColor,
                )
                Text(
                    radicalDisplayName(radical, language),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    stringResource(
                        R.string.catalog_radical_metadata,
                        radical.id,
                        pluralStringResource(
                            R.plurals.catalog_strokes,
                            radical.strokes,
                            radical.strokes,
                        ),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        ru.mobui.kanji.core.designsystem.KanjiSectionTitle(
            stringResource(R.string.catalog_related_kanji),
            Modifier.padding(horizontal = 4.dp),
        )
    }
}
