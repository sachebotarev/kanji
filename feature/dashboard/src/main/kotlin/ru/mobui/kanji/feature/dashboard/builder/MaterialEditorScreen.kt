package ru.mobui.kanji.feature.dashboard.builder

import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import ru.mobui.kanji.core.wordui.SymbolGridItem
import ru.mobui.kanji.core.wordui.kanjiSymbolFacets
import ru.mobui.kanji.feature.dashboard.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import ru.mobui.kanji.domain.catalog.CatalogSnapshot
import ru.mobui.kanji.domain.catalog.DictionaryTag
import ru.mobui.kanji.domain.catalog.MaterialType
import ru.mobui.kanji.domain.catalog.RecipeMaterial
import ru.mobui.kanji.domain.catalog.WordTagAssignment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import ru.mobui.kanji.domain.catalog.meaning
import ru.mobui.kanji.domain.catalog.localized

/** One material editor, with isolated, restorable drafts per type. No repository writes. */
@Composable
internal fun MaterialEditorScreen(initial: RecipeMaterial, snapshot: CatalogSnapshot,
    tags: List<DictionaryTag>, assignments: List<WordTagAssignment>, language: String,
    onApply: (RecipeMaterial) -> Unit, onBack: () -> Unit, onWord: (Int) -> Unit,
    onManageTags: () -> Unit, modifier: Modifier = Modifier, editing: Boolean = false) {
    key(initial.id) {
        var typeKey by rememberSaveable { mutableStateOf(initial.type.key) }
        val type = MaterialType.entries.first { it.key == typeKey }
        val drafts = rememberSaveableStateHolder()
        val missing = stringResource(ru.mobui.kanji.core.wordui.R.string.word_missing)
        val entries = remember(snapshot, language, missing) {
            snapshot.kanji.map { sign ->
                val meaning = sign.meaning(language) ?: missing
                SymbolGridItem(sign.id, sign.symbol, sign.meanings.localized(language)?.firstOrNull() ?: meaning,
                    sign.symbol + ", " + meaning)
            }
        }
        val byWord = remember(snapshot) {
            snapshot.words.associate { it.id to snapshot.literalKanji(it).map { sign -> sign.id }.toSet() }
        }
        val facets = kanjiSymbolFacets(snapshot.kanji, snapshot.radicals, language)
        val selector: @Composable () -> Unit = { MaterialTypeSelector(type, { typeKey = it.key }) }
        drafts.SaveableStateProvider(type.key) {
            val draft = if (type == initial.type) initial else RecipeMaterial(initial.id, type)
            when (type) {
                MaterialType.Book -> BookMaterialEditorScreen(draft, snapshot, language, onApply, onBack, onWord,
                    modifier, editing, selector)
                MaterialType.Dictionary -> DictionaryMaterialEditorScreen(draft, snapshot, tags, assignments,
                    language, onApply, onBack, onWord, onManageTags, modifier, editing, selector)
                MaterialType.Kanji -> KanjiMaterialEditorScreen(draft, snapshot.words, entries, byWord, language,
                    onApply, onBack, onWord, modifier, editing, facets, selector, snapshot.kanji)
                MaterialType.Words -> WordMaterialEditorScreen(draft, snapshot.words, entries, byWord, language,
                    onApply, onBack, onWord, modifier, editing, facets, selector)
            }
        }
    }
}

@Composable
private fun MaterialTypeSelector(selected: MaterialType, onSelect: (MaterialType) -> Unit) {
    val types = listOf(MaterialType.Book, MaterialType.Dictionary, MaterialType.Kanji, MaterialType.Words)
    val labels = listOf(R.string.material_book_type, R.string.material_dictionary_type,
        R.string.material_kanji_type, R.string.material_words_type)
    val hints = listOf(R.string.material_book_type_hint, R.string.material_dictionary_type_hint,
        R.string.material_kanji_type_hint, R.string.material_words_type_hint)
    val selectedIndex = types.indexOf(selected)
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MaterialSectionTitle(stringResource(R.string.material_type_title))
        types.chunked(2).forEachIndexed { row, choices ->
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                choices.forEachIndexed { column, type ->
                    val index = row * 2 + column
                    val chosen = type == selected
                    Surface(
                        Modifier.weight(1f).fillMaxHeight().heightIn(min = 56.dp)
                            .selectable(chosen, role = Role.RadioButton, onClick = { onSelect(type) })
                            .testTag("material-type-${type.key}"),
                        shape = RoundedCornerShape(16.dp),
                        color = if (chosen) scheme.secondaryContainer else scheme.surfaceContainer,
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            RadioButton(chosen, null, Modifier.size(20.dp))
                            Text(
                                stringResource(labels[index]),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                color = if (chosen) scheme.onSecondaryContainer else scheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
        if (selectedIndex >= 0) {
            MaterialSupportingText(
                stringResource(hints[selectedIndex]),
                Modifier.testTag("material-type-hint"),
            )
        }
    }
}
