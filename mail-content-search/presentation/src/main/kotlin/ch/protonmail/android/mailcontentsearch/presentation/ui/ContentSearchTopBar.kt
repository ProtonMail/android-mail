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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.theme.bodyLargeNorm
import ch.protonmail.android.mailcontentsearch.presentation.R
import ch.protonmail.android.mailpagination.domain.model.IncludeFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ContentSearchTopBar(
    queryState: TextFieldState,
    includeFilter: IncludeFilter,
    actions: ContentSearchActions,
    inSelectionMode: Boolean,
    modifier: Modifier = Modifier,
    consumeAutoFocus: () -> Boolean = { false }
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    // Consuming is a side effect (it clears a one-shot flag elsewhere), so it belongs in an effect,
    // not in the remember calculation — composition can run speculatively or be abandoned, which
    // would burn the flag without ever requesting focus.
    var autoFocus by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        autoFocus = consumeAutoFocus()
    }
    // Requested once the field has been placed rather than from an effect, which can run before the
    // focus node exists.
    var hasRequestedFocus by remember { mutableStateOf(false) }

    ProtonTopAppBar(
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top),
        backgroundColor = ProtonTheme.colors.backgroundSecondary,
        modifier = modifier.testTag(ContentSearchTopBarTestTags.RootItem),
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
                // The search runs as the user types, so submitting has nothing left to do but get the
                // keyboard out of the way of the results.
                onSearch = { keyboardController?.hide() },
                // Expansion belongs to the M3 SearchBar this field is intentionally used without.
                expanded = false,
                onExpandedChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onGloballyPositioned {
                        if (autoFocus && !hasRequestedFocus) {
                            hasRequestedFocus = true
                            focusRequester.requestFocus()
                        }
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
                                painter = painterResource(id = R.drawable.ic_material_close),
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
        actions = {
            SearchOptionsMenu(
                includeFilter = includeFilter,
                onToggleIncludeSpam = actions.onToggleIncludeSpam,
                onToggleIncludeTrash = actions.onToggleIncludeTrash
            )
        }
    )
}

object ContentSearchTopBarTestTags {

    const val RootItem = "ContentSearchTopBar"
    const val SearchTextField = "ContentSearchTextField"
    const val ClearQueryButton = "ContentSearchClearQueryButton"
}
