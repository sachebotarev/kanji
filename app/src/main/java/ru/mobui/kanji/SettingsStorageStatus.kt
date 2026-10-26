package ru.mobui.kanji

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
internal fun SettingsStorageStatus(failed: Boolean, onRetry: () -> Unit) {
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().padding(24.dp),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            if (failed) {
                Text(stringResource(R.string.settings_storage_error))
                Button(onRetry) { Text(stringResource(R.string.settings_storage_retry)) }
            } else CircularProgressIndicator()
        }
    }
}
