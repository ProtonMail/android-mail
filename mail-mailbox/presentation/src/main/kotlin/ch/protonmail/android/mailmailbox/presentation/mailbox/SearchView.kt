/*
 * Copyright (c) 2022 Proton Technologies AG
 * This file is part of Proton Technologies AG and Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailmailbox.presentation.mailbox

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.mailcommon.presentation.compose.keyboardVisibilityAsState
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.uicomponents.text.defaultTextFieldColors
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultNorm

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun SearchView(
    initialValue: String,
    actions: SearchView.Actions,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val isKeyboardVisible by keyboardVisibilityAsState()
    var isFocused by remember { mutableStateOf(false) }
    var searchText by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initialValue))
    }

    Row(modifier = modifier) {

        TextField(
            value = searchText,
            onValueChange = {
                searchText = it
            },
            modifier = modifier
                .testTag(SearchViewTestTags.SearchTextField)
                .weight(1f)
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused && !isFocused) {
                        isFocused = true
                        keyboardController?.show()
                    }
                }
                .onGloballyPositioned { _ ->
                    if (!isFocused) {
                        focusRequester.requestFocus()
                    }
                },
            textStyle = ProtonTheme.typography.defaultNorm,
            colors = TextFieldDefaults.defaultTextFieldColors(),
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    actions.onSearchQuerySubmit(searchText.text.trim())
                    keyboardController?.hide()
                }
            ),
            placeholder = {
                Text(
                    modifier = Modifier.testTag(SearchViewTestTags.SearchTextFieldPlaceholder),
                    text = stringResource(R.string.mailbox_search_placeholder_text),
                    color = ProtonTheme.colors.textHint,
                    style = ProtonTheme.typography.defaultNorm
                )
            }
        )

        AnimatedVisibility(
            modifier = Modifier
                .align(CenterVertically)
                .animateContentSize(),
            visible = searchText.text.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            IconButton(
                modifier = Modifier
                    .testTag(SearchViewTestTags.SearchViewClearQueryButton),
                onClick = {
                    searchText = TextFieldValue()
                    actions.onClearSearchQuery()
                    if (!isFocused) {
                        focusRequester.requestFocus()
                    }

                    if (!isKeyboardVisible) {
                        keyboardController?.show()
                    }
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_proton_close),
                    contentDescription = stringResource(
                        id = R.string.mailbox_toolbar_searchview_clear_search_query_content_description
                    ),
                    tint = ProtonTheme.colors.textNorm
                )
            }
        }
    }
}

@Preview
@Composable
internal fun EmptySearchTextFieldPreview() {
    ProtonTheme {
        SearchView(
            modifier = Modifier.fillMaxWidth(),
            initialValue = "",
            actions = SearchView.Actions.Empty
        )
    }
}

@Preview
@Composable
internal fun FilledSearchTextFieldPreview() {
    ProtonTheme {
        SearchView(
            modifier = Modifier.fillMaxWidth(),
            initialValue = "searchtext",
            actions = SearchView.Actions.Empty
        )
    }
}

object SearchView {

    data class Actions(
        val onClearSearchQuery: () -> Unit,
        val onSearchQuerySubmit: (query: String) -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onClearSearchQuery = {},
                onSearchQuerySubmit = {}
            )
        }
    }
}

object SearchViewTestTags {
    const val SearchTextField = "SearchTextField"
    const val SearchTextFieldPlaceholder = "SearchTextPlaceholder"
    const val SearchViewClearQueryButton = "SearchViewClearQueryButton"
}

