package ru.mobui.kanji

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.Dispatchers
import java.util.Locale
import ru.mobui.kanji.core.designsystem.KanjiTheme
import ru.mobui.kanji.data.kanjivg.BundledKanjiReferenceSource
import ru.mobui.kanji.domain.practice.KanjiId
import ru.mobui.kanji.feature.catalog.CatalogReviewContent

/** Kept exclusively in the debug source set; no production deep link or data mutation. */
class CatalogReviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val language = intent.getStringExtra("language") ?: "ru"
        val dark = intent.getBooleanExtra("dark", false)
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !dark; isAppearanceLightNavigationBars = !dark
        }
        val scenario = intent.getIntExtra("scenario", 1)
        val variant = intent.getStringExtra("variant").orEmpty()
        val scale = intent.getFloatExtra("scale", 1f)
        val configuration = Configuration(resources.configuration).apply { setLocale(Locale.forLanguageTag(language)); fontScale = scale }
        val localized = createConfigurationContext(configuration)
        val references = BundledKanjiReferenceSource(assets, Dispatchers.IO)
        val repository = (application as KanjiApplication).catalog
        setContent {
            CompositionLocalProvider(LocalContext provides localized, LocalConfiguration provides configuration,
                LocalDensity provides Density(resources.displayMetrics.density, scale)) {
                KanjiTheme(dark) {
                    CatalogReviewContent(repository, scenario, language, dark, variant) { references.load(KanjiId("u${it.toString(16)}")) }
                }
            }
        }
    }
}
