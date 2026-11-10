package ru.mobui.kanji.feature.catalog.navigation

import ru.mobui.kanji.feature.catalog.R
import ru.mobui.kanji.feature.catalog.browse.LocalCatalogScrollable
import ru.mobui.kanji.feature.catalog.components.CatalogEmptyState
import ru.mobui.kanji.feature.catalog.components.CatalogSearch
import ru.mobui.kanji.feature.catalog.components.CatalogStatus
import ru.mobui.kanji.feature.catalog.components.SearchUi
import ru.mobui.kanji.feature.catalog.presentation.CatalogLoad

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/** Catalog chrome only: no repository, screen switch, filter rules or navigation graph. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CatalogScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String = "",
    search: SearchUi? = null,
    home: Boolean = false,
    resetKey: Any? = Unit,
    tabs: (@Composable () -> Unit)? = null,
    tools: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val behavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val contentScrolls = remember { mutableStateOf(false) }
    LaunchedEffect(resetKey) {
        behavior.state.heightOffset = 0f
        behavior.state.contentOffset = 0f
    }
    CompositionLocalProvider(LocalCatalogScrollable provides contentScrolls) {
        Scaffold(
            modifier
                .testTag("catalog-screen")
                .then(
                    if (tools != null && contentScrolls.value)
                        Modifier.nestedScroll(behavior.nestedScrollConnection)
                    else Modifier,
                ),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Column(Modifier.statusBarsPadding()) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 8.dp).heightIn(min = if (home) 52.dp else 64.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onBack, Modifier.testTag("catalog-back")) {
                            Icon(
                                painterResource(ru.mobui.kanji.core.designsystem.R.drawable.ic_arrow_back),
                                stringResource(R.string.catalog_back),
                                Modifier.size(24.dp),
                            )
                        }
                        Text(
                            title,
                            Modifier.weight(1f)
                                .padding(horizontal = if (home) 0.dp else 8.dp, vertical = if (home) 2.dp else 10.dp)
                                .semantics { heading() },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = if (home) FontWeight.Normal else FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (search != null) {
                            ru.mobui.kanji.core.designsystem.KanjiSearchToggle(
                                open = search.state.open,
                                onToggle = search.toggle,
                                testTag = "catalog-toggle-search",
                                contentDescription = stringResource(
                                    if (search.state.open) R.string.catalog_close_search
                                    else R.string.catalog_open_search,
                                ),
                            )
                        }
                    }
                    if (subtitle.isNotEmpty())
                        Text(
                            subtitle,
                            Modifier.padding(start = 18.dp, end = 18.dp, bottom = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    tabs?.invoke()
                    if (search?.state?.open == true)
                        CatalogSearch(
                            search.state.query,
                            stringResource(search.label),
                            search.query,
                            numeric = search.numeric,
                        )
                    if (tools != null) EnterAlwaysPanel(behavior, tools)
                    SideEffect {
                        if (tools == null || !contentScrolls.value) {
                            if (tools == null) behavior.state.heightOffsetLimit = 0f
                            behavior.state.heightOffset = 0f
                        }
                    }
                }
            },
        ) { padding ->
            Box(
                Modifier.fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                content()
            }
        }
    }
}

// Existing, explicitly approved experimental panel mechanism; see
// doc/decisions/experimental-api.md.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnterAlwaysPanel(behavior: TopAppBarScrollBehavior, content: @Composable () -> Unit) {
    val bar = behavior.state
    Box(
        Modifier.fillMaxWidth()
            .clipToBounds()
            .background(MaterialTheme.colorScheme.background)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val limit = -placeable.height.toFloat()
                if (bar.heightOffsetLimit != limit) bar.heightOffsetLimit = limit
                val offset = bar.heightOffset.roundToInt().coerceIn(-placeable.height, 0)
                layout(placeable.width, placeable.height + offset) { placeable.place(0, offset) }
            },
    ) {
        Column(Modifier.fillMaxWidth()) { content() }
    }
}

@Composable
internal fun <T> CatalogLoaded(
    state: CatalogLoad<T>,
    retry: () -> Unit,
    content: @Composable (T) -> Unit,
) {
    when (state) {
        CatalogLoad.Loading -> CatalogStatus(false, retry)
        CatalogLoad.Error -> CatalogStatus(true, retry)
        is CatalogLoad.Ready -> content(state.content)
    }
}

@Composable
internal fun ChapterEmpty(query: String, range: IntRange?) {
    CatalogEmptyState(
        query.isNotBlank(),
        explanation =
            if (query.isNotBlank() && range != null)
                stringResource(R.string.catalog_chapter_range, range.first, range.last)
            else null,
    )
}
