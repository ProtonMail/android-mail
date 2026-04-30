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

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.mailcategory.presentation.CategoryViewMenu
import ch.protonmail.android.mailcategory.presentation.model.CategoryItemUiModel
import ch.protonmail.android.mailcategory.presentation.model.CategoryViewState
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxListState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxTopAppBarState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.ShowSpamTrashIncludeFilterState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.UnreadFilterState

@Composable
fun MailboxStickyHeader(
    modifier: Modifier = Modifier,
    state: MailboxState,
    actions: MailboxStickyHeader.Actions,
    isCategoryViewEnabled: Boolean
) {

    val isCategoryViewVisible =
        isCategoryViewEnabled &&
            state.categoryViewState is CategoryViewState.Available &&
            state.topAppBarState !is MailboxTopAppBarState.Data.SearchMode &&
            state.mailboxListState !is MailboxListState.Data.SelectionMode

    val horizontalPadding = if (isCategoryViewVisible) {
        0.dp
    } else {
        ProtonDimens.Spacing.Large
    }

    val bottomPadding = if (isCategoryViewVisible) {
        0.dp
    } else {
        ProtonDimens.Spacing.Small
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = horizontalPadding,
                end = horizontalPadding,
                bottom = bottomPadding,
                top = 0.dp
            )
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.Start
    ) {
        if (state.mailboxListState is MailboxListState.Data.SelectionMode) {
            SelectDeselectAllButton(
                modifier = Modifier.height(MailDimens.UnreadFilterChipHeight),
                areAllItemsSelected = state.mailboxListState.areAllItemsSelected,
                actions = SelectDeselectAllButton.Actions(
                    onSelectAllClicked = actions.onSelectAllClicked,
                    onDeselectAllClicked = actions.onDeselectAllClicked
                )
            )

            Spacer(modifier = Modifier.width(ProtonDimens.Spacing.Standard))

            ShowSpamTrashIncludeFilter(
                modifier = Modifier.height(MailDimens.UnreadFilterChipHeight),
                state = state.showSpamTrashIncludeFilterState,
                onFilterEnabled = actions.onSpamTrashFilterEnabled,
                onFilterDisabled = actions.onSpamTrashFilterDisabled
            )
        } else {
            Row {
                if (state.topAppBarState is MailboxTopAppBarState.Data.SearchMode) {
                    SearchModeStickyHeader(
                        searchState = state.topAppBarState,
                        showSpamTrashFilterState = state.showSpamTrashIncludeFilterState,
                        onEnabled = actions.onSpamTrashFilterEnabled,
                        onDisabled = actions.onSpamTrashFilterDisabled
                    )
                } else {
                    if (isCategoryViewEnabled && state.categoryViewState is CategoryViewState.Available) {
                        StickyHeaderWithCategoryView(
                            categoryViewState = state.categoryViewState,
                            onCategoryItemClicked = actions.onCategoryItemClicked
                        )
                    } else {
                        DefaultModeStickyHeader(
                            isCategoryViewEnabled = isCategoryViewEnabled,
                            unreadFilterState = state.unreadFilterState,
                            spamTrashFilterState = state.showSpamTrashIncludeFilterState,
                            onReadEnabled = actions.onUnreadFilterEnabled,
                            onReadDisabled = actions.onUnreadFilterDisabled,
                            onSpamTrashEnabled = actions.onSpamTrashFilterEnabled,
                            onSpamTrashDisabled = actions.onSpamTrashFilterDisabled
                        )
                    }
                }
            }
        }
    }
}

@Suppress("UnusedReceiverParameter")
@Composable
private fun RowScope.SearchModeStickyHeader(
    searchState: MailboxTopAppBarState.Data.SearchMode,
    showSpamTrashFilterState: ShowSpamTrashIncludeFilterState,
    onEnabled: () -> Unit,
    onDisabled: () -> Unit
) {
    if (searchState.searchQuery.isNotEmpty()) {
        ShowSpamTrashIncludeFilter(
            modifier = Modifier.height(MailDimens.UnreadFilterChipHeight),
            state = showSpamTrashFilterState,
            onFilterEnabled = onEnabled,
            onFilterDisabled = onDisabled
        )
    }
}

@Suppress("UnusedReceiverParameter", "UseComposableActions")
@Composable
private fun RowScope.DefaultModeStickyHeader(
    isCategoryViewEnabled: Boolean,
    unreadFilterState: UnreadFilterState,
    spamTrashFilterState: ShowSpamTrashIncludeFilterState,
    onReadEnabled: () -> Unit,
    onReadDisabled: () -> Unit,
    onSpamTrashEnabled: () -> Unit,
    onSpamTrashDisabled: () -> Unit
) {
    if (!isCategoryViewEnabled) {
        UnreadItemsFilter(
            modifier = Modifier.height(MailDimens.UnreadFilterChipHeight),
            state = unreadFilterState,
            onFilterEnabled = onReadEnabled,
            onFilterDisabled = onReadDisabled
        )

        Spacer(modifier = Modifier.width(ProtonDimens.Spacing.Standard))
    }

    ShowSpamTrashIncludeFilter(
        modifier = Modifier.height(MailDimens.UnreadFilterChipHeight),
        state = spamTrashFilterState,
        onFilterEnabled = onSpamTrashEnabled,
        onFilterDisabled = onSpamTrashDisabled
    )
}

@Suppress("UnusedReceiverParameter")
@Composable
private fun RowScope.StickyHeaderWithCategoryView(
    categoryViewState: CategoryViewState.Available,
    onCategoryItemClicked: (CategoryItemUiModel) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        when (categoryViewState) {
            CategoryViewState.Available.Loading -> {
                CategoryViewMenu(
                    items = emptyList(),
                    onItemClick = onCategoryItemClicked
                )
            }

            is CategoryViewState.Available.Data -> {
                CategoryViewMenu(
                    items = categoryViewState.categories,
                    onItemClick = onCategoryItemClicked
                )
            }
        }
    }
}

object MailboxStickyHeader {
    data class Actions(
        val onUnreadFilterEnabled: () -> Unit,
        val onUnreadFilterDisabled: () -> Unit,
        val onSpamTrashFilterEnabled: () -> Unit,
        val onSpamTrashFilterDisabled: () -> Unit,
        val onSelectAllClicked: () -> Unit,
        val onDeselectAllClicked: () -> Unit,
        val onCategoryItemClicked: (CategoryItemUiModel) -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onUnreadFilterEnabled = {},
                onUnreadFilterDisabled = {},
                onSpamTrashFilterEnabled = {},
                onSpamTrashFilterDisabled = {},
                onSelectAllClicked = {},
                onDeselectAllClicked = {},
                onCategoryItemClicked = {}
            )
        }
    }
}
