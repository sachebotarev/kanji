package ru.mobui.kanji.feature.dashboard.builder

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.mobui.kanji.domain.catalog.CatalogSnapshot
import ru.mobui.kanji.domain.catalog.DictionaryTag
import ru.mobui.kanji.domain.catalog.MaterialType
import ru.mobui.kanji.domain.catalog.RecipeMaterial
import ru.mobui.kanji.domain.catalog.WordTagAssignment
import ru.mobui.kanji.domain.catalog.title
import ru.mobui.kanji.feature.dashboard.R
import ru.mobui.kanji.feature.dashboard.widgets.DashboardIcon
import ru.mobui.kanji.feature.dashboard.widgets.IllustratedIcon

@Composable
internal fun RecipeMaterialRow(
    material: RecipeMaterial,
    snapshot: CatalogSnapshot,
    tags: List<DictionaryTag>,
    assignments: List<WordTagAssignment>,
    language: String,
    onOpen: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
) {
    val label = stringResource(
        when (material.type) {
            MaterialType.Book -> R.string.material_book_type
            MaterialType.Dictionary -> R.string.material_dictionary_type
            MaterialType.Kanji -> R.string.material_kanji_type
            MaterialType.Words -> R.string.material_words_type
        },
    )
    val chaptersWord = stringResource(R.string.material_chapters)
    val chaptersMore = stringResource(R.string.material_chapters_and_more)
    val morePattern = stringResource(R.string.material_more)
    val kanjiTitle = stringResource(R.string.material_kanji_title)
    val conditions = buildList {
        if (material.chapters.isNotEmpty()) {
            add(chapterTagLabel(material.chapters, chaptersWord) { chaptersMore.format(it) })
        }
        if (material.kanjiIds.isNotEmpty()) {
            add(
                kanjiTitle + ": " +
                    material.kanjiIds.sorted().take(3).mapNotNull { snapshot.kanjiById[it]?.symbol }.joinToString(" ") +
                    if (material.kanjiIds.count() > 3) " · " + morePattern.format(material.kanjiIds.count() - 3) else "",
            )
        }
        if (material.tagIds.isNotEmpty()) {
            add(
                dictionaryTagsLabel(tags.filter { it.id in material.tagIds }.map { it.name(language) }) {
                    morePattern.format(it)
                },
            )
        }
        if (material.type == MaterialType.Words) {
            add(pluralStringResource(R.plurals.material_words_selected, material.wordIds.size, material.wordIds.size))
        }
    }
    val source = material.sourceId?.let(snapshot.sourceById::get)
    val title = source?.title(language) ?: label
    val cover = when (source?.coverKey) {
        "sage" -> ru.mobui.kanji.core.designsystem.R.drawable.catalog_tobira
        "blue" -> ru.mobui.kanji.core.designsystem.R.drawable.catalog_basic
        else -> null
    }
    val typeArt = when (material.type) {
        MaterialType.Book -> DashboardIcon.Textbooks
        MaterialType.Dictionary -> DashboardIcon.Personal
        MaterialType.Kanji -> DashboardIcon.References
        MaterialType.Words -> DashboardIcon.WordSet
    }
    val openLabel = stringResource(R.string.recipe_open_material, title)
    val interactive = onOpen != null || onRemove != null
    @Composable
    fun Body(showChevron: Boolean, chipConditions: Boolean) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (cover != null) {
                Image(painterResource(cover), null, Modifier.size(28.dp))
            } else {
                IllustratedIcon(typeArt, Modifier.size(28.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (conditions.isNotEmpty()) {
                    if (chipConditions) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            conditions.forEach { condition ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    shape = RoundedCornerShape(6.dp),
                                ) {
                                    Text(
                                        condition,
                                        Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                        }
                    } else {
                        // Read-only: plain meta text — chips read as tappable controls.
                        Text(
                            conditions.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (showChevron) {
                Icon(
                    painterResource(ru.mobui.kanji.core.wordui.R.drawable.selection_chevron_down),
                    null,
                    Modifier.size(18.dp).rotate(-90f),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    if (!interactive) {
        // TRAIN.02: content only — parent panel supplies the quiet substrate.
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .testTag("recipe-material-${material.id}"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Body(showChevron = false, chipConditions = false)
        }
        return
    }
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(start = 10.dp, end = 4.dp, top = 12.dp, bottom = 12.dp)
                .testTag("recipe-material-${material.id}"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onOpen != null) {
                Surface(
                    onClick = onOpen,
                    Modifier
                        .weight(1f)
                        .semantics {
                            contentDescription = openLabel
                            role = Role.Button
                        },
                    color = Color.Transparent,
                ) {
                    Body(showChevron = onRemove == null, chipConditions = true)
                }
            } else {
                Row(Modifier.weight(1f)) {
                    Body(showChevron = false, chipConditions = true)
                }
            }
            if (onRemove != null) {
                IconButton(onRemove, Modifier.size(48.dp).testTag("recipe-remove-${material.id}")) {
                    Icon(
                        painterResource(R.drawable.recipe_remove),
                        stringResource(R.string.recipe_remove),
                        Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
