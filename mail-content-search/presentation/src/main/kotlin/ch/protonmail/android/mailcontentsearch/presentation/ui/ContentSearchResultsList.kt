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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.theme.bodySmallWeak
import ch.protonmail.android.design.compose.theme.titleSmallNorm
import ch.protonmail.android.mailattachments.presentation.model.AttachmentIdUiModel
import ch.protonmail.android.mailcommon.presentation.model.AvatarImageUiModel
import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import ch.protonmail.android.mailcontentsearch.presentation.R
import ch.protonmail.android.mailcontentsearch.presentation.model.ContentSearchResultUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.ComposeMailboxItem
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxItem
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.mailmessage.presentation.model.AvatarImagesUiModel
import ch.protonmail.android.uicomponents.dismissKeyboard
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun ResultsList(
    items: LazyPagingItems<ContentSearchResultUiModel>,
    actions: ContentSearchActions,
    query: String,
    selectionMode: Boolean,
    selectedItemIds: Set<String>,
    avatarImages: AvatarImagesUiModel,
    downloadingAttachmentId: AttachmentIdUiModel?,
    showMinQueryLengthNotice: Boolean,
    totalCount: Int?,
    listState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier
) {
    // Only opening an item needs the result wrapper (it decides which location to open from); everything
    // else acts on the mailbox row, as it does in the mailbox itself.
    fun itemActionsFor(result: ContentSearchResultUiModel) = ComposeMailboxItem.Actions.Empty.copy(
        onItemClicked = { actions.onItemClicked(result) },
        onStarClicked = actions.onStarClicked,
        onItemLongClicked = actions.onItemLongClicked,
        onAttachmentClicked = actions.onAttachmentClicked,
        onAvatarClicked = actions.onItemLongClicked,
        onAvatarImageLoadRequested = actions.onAvatarImageLoadRequested,
        onAvatarImageLoadFailed = actions.onAvatarImageLoadFailed
    )
    // When the keyboard is open the IME inset (which already includes the nav bar) is the
    // larger one — pad by whichever is bigger so the last results stay reachable.
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val imeInset = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val bottomInset = maxOf(navInset, imeInset)
    // Only the action toolbar floats over the list now, and only while selecting — outside selection
    // the list just needs a trailing gap, not room for a bar that isn't there.
    val bottomClearance = if (selectionMode) SelectionToolbarClearance else ProtonDimens.Spacing.Large

    // Search runs as the user types, so the keyboard is only in the way once they start browsing
    // results — dismiss it as soon as the list scrolls.
    val context = LocalContext.current
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) dismissKeyboard(context, view, keyboardController)
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Small),
        contentPadding = PaddingValues(bottom = bottomInset + bottomClearance)
    ) {
        item {
            ResultsListHeader(
                showMinQueryLengthNotice = showMinQueryLengthNotice,
                totalCount = totalCount
            )
        }

        items(
            count = items.itemCount,
            key = { index -> items.peek(index)?.id ?: "placeholder-$index" }
        ) { index ->
            val result = items[index]
            if (result != null) {
                val item = result.item
                MailboxItem(
                    modifier = Modifier.animateItem(),
                    item = item,
                    actions = itemActionsFor(result),
                    avatarImageUiModel = avatarImages.stateFor(item),
                    accessibilitySwipeActions = persistentListOf(),
                    highlightText = result.highlightText(query),
                    isSelectable = true,
                    selectionMode = selectionMode,
                    isSelected = item.id in selectedItemIds,
                    downloadingAttachmentId = downloadingAttachmentId
                )
            }
        }
    }
}

@Composable
private fun ResultsListHeader(
    showMinQueryLengthNotice: Boolean,
    totalCount: Int?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(
            horizontal = ProtonDimens.Spacing.Large,
            vertical = ProtonDimens.Spacing.Large
        )
    ) {
        Text(
            text = if (totalCount == null) {
                stringResource(R.string.content_search_results_title)
            } else {
                pluralStringResource(
                    R.plurals.content_search_results_title_with_count,
                    totalCount,
                    totalCount
                )
            },
            style = ProtonTheme.typography.titleSmallNorm
        )

        if (showMinQueryLengthNotice) {
            Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Standard))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_info_circle),
                    contentDescription = null,
                    tint = ProtonTheme.colors.iconWeak,
                    modifier = Modifier.size(ProtonDimens.IconSize.Small)
                )
                Spacer(modifier = Modifier.width(ProtonDimens.Spacing.Standard))
                Text(
                    text = stringResource(R.string.content_search_min_query_length_notice),
                    style = ProtonTheme.typography.bodySmallWeak
                )
            }
        }
    }
}

/**
 * Sender images are keyed by address, and only participant avatars have one — group and draft avatars
 * fall back to their initials.
 */
internal fun AvatarImagesUiModel.stateFor(item: MailboxItemUiModel): AvatarImageUiModel =
    (item.avatar as? AvatarUiModel.ParticipantAvatar)
        ?.let { getStateForAddress(it.address) }
        ?: AvatarImageUiModel.NotLoaded

// The floating toolbar's height plus its resting bottom padding and a gap above it.
private val SelectionToolbarClearance = 96.dp
