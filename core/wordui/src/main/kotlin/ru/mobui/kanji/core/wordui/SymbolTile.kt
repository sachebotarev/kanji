package ru.mobui.kanji.core.wordui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text

val JapaneseSymbolFont = FontFamily(Font(R.font.catalog_japanese))
val JapaneseSymbolStyle = TextStyle(fontFamily = JapaneseSymbolFont, localeList = LocaleList("ja-JP"),
    platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
        androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,
        androidx.compose.ui.text.style.LineHeightStyle.Trim.Both,
        androidx.compose.ui.text.style.LineHeightStyle.Mode.Tight))

/** The original catalogue tile, shared by browsing and all selection contexts. */
@Composable
fun SymbolTile(symbol: String, caption: String, chosen: Boolean, onClick: () -> Unit,
    modifier: Modifier = Modifier, ordinal: String? = null, strokes: String? = null,
    fullCaption: Boolean = false, containerColor: Color = MaterialTheme.colorScheme.surfaceContainer) {
    val density = LocalDensity.current
    Surface(onClick, modifier.semantics { selected = chosen }, shape = MaterialTheme.shapes.medium,
        color = if (chosen) MaterialTheme.colorScheme.primaryContainer else containerColor) {
        Box {
            Column(Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp,
                top = if (ordinal == null) 18.dp else 24.dp, bottom = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(symbol, style = JapaneseSymbolStyle.copy(fontSize = 36.sp, lineHeight = 44.sp), color = MaterialTheme.colorScheme.primary)
                val captionHeight = with(density) { (kotlin.math.ceil(20.sp.toPx()) * 2).toFloat().toDp() }
                Box(Modifier.fillMaxWidth().then(if (fullCaption) Modifier.heightIn(min = captionHeight) else Modifier.height(captionHeight)),
                    contentAlignment = Alignment.Center) {
                    Text(caption, style = MaterialTheme.typography.bodyMedium.copy(lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
                        androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,
                        androidx.compose.ui.text.style.LineHeightStyle.Trim.None,
                        androidx.compose.ui.text.style.LineHeightStyle.Mode.Tight)),
                        textAlign = TextAlign.Center, maxLines = if (fullCaption) Int.MAX_VALUE else 2, overflow = TextOverflow.Ellipsis)
                }
                if (strokes != null) Text(strokes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (ordinal != null) Text(ordinal, Modifier.align(Alignment.TopEnd).padding(top = 7.dp, end = 10.dp),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (chosen) Text("✓", Modifier.align(Alignment.TopStart).padding(8.dp).clearAndSetSemantics {}, color = MaterialTheme.colorScheme.primary)
        }
    }
}
