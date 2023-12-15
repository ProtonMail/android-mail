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

package ch.protonmail.android.mailmailbox.presentation.mailbox.model

import ch.protonmail.android.mailcommon.presentation.model.BottomBarEvent
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation.AffectingActionMessage
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation.AffectingBottomAppBar
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation.AffectingBottomSheet
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation.AffectingClearDialog
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation.AffectingDeleteDialog
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation.AffectingErrorBar
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation.AffectingMailboxList
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation.AffectingTopAppBar
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation.AffectingUnreadFilter
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.BottomSheetOperation
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.mailsettings.domain.entity.ViewMode

internal sealed interface MailboxOperation {
    sealed interface AffectingTopAppBar
    sealed interface AffectingUnreadFilter
    sealed interface AffectingMailboxList
    sealed interface AffectingBottomAppBar
    sealed interface AffectingOnboarding
    sealed interface AffectingActionMessage
    sealed interface AffectingDeleteDialog
    sealed interface AffectingClearDialog
    sealed interface AffectingBottomSheet
    sealed interface AffectingErrorBar
}

internal sealed interface MailboxViewAction : MailboxOperation {
    data class OnItemLongClicked(
        val item: MailboxItemUiModel
    ) : MailboxViewAction

    data class OnItemAvatarClicked(
        val item: MailboxItemUiModel
    ) : MailboxViewAction

    data class MailboxItemsChanged(
        val itemIds: List<String>
    ) : MailboxViewAction

    object EnterSearchMode : MailboxViewAction, AffectingTopAppBar, AffectingMailboxList
    data class SearchQuery(val query: String) : MailboxViewAction, AffectingMailboxList
    object SearchResult : MailboxViewAction, AffectingMailboxList
    object ExitSearchMode : MailboxViewAction, AffectingTopAppBar, AffectingMailboxList

    object ExitSelectionMode : MailboxViewAction, AffectingTopAppBar, AffectingMailboxList, AffectingBottomAppBar

    data class ItemClicked(val item: MailboxItemUiModel) : MailboxViewAction

    object Refresh : MailboxViewAction, AffectingMailboxList
    object RefreshCompleted : MailboxViewAction, AffectingMailboxList
    object EnableUnreadFilter : MailboxViewAction, AffectingUnreadFilter
    object DisableUnreadFilter : MailboxViewAction, AffectingUnreadFilter

    object MarkAsRead : MailboxViewAction, AffectingMailboxList
    object MarkAsUnread : MailboxViewAction, AffectingMailboxList
    object Trash : MailboxViewAction
    object Delete : MailboxViewAction
    object DeleteConfirmed : MailboxViewAction
    object DeleteDialogDismissed : MailboxViewAction, AffectingDeleteDialog
    object RequestLabelAsBottomSheet : MailboxViewAction, AffectingBottomSheet
    data class LabelAsToggleAction(val label: LabelId) : MailboxViewAction, AffectingBottomSheet
    data class LabelAsConfirmed(val archiveSelected: Boolean) : MailboxViewAction, AffectingBottomSheet

    data class SwipeReadAction(val userId: UserId, val itemId: String, val isRead: Boolean) : MailboxViewAction
    data class SwipeArchiveAction(val userId: UserId, val itemId: String) : MailboxViewAction, AffectingActionMessage
    data class SwipeSpamAction(val userId: UserId, val itemId: String) : MailboxViewAction, AffectingActionMessage
    data class SwipeTrashAction(val userId: UserId, val itemId: String) : MailboxViewAction, AffectingActionMessage
    data class SwipeStarAction(val userId: UserId, val itemId: String, val isStarred: Boolean) : MailboxViewAction

    object RequestMoveToBottomSheet : MailboxViewAction, AffectingBottomSheet
    data class MoveToDestinationSelected(
        val mailLabelId: MailLabelId
    ) : MailboxViewAction, AffectingBottomSheet

    object MoveToConfirmed :
        MailboxViewAction,
        AffectingTopAppBar,
        AffectingBottomAppBar,
        AffectingMailboxList,
        AffectingBottomSheet

    object RequestMoreActionsBottomSheet : MailboxViewAction, AffectingBottomSheet
    object Star : MailboxViewAction, AffectingMailboxList, AffectingBottomSheet
    object UnStar : MailboxViewAction, AffectingMailboxList, AffectingBottomSheet
    object MoveToArchive :
        MailboxViewAction,
        AffectingTopAppBar,
        AffectingBottomAppBar,
        AffectingMailboxList,
        AffectingBottomSheet

