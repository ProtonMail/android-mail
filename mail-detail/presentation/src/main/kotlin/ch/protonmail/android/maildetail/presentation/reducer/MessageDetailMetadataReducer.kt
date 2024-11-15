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

package ch.protonmail.android.maildetail.presentation.reducer

import ch.protonmail.android.maildetail.presentation.mapper.MessageDetailActionBarUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.MessageDetailFooterUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.MessageDetailHeaderUiModelMapper
import ch.protonmail.android.maildetail.presentation.model.MessageDetailEvent
import ch.protonmail.android.maildetail.presentation.model.MessageDetailOperation
import ch.protonmail.android.maildetail.presentation.model.MessageMetadataState
import ch.protonmail.android.maildetail.presentation.model.MessageViewAction
import javax.inject.Inject

class MessageDetailMetadataReducer @Inject constructor(
    private val messageDetailActionBarUiModelMapper: MessageDetailActionBarUiModelMapper,
    private val messageDetailHeaderUiModelMapper: MessageDetailHeaderUiModelMapper,
    private val messageDetailFooterUiModelMapper: MessageDetailFooterUiModelMapper
) {

    @SuppressWarnings("NotImplementedDeclaration")
    suspend fun newStateFrom(
        currentState: MessageMetadataState,
        event: MessageDetailOperation.AffectingMessage
    ): MessageMetadataState {
        return when (event) {
            is MessageDetailEvent.MessageWithLabelsEvent -> MessageMetadataState.Data(
                messageDetailActionBarUiModelMapper.toUiModel(event.messageWithLabels.message),
                messageDetailHeaderUiModelMapper.toUiModel(
                    event.messageWithLabels,
                    event.contacts,
                    event.folderColor,
                    event.autoDeleteSetting
                ),
                messageDetailFooterUiModelMapper.toUiModel(event.messageWithLabels)
            )

            is MessageDetailEvent.NoCachedMetadata -> currentState
            is MessageViewAction.Star -> currentState.toNewStateForStarredMessage()
            is MessageViewAction.UnStar -> currentState.toNewStateForUnStarredMessage()
            is MessageDetailEvent.ErrorAddingStar -> currentState.toNewStateForErrorAddingStar()
            is MessageDetailEvent.ErrorRemovingStar -> currentState.toNewStateForErrorRemovingStar()
        }
    }

    private fun MessageMetadataState.toNewStateForStarredMessage() = when (this) {
        is MessageMetadataState.Loading -> this
        is MessageMetadataState.Data -> copy(messageDetailActionBar.copy(isStarred = true))
    }

    private fun MessageMetadataState.toNewStateForUnStarredMessage() = when (this) {
        is MessageMetadataState.Loading -> this
        is MessageMetadataState.Data -> copy(messageDetailActionBar.copy(isStarred = false))
    }

    private fun MessageMetadataState.toNewStateForErrorAddingStar() = when (this) {
        is MessageMetadataState.Loading -> this
        is MessageMetadataState.Data -> copy(messageDetailActionBar.copy(isStarred = false))
    }

    private fun MessageMetadataState.toNewStateForErrorRemovingStar() = when (this) {
        is MessageMetadataState.Loading -> this
        is MessageMetadataState.Data -> copy(messageDetailActionBar.copy(isStarred = true))
    }
}
