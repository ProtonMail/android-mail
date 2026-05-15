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

package ch.protonmail.android.mailmailbox.presentation.mailbox.previewdata

import ch.protonmail.android.mailcategory.presentation.model.CategoryViewState
import ch.protonmail.android.mailcategory.presentation.sample.CategoryItemUiModelSample
import ch.protonmail.android.mailcommon.presentation.model.CappedNumberUiModel
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.ActionUiModel
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.model.BottomBarTarget
import ch.protonmail.android.mailcommon.presentation.model.BottomSheetState
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.sample.ActionUiModelSample
import ch.protonmail.android.mailcommon.presentation.ui.delete.DeleteDialogState
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.presentation.text
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.LoadingBarUiState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxComposerNavigationState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxListState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxListState.Data.SelectionMode.SelectedMailboxItem
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxSearchState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxTopAppBarState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.ShowSpamTrashIncludeFilterState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.SwipeActionsUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.UnreadFilterState
import ch.protonmail.android.mailmessage.presentation.model.AvatarImagesUiModel
import kotlinx.collections.immutable.toImmutableList

object MailboxStateSampleData {

    private val inboxDynamicLabel = MailLabel.System(
        MailLabelId.System(LabelId("inbox")),
        SystemLabelId.Inbox,
        0
    )

    private val allMailDynamicLabel = MailLabel.System(
        MailLabelId.System(LabelId("allmail")),
        SystemLabelId.AllMail,
        0
    )

    private val trashDynamicLabel = MailLabel.System(
        MailLabelId.System(LabelId("trash")),
        SystemLabelId.Trash,
        0
    )

    val Loading = MailboxState(
        mailboxListState = MailboxListState.Loading,
        topAppBarState = MailboxTopAppBarState.Loading,
        unreadFilterState = UnreadFilterState.Loading,
        categoryViewState = CategoryViewState.Available.Loading,
        showSpamTrashIncludeFilterState = ShowSpamTrashIncludeFilterState.Loading,
        bottomAppBarState = BottomBarState.Loading,
        actionResult = Effect.empty(),
        deleteDialogState = DeleteDialogState.Hidden,
        clearAllDialogState = DeleteDialogState.Hidden,
        bottomSheetState = null,
        composerNavigationState = MailboxComposerNavigationState.Enabled(),
        error = Effect.empty(),
        showRatingBooster = Effect.empty()
    )

    val Inbox = MailboxState(
        mailboxListState = MailboxListState.Data.ViewMode(
            currentMailLabel = inboxDynamicLabel,
            openItemEffect = Effect.empty(),
            scrollToMailboxTop = Effect.empty(),
            refreshErrorEffect = Effect.empty(),
            refreshOngoing = false,
            loadingBarState = LoadingBarUiState.Hide,
            swipeActions = SwipeActionsUiModel(
                start = SwipeUiModelSampleData.Trash,
                end = SwipeUiModelSampleData.Archive
            ),
            searchState = MailboxSearchStateSampleData.NotSearching,
            shouldShowFab = true,
            avatarImagesUiModel = AvatarImagesUiModel.Empty
        ),
        topAppBarState = MailboxTopAppBarState.Data.DefaultMode(
            currentLabelName = inboxDynamicLabel.text(),
            primaryAvatarItem = null
        ),
        unreadFilterState = UnreadFilterState.Data(
            isFilterEnabled = false,
            unreadCount = CappedNumberUiModel.Exact(1),
            activeCategoryColor = CategoryItemUiModelSample.primary.activeColor
        ),
        categoryViewState = CategoryViewState.Available.Data(
            categories = CategoryItemUiModelSample.all
        ),
        showSpamTrashIncludeFilterState = ShowSpamTrashIncludeFilterState.Data.Shown(
            enabled = false
        ),
        bottomAppBarState = BottomBarState.Data.Hidden(
            target = BottomBarTarget.Mailbox,
            actions = listOf(ActionUiModelSample.Archive).toImmutableList()
        ),
        actionResult = Effect.empty(),
        deleteDialogState = DeleteDialogState.Hidden,
        clearAllDialogState = DeleteDialogState.Hidden,
        bottomSheetState = null,
        composerNavigationState = MailboxComposerNavigationState.Enabled(),
        error = Effect.empty(),
        showRatingBooster = Effect.empty()
    )

