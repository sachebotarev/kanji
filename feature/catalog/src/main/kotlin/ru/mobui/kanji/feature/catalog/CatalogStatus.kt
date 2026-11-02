package ru.mobui.kanji.feature.catalog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun CatalogStatus(error: Boolean, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)
        .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CatalogArtwork(if (error) R.drawable.catalog_error else R.drawable.catalog_loading, Modifier.size(180.dp), mascot = true)
        Spacer(Modifier.height(24.dp))
        Text(stringResource(if (error) R.string.catalog_error else R.string.catalog_loading),
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, lineHeight = 28.sp),
            color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(16.dp))
        if (error) Button(onRetry, Modifier.width(220.dp).heightIn(min = 48.dp)) { Text(stringResource(R.string.catalog_retry)) }
        else CatalogLoadingDots()
    }
}
