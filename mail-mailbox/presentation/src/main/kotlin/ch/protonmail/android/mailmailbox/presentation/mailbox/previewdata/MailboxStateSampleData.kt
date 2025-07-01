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

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.ActionUiModel
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.sample.ActionUiModelSample
import ch.protonmail.android.mailcommon.presentation.ui.delete.DeleteDialogState
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.presentation.text
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxListState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxListState.Data.SelectionMode.SelectedMailboxItem
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxSearchState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxTopAppBarState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.StorageLimitState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.SwipeActionsUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.UnreadFilterState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.UpgradeStorageState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.BottomSheetState
import ch.protonmail.android.mailsettings.presentation.accountsettings.autodelete.AutoDeleteSettingState
import kotlinx.collections.immutable.toImmutableList

object MailboxStateSampleData {

    val Loading = MailboxState(
        mailboxListState = MailboxListState.Loading,
        topAppBarState = MailboxTopAppBarState.Loading,
        upgradeStorageState = UpgradeStorageState(notificationDotVisible = false),
        unreadFilterState = UnreadFilterState.Loading,
        bottomAppBarState = BottomBarState.Loading,
        actionResult = Effect.empty(),
        deleteDialogState = DeleteDialogState.Hidden,
        deleteAllDialogState = DeleteDialogState.Hidden,
        bottomSheetState = null,
        storageLimitState = StorageLimitState.HasEnoughSpace,
        error = Effect.empty(),
        showRatingBooster = Effect.empty(),
        autoDeleteSettingState = AutoDeleteSettingState.Loading,
        showNPSFeedback = Effect.empty()
    )

    val Inbox = MailboxState(
        mailboxListState = MailboxListState.Data.ViewMode(
            currentMailLabel = MailLabel.System(MailLabelId.System.Inbox),
            openItemEffect = Effect.empty(),
            scrollToMailboxTop = Effect.empty(),
            offlineEffect = Effect.empty(),
            refreshErrorEffect = Effect.empty(),
            refreshRequested = false,
            swipingEnabled = true,
            swipeActions = SwipeActionsUiModel(
                start = SwipeUiModelSampleData.Trash,
                end = SwipeUiModelSampleData.Archive
            ),
            searchState = MailboxSearchStateSampleData.NotSearching,
            clearState = MailboxListState.Data.ClearState.Hidden,
            autoDeleteBannerState = MailboxListState.Data.AutoDeleteBannerState.Hidden
        ),
        topAppBarState = MailboxTopAppBarState.Data.DefaultMode(
            currentLabelName = MailLabel.System(MailLabelId.System.Inbox).text()
        ),
        upgradeStorageState = UpgradeStorageState(notificationDotVisible = false),
        unreadFilterState = UnreadFilterState.Data(
            isFilterEnabled = false,
            numUnread = 1
        ),
        bottomAppBarState = BottomBarState.Data.Hidden(
            actions = listOf(ActionUiModelSample.Archive).toImmutableList()
        ),
        actionResult = Effect.empty(),
        deleteDialogState = DeleteDialogState.Hidden,
        deleteAllDialogState = DeleteDialogState.Hidden,
        bottomSheetState = null,
        storageLimitState = StorageLimitState.HasEnoughSpace,
        error = Effect.empty(),
        showRatingBooster = Effect.empty(),
        autoDeleteSettingState = AutoDeleteSettingState.Loading,
        showNPSFeedback = Effect.empty()
    )

    val AllMail = MailboxState(
        mailboxListState = MailboxListState.Data.ViewMode(
            currentMailLabel = MailLabel.System(MailLabelId.System.AllMail),
            openItemEffect = Effect.empty(),
            scrollToMailboxTop = Effect.empty(),
            offlineEffect = Effect.empty(),
            refreshErrorEffect = Effect.empty(),
            refreshRequested = false,
            swipingEnabled = true,
            swipeActions = SwipeActionsUiModel(
                start = SwipeUiModelSampleData.Trash,
                end = SwipeUiModelSampleData.Archive
            ),
            searchState = MailboxSearchStateSampleData.NotSearching,
            clearState = MailboxListState.Data.ClearState.Hidden,
            autoDeleteBannerState = MailboxListState.Data.AutoDeleteBannerState.Hidden
        ),
        topAppBarState = MailboxTopAppBarState.Data.DefaultMode(
            currentLabelName = MailLabel.System(MailLabelId.System.AllMail).text()
        ),
        upgradeStorageState = UpgradeStorageState(notificationDotVisible = false),
        unreadFilterState = UnreadFilterState.Data(
            isFilterEnabled = false,
            numUnread = 1
        ),
        bottomAppBarState = BottomBarState.Data.Hidden(
            actions = listOf(ActionUiModelSample.Archive).toImmutableList()
        ),
        actionResult = Effect.empty(),
        deleteDialogState = DeleteDialogState.Hidden,
        deleteAllDialogState = DeleteDialogState.Hidden,
        bottomSheetState = null,
        storageLimitState = StorageLimitState.HasEnoughSpace,
        error = Effect.empty(),
        showRatingBooster = Effect.empty(),
        autoDeleteSettingState = AutoDeleteSettingState.Loading,
        showNPSFeedback = Effect.empty()
    )

    fun createSelectionMode(
        selectedMailboxItemUiModels: List<MailboxItemUiModel>,
        bottomBarAction: List<ActionUiModel> = listOf(ActionUiModelSample.Archive),
        currentMailLabel: MailLabel = MailLabel.System(MailLabelId.System.Inbox),
        bottomSheetState: BottomSheetState? = null,
        error: Effect<TextUiModel> = Effect.empty()
    ) = MailboxState(
        mailboxListState = MailboxListState.Data.SelectionMode(
            currentMailLabel = currentMailLabel,
            selectedMailboxItems = selectedMailboxItemUiModels.map {
                SelectedMailboxItem(it.userId, it.id, it.isRead, it.showStar)
            }.toSet(),
            swipingEnabled = true,
            swipeActions = SwipeActionsUiModel(
                start = SwipeUiModelSampleData.Trash,
                end = SwipeUiModelSampleData.Archive
            ),
            searchState = MailboxSearchState.NotSearching,
            clearState = MailboxListState.Data.ClearState.Hidden,
            autoDeleteBannerState = MailboxListState.Data.AutoDeleteBannerState.Hidden
        ),
        topAppBarState = MailboxTopAppBarState.Data.SelectionMode(
            currentLabelName = MailLabel.System(MailLabelId.System.Inbox).text(),
            selectedCount = selectedMailboxItemUiModels.size
        ),
        upgradeStorageState = UpgradeStorageState(notificationDotVisible = false),
        unreadFilterState = UnreadFilterState.Data(
            isFilterEnabled = false,
            numUnread = 1
        ),
        bottomAppBarState = BottomBarState.Data.Hidden(
            actions = bottomBarAction.toImmutableList()
        ),
        actionResult = Effect.empty(),
        deleteDialogState = DeleteDialogState.Hidden,
        deleteAllDialogState = DeleteDialogState.Hidden,
        bottomSheetState = bottomSheetState,
        storageLimitState = StorageLimitState.HasEnoughSpace,
        error = error,
        showRatingBooster = Effect.empty(),
        autoDeleteSettingState = AutoDeleteSettingState.Loading,
        showNPSFeedback = Effect.empty()
    )
}
