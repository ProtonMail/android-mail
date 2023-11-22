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
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation.AffectingActionMessage
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation.AffectingBottomAppBar
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation.AffectingDeleteDialog
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation.AffectingMailboxList
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation.AffectingTopAppBar
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation.AffectingUnreadFilter
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.BottomSheetOperation
import me.proton.core.mailsettings.domain.entity.ViewMode

internal sealed interface MailboxOperation {
    sealed interface AffectingTopAppBar
    sealed interface AffectingUnreadFilter
    sealed interface AffectingMailboxList
    sealed interface AffectingBottomAppBar
    sealed interface AffectingOnboarding
    sealed interface AffectingActionMessage
    sealed interface AffectingDeleteDialog
    sealed interface AffectingBottomSheet
}

internal sealed interface MailboxViewAction : MailboxOperation {
    data class OnItemLongClicked(
        val item: MailboxItemUiModel
    ) : MailboxViewAction

    data class OnItemAvatarClicked(
        val item: MailboxItemUiModel
    ) : MailboxViewAction

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

    /*
     *`OnOfflineWithData` and `OnErrorWithData` are not actual Actions which are actively performed by the user
     * but rather "Events" which happen when loading mailbox items. They are represented as actions due to
     * limitations of the paging library, which delivers such events on the Composable. See commit 7c3f88 for more.
     */
    object OnOfflineWithData : MailboxViewAction, AffectingMailboxList
    object OnErrorWithData : MailboxViewAction, AffectingMailboxList
    object CloseOnboarding : MailboxViewAction, MailboxOperation.AffectingOnboarding
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

    data class SelectionModeEnabledChanged(
        val selectionModeEnabled: Boolean
    ) : MailboxEvent, AffectingMailboxList

    data class EnterSelectionMode(
        val item: MailboxItemUiModel
    ) : MailboxEvent, AffectingTopAppBar, AffectingMailboxList, AffectingBottomAppBar

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

    data class MessageBottomBarEvent(
        val bottomBarEvent: BottomBarEvent
    ) : MailboxEvent, AffectingBottomAppBar

    data class MailboxBottomSheetEvent(
        val bottomSheetOperation: BottomSheetOperation
    ) : MailboxEvent, MailboxOperation.AffectingBottomSheet
}


