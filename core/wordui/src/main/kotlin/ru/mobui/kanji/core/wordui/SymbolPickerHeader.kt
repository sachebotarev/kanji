package ru.mobui.kanji.core.wordui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import ru.mobui.kanji.core.designsystem.KanjiSearchToggle
import ru.mobui.kanji.core.designsystem.LocalSearchOpen

@Composable
fun SymbolPickerHeader(
    title: String,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    searchOpen: Boolean = LocalSearchOpen.current,
    backTag: String = "symbol-picker-back",
    searchTag: String = "symbol-picker-search-toggle",
) {
    Row(modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onBack, Modifier.testTag(backTag)) {
            Icon(
                painterResource(ru.mobui.kanji.core.designsystem.R.drawable.ic_arrow_back),
                stringResource(R.string.selection_back),
                Modifier.size(24.dp),
            )
        }
        Text(
            title,
            Modifier.weight(1f).padding(horizontal = 8.dp),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        KanjiSearchToggle(
            open = searchOpen,
            onToggle = onSearch,
            testTag = searchTag,
        )
    }
}
