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

package ch.protonmail.android.maillabel.presentation.bottomsheet.moveto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailconversation.domain.usecase.MoveConversations
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.ViewMode
import ch.protonmail.android.maillabel.presentation.model.MailLabelText
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.usecase.MoveMessages
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.proton.core.domain.entity.UserId

@HiltViewModel(assistedFactory = MoveToViewModel.Factory::class)
internal class MoveToViewModel @AssistedInject constructor(
    @Assisted val initialData: MoveToBottomSheet.InitialData,
    private val observePrimaryUserId: ObservePrimaryUserId,
    private val getMoveToLocations: GetMoveToLocations,
    private val moveMessages: MoveMessages,
    private val moveConversations: MoveConversations,
    private val reducer: MoveToReducer
) : ViewModel() {

    private val mutableState = MutableStateFlow<MoveToState>(MoveToState.Loading)
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val entryPoint = initialData.entryPoint
            val content = getInitialStateForEntryPoint(entryPoint).getOrElse {
                return@launch emitNewStateFrom(MoveToOperation.MoveToEvent.LoadingError)
            }

            emitNewStateFrom(operation = MoveToOperation.MoveToEvent.InitialData(content, entryPoint))
        }
    }

    fun submit(action: MoveToOperation.MoveToAction) {
        viewModelScope.launch {
            when (action) {
                is MoveToOperation.MoveToAction.MoveToDestinationSelected ->
                    processMoveToOperation(action.mailLabelId, action.mailLabelText, isCategory = false)

                is MoveToOperation.MoveToAction.CategorySelected -> processCategorySelection(action)
            }
        }
    }

    private suspend fun processCategorySelection(action: MoveToOperation.MoveToAction.CategorySelected) {
        // Selecting the category the messages already belong to is a no-op.
        if (action.mailLabelId.labelId == initialData.activeCategoryId) {
            emitNewStateFrom(MoveToOperation.MoveToEvent.CategoryNoChange)
            return
        }

        processMoveToOperation(action.mailLabelId, action.mailLabelText, isCategory = true)
    }

    private suspend fun processMoveToOperation(
        mailLabelId: MailLabelId,
        mailLabelText: MailLabelText,
        isCategory: Boolean
    ) {
        val userId = observePrimaryUserId().filterNotNull().first()

        val handleMoveData = HandleMoveData(
            userId = userId,
            selectedItems = initialData.items.toSet(),
            labelId = mailLabelId,
            mailLabelText = mailLabelText,
            entryPoint = initialData.entryPoint
        )

        emitNewStateFrom(handleMoveOperation(handleMoveData, isCategory))
    }

    private suspend fun handleMoveOperation(data: HandleMoveData, isCategory: Boolean): MoveToOperation {
        val items = data.selectedItems
        val label = data.labelId.labelId
        val mailLabelText = data.mailLabelText
        val entryPoint = initialData.entryPoint

        return when (entryPoint) {
            MoveToBottomSheetEntryPoint.Conversation -> moveConversations(
                userId = data.userId,
                conversationIds = items.map { ConversationId(it.value) },
                labelId = label
            )

            is MoveToBottomSheetEntryPoint.Message -> moveMessages(
                userId = data.userId,
                messageIds = items.map { MessageId(it.value) },
                labelId = label
            )

            is MoveToBottomSheetEntryPoint.Mailbox -> when (entryPoint.viewMode) {
                ViewMode.ConversationGrouping -> moveConversations(
                    userId = data.userId,
                    conversationIds = items.map { ConversationId(it.value) },
                    labelId = label
                )

                ViewMode.NoConversationGrouping -> moveMessages(
                    userId = data.userId,
                    messageIds = items.map { MessageId(it.value) },
                    labelId = label
                )
            }
        }.fold(
            ifLeft = { MoveToOperation.MoveToEvent.ErrorMoving },
            ifRight = { MoveToOperation.MoveToEvent.MoveComplete(mailLabelText, isCategory) }
        )
    }

    private fun emitNewStateFrom(operation: MoveToOperation) {
        mutableState.update { reducer.newStateFrom(state.value, operation) }
    }

    private suspend fun getInitialStateForEntryPoint(
        entryPoint: MoveToBottomSheetEntryPoint
    ): Either<GetInitialStateError, List<MailLabel>> {
        val userId = initialData.userId
        val labelId = initialData.currentLocationLabelId
        val items = initialData.items.takeIf { it.isNotEmpty() } ?: return GetInitialStateError.NoItemsProvided.left()

        val result = when (entryPoint) {
            is MoveToBottomSheetEntryPoint.Conversation -> getMoveToLocations.forConversation(
                userId = userId,
                labelId = labelId,
                conversationId = ConversationId(items.first().value)
            )

            is MoveToBottomSheetEntryPoint.Message -> getMoveToLocations.forMessage(
                userId = userId,
                labelId = labelId,
                messageId = MessageId(items.first().value)
            )

            is MoveToBottomSheetEntryPoint.Mailbox -> getMoveToLocations.forMailbox(
                userId = userId,
                labelId = labelId,
                moveToItemIds = items,
                viewMode = entryPoint.viewMode
            )
        }

        return result.mapLeft { error ->
            GetInitialStateError.LoadingContentError(error)
        }
    }

    private data class HandleMoveData(
        val userId: UserId,
        val selectedItems: Set<MoveToItemId>,
        val entryPoint: MoveToBottomSheetEntryPoint,
        val mailLabelText: MailLabelText,
        val labelId: MailLabelId
    )

    sealed interface GetInitialStateError {
        data object NoItemsProvided : GetInitialStateError

        @JvmInline
        value class LoadingContentError(val error: DataError) : GetInitialStateError
    }

    @AssistedFactory
    interface Factory {

        fun create(payload: MoveToBottomSheet.InitialData): MoveToViewModel
    }
}
