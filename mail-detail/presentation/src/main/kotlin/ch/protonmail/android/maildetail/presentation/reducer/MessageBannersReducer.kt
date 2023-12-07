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

import ch.protonmail.android.maildetail.presentation.mapper.MessageBannersUiModelMapper
import ch.protonmail.android.maildetail.presentation.model.MessageBannersState
import ch.protonmail.android.maildetail.presentation.model.MessageDetailEvent
import ch.protonmail.android.maildetail.presentation.model.MessageDetailOperation
import javax.inject.Inject

class MessageBannersReducer @Inject constructor(
    private val messageBannersUiModelMapper: MessageBannersUiModelMapper
) {

    fun newStateFrom(event: MessageDetailOperation.AffectingMessageBanners): MessageBannersState {
        return when (event) {
            is MessageDetailEvent.MessageWithLabelsEvent -> MessageBannersState.Data(
                messageBannersUiModelMapper.createMessageBannersUiModel(event.messageWithLabels.message)
            )
        }
    }
}
