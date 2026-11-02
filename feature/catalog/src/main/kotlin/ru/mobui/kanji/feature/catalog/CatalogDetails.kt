package ru.mobui.kanji.feature.catalog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.mobui.kanji.domain.catalog.*

@Composable
internal fun KanjiDetail(state: DetailContent.Kanji, language: String, onRadical: (Int) -> Unit, onWords: () -> Unit) {
    val kanji = state.kanji
    if (kanji == null) { Text(stringResource(R.string.catalog_not_found)); return }
    DetailColumn {
        CatalogPanel {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                KanjiDrawing(kanji.symbol, state.drawing, Modifier.testTag("kanji-symbol").fillMaxWidth())
                Text(kanji.meaning(language) ?: stringResource(R.string.catalog_missing),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, lineHeight = 28.sp), textAlign = TextAlign.Center)
                Text(pluralStringResource(R.plurals.catalog_strokes, kanji.strokes, kanji.strokes),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (kanji.on.isNotEmpty()) CatalogReadings(stringResource(R.string.catalog_on), kanji.on)
            if (kanji.kun.isNotEmpty()) CatalogReadings(stringResource(R.string.catalog_kun), kanji.kun)
            if (kanji.on.isEmpty() && kanji.kun.isEmpty()) Text(stringResource(R.string.catalog_no_reading))
            else Text(stringResource(R.string.catalog_reading_hint), Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (kanji.schoolStage != null || kanji.jlptLevel != null || kanji.joyo || kanji.kyoiku || kanji.jinmeiyo) CatalogPanel(spacing = 4.dp) {
            kanji.schoolStage?.let { stage ->
                Text(when (stage) {
                    SchoolStage.Elementary -> kanji.schoolYear?.let { stringResource(R.string.catalog_school_elementary, it) }
                        ?: stringResource(R.string.catalog_school_elementary_unknown)
                    SchoolStage.JuniorHigh -> stringResource(R.string.catalog_school_junior)
                    SchoolStage.HighSchool -> stringResource(R.string.catalog_school_high)
                }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val labels = listOfNotNull(kanji.jlptLevel?.let { jlptLabel(JlptFilter.Exact(it)) },
                    if (kanji.joyo) stringResource(R.string.catalog_joyo) else null,
                    if (kanji.kyoiku) stringResource(R.string.catalog_kyoiku) else null,
                    if (kanji.jinmeiyo) stringResource(R.string.catalog_jinmeiyo) else null)
                labels.forEach { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        state.radical?.let { radical ->
            CatalogRow(stringResource(R.string.catalog_radical), radical.symbol + " · " + (radical.name(language) ?: stringResource(R.string.catalog_missing)),
                { onRadical(radical.id) }, Modifier.testTag("kanji-key"))
        }
        CatalogRow(stringResource(R.string.catalog_related_words), pluralStringResource(R.plurals.catalog_word_count, state.wordCount, state.wordCount), onWords)
    }
}

@Composable
private fun CatalogReadings(label: String, readings: List<String>) {
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(readings.joinToString(" · "), style = CatalogJapaneseStyle.copy(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Medium))
    }
}

@Composable
internal fun RadicalDetail(state: DetailContent.Radical, language: String, query: String, onKanji: (Int) -> Unit) {
    val radical = state.radical
    if (radical == null) { Text(stringResource(R.string.catalog_not_found)); return }
    val numbers = state.rows.numbers
    if (state.rows.entries.isEmpty()) {
        val scroll = rememberCatalogColumnState()
        Column(Modifier.fillMaxSize().verticalScroll(scroll, enabled = scroll.maxValue > 0)) {
        RadicalIntro(radical, language, inset = true)
        CatalogMaterialCount(0, 0, MaterialKind.Kanji)
        CatalogEmptyState(query.isNotBlank(), reportScroll = false)
        }
    } else KanjiGrid(state.rows.entries, language, onKanji, Modifier.fillMaxSize(), numbers = numbers, header = {
        Column {
            RadicalIntro(radical, language, inset = false)
            CatalogMaterialCount(state.rows.entries.size, state.rows.entries.size, MaterialKind.Kanji, inset = false)
            if (numbers.isNotEmpty()) CatalogHint(stringResource(R.string.catalog_placement_legend), inset = false)
        }
    })
}

@Composable
private fun RadicalIntro(radical: CatalogRadical, language: String, inset: Boolean) {
    Column(Modifier.padding(start = if (inset) 20.dp else 0.dp, end = if (inset) 20.dp else 0.dp, top = 12.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        CatalogPanel {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(radical.symbol, style = CatalogJapaneseStyle.copy(fontSize = 64.sp, lineHeight = 76.sp), color = CatalogJapaneseColor)
                Text(radical.name(language) ?: stringResource(R.string.catalog_missing),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, lineHeight = 28.sp), textAlign = TextAlign.Center)
                Text(stringResource(R.string.catalog_radical_metadata, radical.id,
                    pluralStringResource(R.plurals.catalog_strokes, radical.strokes, radical.strokes)),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(stringResource(R.string.catalog_related_kanji), Modifier.padding(horizontal = 4.dp), style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp))
    }
}