    val AllMail = MailboxState(
        mailboxListState = MailboxListState.Data.ViewMode(
            currentMailLabel = allMailDynamicLabel,
            openItemEffect = Effect.empty(),
            scrollToMailboxTop = Effect.empty(),
            refreshErrorEffect = Effect.empty(),
            refreshOngoing = false,
            loadingBarState = LoadingBarUiState.Hide,
            swipeActions = SwipeActionsUiModel(
                start = SwipeUiModelSampleData.Trash,
                end = SwipeUiModelSampleData.Archive
            ),
            searchState = MailboxSearchStateSampleData.NotSearching,
            shouldShowFab = true,
            avatarImagesUiModel = AvatarImagesUiModel.Empty
        ),
        topAppBarState = MailboxTopAppBarState.Data.DefaultMode(
            currentLabelName = allMailDynamicLabel.text(),
            primaryAvatarItem = null
        ),
        unreadFilterState = UnreadFilterState.Data(
            isFilterEnabled = false,
            unreadCount = CappedNumberUiModel.Exact(1)
        ),
        categoryViewState = CategoryViewState.NotAvailable,
        showSpamTrashIncludeFilterState = ShowSpamTrashIncludeFilterState.Data.Shown(
            enabled = false
        ),
        bottomAppBarState = BottomBarState.Data.Hidden(
            target = BottomBarTarget.Mailbox,
            actions = listOf(ActionUiModelSample.Archive).toImmutableList()
        ),
        actionResult = Effect.empty(),
        deleteDialogState = DeleteDialogState.Hidden,
        clearAllDialogState = DeleteDialogState.Hidden,
        bottomSheetState = null,
        composerNavigationState = MailboxComposerNavigationState.Enabled(),
        error = Effect.empty(),
        showRatingBooster = Effect.empty()
    )

    val Trash = Inbox.copy(
        mailboxListState = (Inbox.mailboxListState as MailboxListState.Data.ViewMode).copy(
            currentMailLabel = trashDynamicLabel
        )
    )

    fun createSelectionMode(
        selectedMailboxItemUiModels: List<MailboxItemUiModel>,
        bottomBarAction: List<ActionUiModel> = listOf(ActionUiModelSample.Archive),
        currentMailLabel: MailLabel = inboxDynamicLabel,
        bottomSheetState: BottomSheetState? = null,
        error: Effect<TextUiModel> = Effect.empty(),
        showRatingBooster: Effect<Unit> = Effect.empty()
    ) = MailboxState(
        mailboxListState = MailboxListState.Data.SelectionMode(
            currentMailLabel = currentMailLabel,
            selectedMailboxItems = selectedMailboxItemUiModels.map {
                SelectedMailboxItem(it.id, it.isRead, it.isStarred)
            }.toSet(),
            swipeActions = SwipeActionsUiModel(
                start = SwipeUiModelSampleData.Trash,
                end = SwipeUiModelSampleData.Archive
            ),
            searchState = MailboxSearchState.NotSearching,
            shouldShowFab = false,
            avatarImagesUiModel = AvatarImagesUiModel.Empty,
            areAllItemsSelected = false,
            refreshOngoing = false,
            loadingBarState = LoadingBarUiState.Hide
        ),
        topAppBarState = MailboxTopAppBarState.Data.SelectionMode(
            currentLabelName = inboxDynamicLabel.text(),
            selectedCount = selectedMailboxItemUiModels.size,
            primaryAvatarItem = null
        ),
        unreadFilterState = UnreadFilterState.Data(
            isFilterEnabled = false,
            unreadCount = CappedNumberUiModel.Exact(1)
        ),
        categoryViewState = CategoryViewState.NotAvailable,
        showSpamTrashIncludeFilterState = ShowSpamTrashIncludeFilterState.Data.Hidden,
        bottomAppBarState = BottomBarState.Data.Hidden(
            target = BottomBarTarget.Mailbox,
            actions = bottomBarAction.toImmutableList()
        ),
        actionResult = Effect.empty(),
        deleteDialogState = DeleteDialogState.Hidden,
        clearAllDialogState = DeleteDialogState.Hidden,
        bottomSheetState = bottomSheetState,
        composerNavigationState = MailboxComposerNavigationState.Enabled(),
        error = error,
        showRatingBooster = showRatingBooster
    )
}
