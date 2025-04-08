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

import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.ActionUiModel
import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.presentation.MailLabelUiModel
import ch.protonmail.android.maillabel.presentation.model.LabelUiModelWithSelectedState
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.Participant
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import me.proton.core.contact.domain.entity.ContactId
import me.proton.core.label.domain.entity.LabelId

data class BottomSheetState(
    val contentState: BottomSheetContentState?,
    val bottomSheetVisibilityEffect: Effect<BottomSheetVisibilityEffect> = Effect.empty()
) {

    fun isShowEffectWithoutContent() =
        bottomSheetVisibilityEffect == Effect.of(BottomSheetVisibilityEffect.Show) && contentState == null
}

sealed interface BottomSheetVisibilityEffect {
    object Show : BottomSheetVisibilityEffect
    object Hide : BottomSheetVisibilityEffect
}

sealed interface BottomSheetContentState
sealed interface BottomSheetOperation {
    object Requested : BottomSheetOperation
    object Dismiss : BottomSheetOperation
}

sealed interface LabelAsBottomSheetEntryPoint {
    data object Conversation : LabelAsBottomSheetEntryPoint
    data class Message(val messageId: MessageId) : LabelAsBottomSheetEntryPoint
    data class LabelAsSwipeAction(val itemId: String) : LabelAsBottomSheetEntryPoint
    data object SelectionMode : LabelAsBottomSheetEntryPoint
}

sealed interface MoveToBottomSheetEntryPoint {
    data object Conversation : MoveToBottomSheetEntryPoint
    data class Message(val messageId: MessageId) : MoveToBottomSheetEntryPoint
    data class MoveToSwipeAction(val itemId: String) : MoveToBottomSheetEntryPoint
    data object SelectionMode : MoveToBottomSheetEntryPoint
}

sealed interface MailboxUpsellingEntryPoint {
    object Mailbox : MailboxUpsellingEntryPoint
    object AutoDelete : MailboxUpsellingEntryPoint
}

sealed interface MoveToBottomSheetState : BottomSheetContentState {

    data class Data(
        val moveToDestinations: ImmutableList<MailLabelUiModel>,
        val selected: MailLabelUiModel?,
        val entryPoint: MoveToBottomSheetEntryPoint
    ) : MoveToBottomSheetState

    data object Loading : MoveToBottomSheetState

    sealed interface MoveToBottomSheetOperation : BottomSheetOperation

    sealed interface MoveToBottomSheetEvent : MoveToBottomSheetOperation {
        data class ActionData(
            val moveToDestinations: ImmutableList<MailLabelUiModel>,
            val entryPoint: MoveToBottomSheetEntryPoint
        ) : MoveToBottomSheetEvent
    }

    sealed interface MoveToBottomSheetAction : MoveToBottomSheetOperation {
        data class MoveToDestinationSelected(val mailLabelId: MailLabelId) : MoveToBottomSheetAction
    }
}

sealed interface LabelAsBottomSheetState : BottomSheetContentState {

    data class Data(
        val labelUiModelsWithSelectedState: ImmutableList<LabelUiModelWithSelectedState>,
        val entryPoint: LabelAsBottomSheetEntryPoint
    ) : LabelAsBottomSheetState

    data object Loading : LabelAsBottomSheetState

    sealed interface LabelAsBottomSheetOperation : BottomSheetOperation

    sealed interface LabelAsBottomSheetEvent : LabelAsBottomSheetOperation {
        data class ActionData(
            val customLabelList: ImmutableList<MailLabelUiModel.Custom>,
            val selectedLabels: ImmutableList<LabelId>,
            val partiallySelectedLabels: ImmutableList<LabelId> = emptyList<LabelId>().toImmutableList(),
            val entryPoint: LabelAsBottomSheetEntryPoint
        ) : LabelAsBottomSheetEvent
    }

    sealed interface LabelAsBottomSheetAction : LabelAsBottomSheetOperation {
        data class LabelToggled(val labelId: LabelId) : LabelAsBottomSheetAction
    }
}

sealed interface MailboxMoreActionsBottomSheetState : BottomSheetContentState {

    data class Data(
        val actionUiModels: ImmutableList<ActionUiModel>
    ) : MailboxMoreActionsBottomSheetState
    object Loading : MailboxMoreActionsBottomSheetState

    sealed interface MailboxMoreActionsBottomSheetOperation : BottomSheetOperation
    sealed interface MailboxMoreActionsBottomSheetEvent : MailboxMoreActionsBottomSheetOperation {
        data class ActionData(
            val actionUiModels: ImmutableList<ActionUiModel>
        ) : MailboxMoreActionsBottomSheetEvent
    }
}

sealed interface DetailMoreActionsBottomSheetState : BottomSheetContentState {
    data class Data(
        val isAffectingConversation: Boolean,
        val messageDataUiModel: MessageDataUiModel,
        val replyActionsUiModel: ImmutableList<ActionUiModel>
    ) : DetailMoreActionsBottomSheetState

    object Loading : DetailMoreActionsBottomSheetState

    sealed interface MessageDetailMoreActionsBottomSheetOperation : BottomSheetOperation
    sealed interface MessageDetailMoreActionsBottomSheetEvent : MessageDetailMoreActionsBottomSheetOperation {
        data class DataLoaded(
            val affectingConversation: Boolean,
            val messageSender: String,
            val messageSubject: String,
            val messageId: String,
            val participantsCount: Int,
            val actions: List<Action>
        ) : MessageDetailMoreActionsBottomSheetEvent
    }

    data class MessageDataUiModel(
        val headerSubjectText: TextUiModel,
        val headerDescriptionText: TextUiModel,
        val messageId: String
    )
}

sealed interface UpsellingBottomSheetState : BottomSheetContentState {
    data class Requested(val entryPoint: MailboxUpsellingEntryPoint) : UpsellingBottomSheetState
    sealed interface UpsellingBottomSheetOperation : BottomSheetOperation
    sealed interface UpsellingBottomSheetEvent : UpsellingBottomSheetOperation {
        data class Ready(val entryPoint: MailboxUpsellingEntryPoint) : UpsellingBottomSheetEvent
    }
}

sealed interface ContactActionsBottomSheetState : BottomSheetContentState {

    data class Data(
        val participant: Participant,
        val avatarUiModel: AvatarUiModel,
        val contactId: ContactId?
    ) : ContactActionsBottomSheetState

    object Loading : ContactActionsBottomSheetState

    sealed interface ContactActionsBottomSheetOperation : BottomSheetOperation

    sealed interface ContactActionsBottomSheetEvent : ContactActionsBottomSheetOperation {
        data class ActionData(
            val participant: Participant,
            val avatarUiModel: AvatarUiModel,
            val contactId: ContactId?
        ) : ContactActionsBottomSheetEvent
    }
}
