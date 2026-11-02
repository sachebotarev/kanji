package ru.mobui.kanji.feature.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun CatalogStatus(error: Boolean, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp).semantics {
            liveRegion = LiveRegionMode.Polite
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CatalogArtwork(
            if (error) R.drawable.catalog_error else R.drawable.catalog_loading,
            Modifier.size(180.dp),
            mascot = true,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(if (error) R.string.catalog_error else R.string.catalog_loading),
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, lineHeight = 28.sp),
            color =
                if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(16.dp))
        if (error)
            Button(onRetry, Modifier.width(220.dp).heightIn(min = 48.dp)) {
                Text(stringResource(R.string.catalog_retry))
            }
        else CatalogLoadingDots()
    }
}
