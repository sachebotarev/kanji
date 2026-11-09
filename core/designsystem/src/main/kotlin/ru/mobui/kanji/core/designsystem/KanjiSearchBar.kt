package ru.mobui.kanji.core.designsystem

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Standard search field based on Catalog UX standards.
 * Supports auto-focus, optional leading icon, clear button, and input shape compliance.
 */
@Composable
fun KanjiSearchBar(
    query: String,
    onQuery: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = stringResource(R.string.core_search),
    autoFocus: Boolean = false,
    leadingIcon: Boolean = true,
    testTag: String = "search-input",
    numeric: Boolean = false,
    onClear: (() -> Unit)? = null,
) {
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val focusRequested = rememberSaveable { mutableStateOf(false) }

    if (autoFocus) {
        LaunchedEffect(Unit) {
            if (!focusRequested.value) {
                focusRequested.value = true
                focus.requestFocus()
                keyboard?.show()
            }
        }
    }

    OutlinedTextField(
        value = query,
        onValueChange = onQuery,
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
            .then(if (autoFocus) Modifier.focusRequester(focus) else Modifier),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium,
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = if (leadingIcon) {
            {
                Icon(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else null,
        trailingIcon = {
            if (query.isNotEmpty()) {
                val clearAction = onClear ?: { onQuery("") }
                IconButton(onClick = clearAction, modifier = Modifier.testTag("search-clear")) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.core_clear),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text,
            imeAction = ImeAction.Search,
        ),
        keyboardActions = KeyboardActions(
            onSearch = {
                focusManager.clearFocus()
                keyboard?.hide()
            },
        ),
        shape = MaterialTheme.inputShape,
        colors = kanjiOutlinedTextFieldColors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = MaterialTheme.colorScheme.outline,
        ),
    )
}
