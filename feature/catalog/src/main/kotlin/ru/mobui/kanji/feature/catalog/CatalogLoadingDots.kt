package ru.mobui.kanji.feature.catalog

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
internal fun CatalogLoadingDots(modifier: Modifier = Modifier) {
    val resolver = LocalContext.current.contentResolver
    fun motionEnabled() = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    var motion by remember(resolver) { mutableStateOf(motionEnabled()) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { motion = motionEnabled() }
        }
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    val phase = if (motion) {
        val transition = rememberInfiniteTransition(label = "catalog-loading")
        transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "dots-phase").value
    } else 0f
    Row(modifier.height(16.dp).clearAndSetSemantics {}, horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { index ->
            val lift = if (motion) sin((phase + index / 3f) * Math.PI * 2).toFloat() * 3f else 0f
            Box(Modifier.offset(y = lift.dp).size(4.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = .35f), CircleShape))
        }
    }
}
