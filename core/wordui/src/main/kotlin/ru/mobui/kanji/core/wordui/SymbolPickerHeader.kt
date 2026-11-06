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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

@Composable
fun SymbolPickerHeader(title: String, onBack: () -> Unit, onSearch: () -> Unit,
    modifier: Modifier = Modifier, backTag: String = "symbol-picker-back", searchTag: String = "symbol-picker-search-toggle") {
    Row(modifier.fillMaxWidth().padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onBack, Modifier.testTag(backTag)) { Icon(painterResource(R.drawable.selection_back), stringResource(R.string.selection_back)) }
        Text(
            title,
            Modifier.weight(1f),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        IconButton(onSearch, Modifier.testTag(searchTag)) { Icon(painterResource(R.drawable.selection_search), stringResource(R.string.selection_open_search)) }
    }
}
