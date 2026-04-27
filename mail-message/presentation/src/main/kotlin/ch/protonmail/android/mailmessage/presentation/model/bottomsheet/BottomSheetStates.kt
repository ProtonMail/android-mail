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

package ch.protonmail.android.mailmessage.presentation.model.bottomsheet

import androidx.compose.runtime.Immutable
import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.domain.model.AvailableActions
import ch.protonmail.android.mailcommon.presentation.model.ActionUiModel
import ch.protonmail.android.mailcommon.presentation.model.AvatarImageUiModel
import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import ch.protonmail.android.mailcommon.presentation.model.BottomSheetContentState
import ch.protonmail.android.mailcommon.presentation.model.BottomSheetOperation
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcontact.domain.model.ContactId
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.maillabel.presentation.bottomsheet.LabelAsBottomSheetEntryPoint
import ch.protonmail.android.maillabel.presentation.bottomsheet.LabelAsItemId
import ch.protonmail.android.maillabel.presentation.bottomsheet.moveto.MoveToBottomSheetEntryPoint
import ch.protonmail.android.maillabel.presentation.bottomsheet.moveto.MoveToItemId
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.Participant
import ch.protonmail.android.mailmessage.presentation.model.ContactActionUiModel
import ch.protonmail.android.mailsnooze.presentation.model.SnoozeConversationId
import kotlinx.collections.immutable.ImmutableList
import me.proton.core.domain.entity.UserId

sealed interface MoveToBottomSheetState : BottomSheetContentState {

    @Immutable
    data class Requested(
        val userId: UserId,
        val currentLabel: LabelId,
        val itemIds: List<MoveToItemId>,
        val entryPoint: MoveToBottomSheetEntryPoint
    ) : MoveToBottomSheetState

    sealed interface MoveToBottomSheetOperation : BottomSheetOperation
    sealed interface MoveToBottomSheetEvent : MoveToBottomSheetOperation {
        data class Ready(
            val userId: UserId,
            val currentLabel: LabelId,
            val itemIds: List<MoveToItemId>,
            val entryPoint: MoveToBottomSheetEntryPoint
        ) : MoveToBottomSheetEvent
    }
}

sealed interface LabelAsBottomSheetState : BottomSheetContentState {

    @Immutable
    data class Requested(
        val userId: UserId,
        val currentLocationLabelId: LabelId,
        val itemIds: List<LabelAsItemId>,
        val entryPoint: LabelAsBottomSheetEntryPoint
    ) : LabelAsBottomSheetState

    sealed interface LabelAsBottomSheetOperation : BottomSheetOperation
    sealed interface LabelAsBottomSheetEvent : LabelAsBottomSheetOperation {
        data class Ready(
            val userId: UserId,
            val currentLabel: LabelId,
            val itemIds: List<LabelAsItemId>,
            val entryPoint: LabelAsBottomSheetEntryPoint
        ) : LabelAsBottomSheetEvent
    }
}

sealed interface MailboxMoreActionsBottomSheetState : BottomSheetContentState {

    data class Data(
        val hiddenActionUiModels: ImmutableList<ActionUiModel>,
        val visibleActionUiModels: ImmutableList<ActionUiModel>,
        val customizeToolbarActionUiModel: ActionUiModel,
        val selectedCount: Int
    ) : MailboxMoreActionsBottomSheetState

    data object Loading : MailboxMoreActionsBottomSheetState

    sealed interface MailboxMoreActionsBottomSheetOperation : BottomSheetOperation
    sealed interface MailboxMoreActionsBottomSheetEvent : MailboxMoreActionsBottomSheetOperation {
        data class ActionData(
            val hiddenActionUiModels: ImmutableList<ActionUiModel>,
            val visibleActionUiModels: ImmutableList<ActionUiModel>,
            val customizeToolbarActionUiModel: ActionUiModel,
            val selectedCount: Int
        ) : MailboxMoreActionsBottomSheetEvent
    }
}

sealed interface DetailMoreActionsBottomSheetState : BottomSheetContentState {
    data class Data(
        val detailDataUiModel: DetailDataUiModel,
        val replyActions: ImmutableList<ActionUiModel>,
        val messageActions: ImmutableList<ActionUiModel>,
        val moveActions: ImmutableList<ActionUiModel>,
        val genericActions: ImmutableList<ActionUiModel>,
        val customizeToolbarActionUiModel: ActionUiModel?
    ) : DetailMoreActionsBottomSheetState

    data object Loading : DetailMoreActionsBottomSheetState

    sealed interface DetailMoreActionsBottomSheetOperation : BottomSheetOperation
    sealed interface DetailMoreActionsBottomSheetEvent : DetailMoreActionsBottomSheetOperation {
        data class DataLoaded(
            val messageSubject: String,
            val messageIdInConversation: String?,
            val availableActions: AvailableActions,
            val customizeToolbarAction: Action?
        ) : DetailMoreActionsBottomSheetEvent
    }

    data class DetailDataUiModel(
        val headerSubjectText: TextUiModel,
        val messageIdInConversation: String?
    )
}

sealed interface ManageAccountSheetState : BottomSheetContentState {
    data object Requested : ManageAccountSheetState

    sealed interface ManageAccountsBottomSheetOperation : BottomSheetOperation
    sealed interface ManageAccountsBottomSheetEvent : ManageAccountsBottomSheetOperation {
        data object Ready : ManageAccountsBottomSheetEvent
    }
}

sealed interface ContactActionsBottomSheetState : BottomSheetContentState {

    sealed interface Origin {
        data class MessageDetails(val messageId: MessageId) : Origin
        data object Unknown : Origin
    }

    data class ContactActionsGroups(
        val firstGroup: ImmutableList<ContactActionUiModel>,
        val secondGroup: ImmutableList<ContactActionUiModel>,
        val thirdGroup: ImmutableList<ContactActionUiModel>
    )

    data class Data(
        val participant: Participant,
        val avatarUiModel: AvatarUiModel?,
        val actions: ContactActionsGroups,
        val origin: Origin,
        val avatarImageUiModel: AvatarImageUiModel = AvatarImageUiModel.NoImageAvailable
    ) : ContactActionsBottomSheetState

    data object Loading : ContactActionsBottomSheetState

    sealed interface ContactActionsBottomSheetOperation : BottomSheetOperation

    sealed interface ContactActionsBottomSheetEvent : ContactActionsBottomSheetOperation {
        data class ActionData(
            val participant: Participant,
            val avatarUiModel: AvatarUiModel?,
            val contactId: ContactId?,
            val origin: Origin,
            val isSenderBlocked: Boolean,
            val isPrimaryUserAddress: Boolean,
            val avatarImageUiModel: AvatarImageUiModel = AvatarImageUiModel.NoImageAvailable
        ) : ContactActionsBottomSheetEvent
    }
}

sealed interface SnoozeSheetState : BottomSheetContentState {
    @Immutable
    data class Requested(
        val userId: UserId,
        val labelId: LabelId,
        val itemIds: List<SnoozeConversationId>
    ) : BottomSheetContentState

    sealed interface SnoozeOptionsBottomSheetOperation : BottomSheetOperation
    sealed interface SnoozeOptionsBottomSheetEvent : SnoozeOptionsBottomSheetOperation {
        data class Ready(
            val userId: UserId,
            val labelId: LabelId,
            val itemIds: List<SnoozeConversationId>
        ) : SnoozeOptionsBottomSheetEvent
    }
}
