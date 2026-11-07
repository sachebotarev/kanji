package ru.mobui.kanji.core.wordui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.mobui.kanji.domain.catalog.DictionaryTag
import ru.mobui.kanji.domain.catalog.TagColor
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text

/**
 * Fixed dictionary-tag palette from `dictionary-tags.css` (`--tag-color` / `--tag-color-dark`).
 * Not derived from Material roles — mockup keeps these HEX values across themes.
 */
fun TagColor.swatch(dark: Boolean): Color = when (this) {
    TagColor.Neutral -> if (dark) TagSwatchColors.NeutralDark else TagSwatchColors.NeutralLight
    TagColor.Green -> if (dark) TagSwatchColors.GreenDark else TagSwatchColors.GreenLight
    TagColor.Blue -> if (dark) TagSwatchColors.BlueDark else TagSwatchColors.BlueLight
    TagColor.Violet -> if (dark) TagSwatchColors.VioletDark else TagSwatchColors.VioletLight
    TagColor.Pink -> if (dark) TagSwatchColors.PinkDark else TagSwatchColors.PinkLight
    TagColor.Orange -> if (dark) TagSwatchColors.OrangeDark else TagSwatchColors.OrangeLight
    TagColor.Gold -> if (dark) TagSwatchColors.GoldDark else TagSwatchColors.GoldLight
}

private object TagSwatchColors {
    val NeutralDark = Color(0xFFA3AAA6)
    val NeutralLight = Color(0xFF717875)
    val GreenDark = Color(0xFF81C99F)
    val GreenLight = Color(0xFF28744C)
    val BlueDark = Color(0xFF91B9EF)
    val BlueLight = Color(0xFF366DB3)
    val VioletDark = Color(0xFFC5A2EC)
    val VioletLight = Color(0xFF8358AE)
    val PinkDark = Color(0xFFEEA0C3)
    val PinkLight = Color(0xFFAD527C)
    val OrangeDark = Color(0xFFF0AC83)
    val OrangeLight = Color(0xFFB65E32)
    val GoldDark = Color(0xFFDFC16A)
    val GoldLight = Color(0xFF94721C)
}

@Composable
private fun tagPaletteDark(): Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f

/** Decorative 10 dp category color. Selection is always conveyed independently. */
@Composable
fun TagDot(color: TagColor, modifier: Modifier = Modifier) {
    Box(modifier.size(10.dp).background(color.swatch(tagPaletteDark()), CircleShape))
}

/** 28 dp swatch in a 48 dp touch target — matches `.dictionary-tag-color-option` / `.dictionary-tag-swatch`. */
@Composable
fun TagSwatch(
    color: TagColor,
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier
            .size(48.dp)
            .clip(CircleShape)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, radius = 24.dp),
                onClick = onClick,
            )
            .semantics {
                contentDescription = label
                this.selected = selected
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(28.dp)
                .background(color.swatch(tagPaletteDark()), CircleShape)
                .then(
                    if (selected) {
                        Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                    } else {
                        Modifier
                    },
                ),
        )
    }
}

/**
 * Inline tag summary for editor navigation rows: colored dots + names, joined with " · ".
 * Matches HTML `dictionaryTagInlineNames`.
 */
@Composable
fun TagInlineNames(
    tags: List<DictionaryTag>,
    language: String,
    modifier: Modifier = Modifier,
    empty: String,
    limit: Int = 2,
) {
    if (tags.isEmpty()) {
        Text(
            empty,
            modifier,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    FlowRow(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val visible = tags.take(limit)
        visible.forEachIndexed { index, tag ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TagDot(tag.color)
                Text(
                    tag.name(language),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (index < visible.lastIndex || tags.count() > limit) {
                    Text(
                        " · ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (tags.count() > limit) {
            Text(
                stringResource(R.string.tags_more, tags.count() - limit),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun TagChips(tags: List<DictionaryTag>, language: String, modifier: Modifier = Modifier, limit: Int = Int.MAX_VALUE, compact: Boolean = false) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        tags.take(limit).forEach { tag ->
            Surface(shape = RoundedCornerShape(if (compact) 6.dp else 12.dp),
                color = if (compact) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerHigh) {
                Row(Modifier.padding(horizontal = if (compact) 8.dp else 10.dp, vertical = if (compact) 3.dp else 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 6.dp)) {
                    TagDot(tag.color)
                    Text(tag.name(language),
                        style = if (compact) MaterialTheme.typography.labelMedium.copy(lineHeight = 18.sp) else MaterialTheme.typography.labelMedium)
                }
            }
        }
        if (tags.count() > limit) Text(stringResource(R.string.tags_more, tags.count() - limit),
            style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(vertical = 6.dp))
    }
}

/** Same selectable tag row for dictionary and recipe material pickers. No navigation or storage. */
@Composable
fun TagChoiceRow(tag: DictionaryTag, language: String, chosen: Boolean, onClick: () -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true, subtitle: String? = null,
    managing: Boolean = false, onDelete: (() -> Unit)? = null) {
    Surface(onClick, modifier.fillMaxWidth().testTag("tag-${tag.id}")
        .then(if (managing) Modifier else Modifier.semantics { selected = chosen }),
        enabled = enabled, shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = if (chosen && !managing) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null) {
        Box {
            Row(
                Modifier.heightIn(min = 64.dp).padding(
                    start = if (managing) 16.dp else 32.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp,
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TagDot(tag.color)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(tag.name(language), style = MaterialTheme.typography.titleMedium)
                    if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (managing) {
                    androidx.compose.material3.Icon(androidx.compose.ui.res.painterResource(R.drawable.selection_edit),
                        null, Modifier.size(20.dp))
                    if (onDelete != null) androidx.compose.material3.IconButton(onDelete,
                        Modifier.size(48.dp).testTag("tag-delete-${tag.id}"), enabled = enabled) {
                        androidx.compose.material3.Icon(androidx.compose.ui.res.painterResource(R.drawable.selection_delete),
                            stringResource(R.string.tag_picker_delete, tag.name(language)))
                    }
                }
            }
            if (!managing && chosen) {
                Text(
                    "✓",
                    Modifier.align(Alignment.TopStart).padding(8.dp).clearAndSetSemantics {},
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
