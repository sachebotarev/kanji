package ru.mobui.kanji.feature.practice

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Local disclosure state never changes the setting or invokes its callbacks. */
@Composable
internal fun SettingHelp(
    label: Int,
    help: Int,
    tag: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    initiallyExpanded: Boolean = false,
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    val title = stringResource(label)
    val action = stringResource(if (expanded) R.string.help_collapse else R.string.help_expand, title)
    val description = stringResource(if (expanded) R.string.help_expanded else R.string.help_collapsed)
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .testTag("help-toggle-$tag")
                .clickable(role = Role.Button, onClickLabel = action) { expanded = !expanded }
                .semantics { stateDescription = description },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            value?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            Icon(painterResource(if (expanded) R.drawable.ic_help_collapse else R.drawable.ic_setting_info),
                contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
        }
        if (expanded) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).testTag("help-content-$tag"),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Text(stringResource(help), Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Preview(locale = "ru", widthDp = 360)
@Composable
private fun ExpandedHelpPreview() {
    MaterialTheme(colorScheme = lightColorScheme()) {
        Surface { SettingHelp(R.string.setting_translation_limit, R.string.help_translation, "translation",
            value = "12%", initiallyExpanded = true) }
    }
}

@Preview(locale = "en", widthDp = 360, fontScale = 1.5f)
@Composable
private fun CollapsedHelpPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface { SettingHelp(R.string.setting_translation_limit, R.string.help_translation, "translation", value = "12%") }
    }
}