    object MoveToSpam :
        MailboxViewAction,
        AffectingTopAppBar,
        AffectingBottomAppBar,
        AffectingMailboxList,
        AffectingBottomSheet

    object DismissBottomSheet : MailboxViewAction, AffectingBottomSheet

    /*
     *`OnOfflineWithData` and `OnErrorWithData` are not actual Actions which are actively performed by the user
     * but rather "Events" which happen when loading mailbox items. They are represented as actions due to
     * limitations of the paging library, which delivers such events on the Composable. See commit 7c3f88 for more.
     */
    object OnOfflineWithData : MailboxViewAction, AffectingMailboxList
    object OnErrorWithData : MailboxViewAction, AffectingMailboxList
    object CloseOnboarding : MailboxViewAction, MailboxOperation.AffectingOnboarding
    object DeleteAll : MailboxViewAction
    object DeleteAllConfirmed : MailboxViewAction
    object DeleteAllDialogDismissed : MailboxViewAction, AffectingClearDialog
}

internal sealed interface MailboxEvent : MailboxOperation {

    data class NewLabelSelected(
        val selectedLabel: MailLabel,
        val selectedLabelCount: Int?
    ) : MailboxEvent, AffectingTopAppBar, AffectingUnreadFilter, AffectingMailboxList

    data class SelectedLabelChanged(
        val selectedLabel: MailLabel
    ) : MailboxEvent, AffectingTopAppBar, AffectingMailboxList

    data class SelectedLabelCountChanged(
        val selectedLabelCount: Int
    ) : MailboxEvent, AffectingUnreadFilter

    data class EnterSelectionMode(
        val item: MailboxItemUiModel
    ) : MailboxEvent, AffectingTopAppBar, AffectingMailboxList, AffectingBottomAppBar

    data class SwipeActionsChanged(
        val swipeActionsPreference: SwipeActionsUiModel
    ) : MailboxEvent, AffectingMailboxList

    object ShowOnboarding : MailboxEvent, MailboxOperation.AffectingOnboarding

    data class Trash(val numAffectedMessages: Int) :
        MailboxEvent,
        AffectingMailboxList,
        AffectingTopAppBar,
        AffectingBottomAppBar,
        AffectingActionMessage

    data class Delete(val viewMode: ViewMode, val numAffectedMessages: Int) : MailboxEvent, AffectingDeleteDialog
    data class DeleteConfirmed(val viewMode: ViewMode, val numAffectedMessages: Int) :
        MailboxEvent,
        AffectingMailboxList,
        AffectingTopAppBar,
        AffectingBottomAppBar,
        AffectingActionMessage,
        AffectingDeleteDialog

    data class DeleteAll(val viewMode: ViewMode, val location: LabelId) : MailboxEvent, AffectingClearDialog
    data class DeleteAllConfirmed(val viewMode: ViewMode) : MailboxEvent, AffectingClearDialog

    sealed interface ItemClicked : MailboxEvent {

        val item: MailboxItemUiModel

        data class ItemDetailsOpenedInViewMode(
            override val item: MailboxItemUiModel,
            val preferredViewMode: ViewMode
        ) : ItemClicked, AffectingMailboxList

        data class OpenComposer(
            override val item: MailboxItemUiModel
        ) : ItemClicked, AffectingMailboxList

        data class ItemAddedToSelection(
            override val item: MailboxItemUiModel
        ) : ItemClicked, AffectingMailboxList, AffectingTopAppBar

        data class ItemRemovedFromSelection(
            override val item: MailboxItemUiModel
        ) : ItemClicked, AffectingMailboxList, AffectingTopAppBar
    }

    data class ItemsRemovedFromSelection(
        val itemIds: List<String>
    ) : MailboxEvent, AffectingMailboxList, AffectingTopAppBar

    data class MessageBottomBarEvent(
        val bottomBarEvent: BottomBarEvent
    ) : MailboxEvent, AffectingBottomAppBar

    data class MailboxBottomSheetEvent(
        val bottomSheetOperation: BottomSheetOperation
    ) : MailboxEvent, AffectingBottomSheet

    object ErrorLabeling : MailboxEvent, AffectingErrorBar
    object ErrorRetrievingCustomMailLabels : MailboxEvent, AffectingErrorBar, AffectingBottomSheet
    object ErrorRetrievingFolderColorSettings : MailboxEvent, AffectingErrorBar, AffectingBottomSheet
    object ErrorMoving : MailboxEvent, AffectingErrorBar
    object ErrorRetrievingDestinationMailFolders : MailboxEvent, AffectingErrorBar, AffectingBottomSheet
}


