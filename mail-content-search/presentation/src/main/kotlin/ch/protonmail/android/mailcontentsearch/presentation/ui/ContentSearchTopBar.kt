/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailcontentsearch.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.delete
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import ch.protonmail.android.design.compose.component.appbar.ProtonTopAppBar
import ch.protonmail.android.design.compose.modifier.CollapsingTopBarState
import ch.protonmail.android.design.compose.modifier.collapsingContainer
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.theme.bodyLargeNorm
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcontentsearch.presentation.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.ShowSpamTrashIncludeFilter
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.ShowSpamTrashIncludeFilterState
import ch.protonmail.android.mailpagination.domain.model.IncludeFilter
import ch.protonmail.android.mailcommon.presentation.R as CommonR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ContentSearchTopBar(
    queryState: TextFieldState,
    includeFilter: IncludeFilter,
    isSearchLive: Boolean,
    actions: ContentSearchActions,
    inSelectionMode: Boolean,
    collapsingState: CollapsingTopBarState,
    modifier: Modifier = Modifier,
    focusRequests: SearchFieldFocusRequests = remember { SearchFieldFocusRequests() }
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    // A request that arrives before the field has been placed has to wait: requesting focus on a
    // node that does not exist yet throws.
    var isFieldPlaced by remember { mutableStateOf(false) }
    var isFocusPending by remember { mutableStateOf(false) }

    val takeFocus = {
        isFocusPending = false
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    LaunchedEffect(focusRequests.count) {
        if (focusRequests.count == 0) return@LaunchedEffect
        if (isFieldPlaced) takeFocus() else isFocusPending = true
    }

    Column(
        modifier = modifier
            .background(ProtonTheme.colors.backgroundSecondary)
            // The chip row stays put while the field collapses, so the inset moves here whenever the
            // row is shown. Without it the chips would end up under the status bar.
            .then(
                if (isSearchLive) {
                    Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                } else {
                    Modifier
                }
            )
            .testTag(ContentSearchTopBarTestTags.RootItem)
    ) {
        // Only the field and the back button collapse. The chip row below them is pinned.
        Column(modifier = Modifier.collapsingContainer(collapsingState)) {
            ProtonTopAppBar(
                windowInsets = if (isSearchLive) {
                    WindowInsets(0, 0, 0, 0)
                } else {
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
                },
                backgroundColor = ProtonTheme.colors.backgroundSecondary,
                navigationIcon = {
                    IconButton(
                        onClick = {
                            keyboardController?.hide()
                            if (inSelectionMode) actions.onExitSelectionMode() else actions.onClose()
                        }
                    ) {
                        Icon(
                            modifier = Modifier.size(ProtonDimens.IconSize.Default),
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_search_close_content_description),
                            tint = ProtonTheme.colors.iconNorm
                        )
                    }
                },
                title = {
                    SearchBarDefaults.InputField(
                        state = queryState,
                        inputTransformation = QueryLengthLimit,
                        onSearch = {
                            keyboardController?.hide()
                            actions.onSearch()
                        },
                        // Expansion belongs to the M3 SearchBar this field is intentionally used without.
                        expanded = false,
                        onExpandedChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onGloballyPositioned {
                                isFieldPlaced = true
                                if (isFocusPending) takeFocus()
                            }
                            .testTag(ContentSearchTopBarTestTags.SearchTextField),
                        textStyle = ProtonTheme.typography.bodyLargeNorm,
                        placeholder = {
                            Text(
                                text = stringResource(R.string.content_search_placeholder_text),
                                color = ProtonTheme.colors.textHint,
                                style = ProtonTheme.typography.bodyLargeNorm
                            )
                        },
                        trailingIcon = {
                            AnimatedVisibility(
                                visible = queryState.text.isNotEmpty(),
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                IconButton(
                                    modifier = Modifier.testTag(ContentSearchTopBarTestTags.ClearQueryButton),
                                    onClick = {
                                        actions.onClearQuery()
                                        // Clearing puts the user back at the start of a search rather than
                                        // done with one, so hand the caret and the keyboard back — the
                                        // keyboard is gone by now if they dismissed it to browse results.
                                        focusRequester.requestFocus()
                                        keyboardController?.show()
                                    }
                                ) {
                                    Icon(
                                        modifier = Modifier.size(ProtonDimens.IconSize.Medium),
                                        painter = painterResource(id = CommonR.drawable.ic_material_close),
                                        contentDescription = stringResource(
                                            R.string.content_search_clear_query_content_description
                                        ),
                                        tint = ProtonTheme.colors.iconNorm
                                    )
                                }
                            }
                        },
                        shape = RectangleShape,
                        colors = SearchBarDefaults.inputFieldColors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = ProtonTheme.colors.textNorm,
                            unfocusedTextColor = ProtonTheme.colors.textNorm,
                            cursorColor = ProtonTheme.colors.textNorm
                        )
                    )
                },
                actions = {}
            )
        }
        // The chip scopes the live search, so it stays up through the loading of a resubmitted
        // query instead of flickering out and back with every new spin-up, on the same surface as
        // the field above.
        if (isSearchLive) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ProtonDimens.Spacing.Large)
                    .padding(bottom = ProtonDimens.Spacing.Medium)
            ) {
                ShowSpamTrashIncludeFilter(
                    modifier = Modifier.height(MailDimens.UnreadFilterChipHeight),
                    state = ShowSpamTrashIncludeFilterState.Data.Shown(enabled = includeFilter.includesAny),
                    onFilterEnabled = actions.onToggleIncludeSpamTrash,
                    onFilterDisabled = actions.onToggleIncludeSpamTrash
                )
            }
        }
    }
}

// No real search term gets close to this. A longer paste is cut down to fit rather than rejected,
// which is what the built-in maxLength would do. The cut steps back one character rather than
// split an emoji in half.
private const val MAX_QUERY_LENGTH = 255
private val QueryLengthLimit = InputTransformation {
    if (length > MAX_QUERY_LENGTH) {
        val end = if (charAt(MAX_QUERY_LENGTH - 1).isHighSurrogate()) MAX_QUERY_LENGTH - 1 else MAX_QUERY_LENGTH
        delete(end, length)
    }
}

/**
 * Asks the search field for focus, from outside the top bar that owns it.
 *
 * The screen decides when the field should take the caret and raise the keyboard, but the
 * [FocusRequester] belongs with the field. Each [request] is one hand-over.
 */
@Stable
internal class SearchFieldFocusRequests {

    var count by mutableIntStateOf(0)
        private set

    fun request() {
        count++
    }
}

object ContentSearchTopBarTestTags {

    const val RootItem = "ContentSearchTopBar"
    const val SearchTextField = "ContentSearchTextField"
    const val ClearQueryButton = "ContentSearchClearQueryButton"
}
