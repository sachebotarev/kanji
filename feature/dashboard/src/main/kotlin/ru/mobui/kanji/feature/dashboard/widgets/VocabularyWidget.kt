package ru.mobui.kanji.feature.dashboard.widgets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import ru.mobui.kanji.feature.dashboard.*
import ru.mobui.kanji.feature.dashboard.R

@Composable
fun VocabularyWidget(state: VocabularyOverview, modifier: Modifier = Modifier, onOpenCatalog: () -> Unit = {}, onOpenDictionary: () -> Unit = {}) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.dashboard_dictionaries), Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.height(IntrinsicSize.Min), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Surface(Modifier.fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                color = MaterialTheme.colorScheme.surfaceContainer) {
                Row(Modifier.fillMaxWidth().clickable(onClick = onOpenCatalog).testTag("open-catalog").padding(horizontal = 16.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    IllustratedIcon(DashboardIcon.Textbooks, Modifier.size(34.dp))
                    Column {
                        Text(stringResource(R.string.dashboard_textbooks), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.dashboard_catalog_description),
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Surface(Modifier.fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer) {
                Row(Modifier.fillMaxWidth().clickable(onClick = onOpenDictionary).testTag("open-dictionary").padding(horizontal = 16.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    IllustratedIcon(DashboardIcon.Personal, Modifier.size(34.dp))
                    Column {
                        Text(stringResource(R.string.dashboard_personal), style = MaterialTheme.typography.titleMedium)
                        Text(state.personalTotal?.let { pluralStringResource(R.plurals.dashboard_personal_words, it, it) }
                            ?: stringResource(R.string.dashboard_personal_description),
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
