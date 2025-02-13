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

package ch.protonmail.android.mailcomposer.presentation.facade

import ch.protonmail.android.mailcomposer.domain.model.DraftFields
import ch.protonmail.android.mailcomposer.domain.usecase.ClearMessageSendingError
import ch.protonmail.android.mailcomposer.domain.usecase.ObserveMessageSendingError
import ch.protonmail.android.mailcomposer.domain.usecase.SendMessage
import ch.protonmail.android.mailcomposer.presentation.usecase.FormatMessageSendingError
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.model.MessageId
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class MessageSendingFacade @Inject constructor(
    private val sendMessage: SendMessage,
    private val observeSendingErrors: ObserveMessageSendingError,
    private val formatMessageSendingError: FormatMessageSendingError,
    private val clearMessageSendingError: ClearMessageSendingError
) {

    suspend fun sendMessage(
        userId: UserId,
        messageId: MessageId,
        fields: DraftFields,
        action: DraftAction = DraftAction.Compose
    ) = sendMessage.invoke(userId, messageId, fields, action)

    fun observeAndFormatSendingErrors(userId: UserId, messageId: MessageId) =
        observeSendingErrors.invoke(userId, messageId).map { formatMessageSendingError.invoke(it) }

    suspend fun clearMessageSendingError(userId: UserId, messageId: MessageId) =
        clearMessageSendingError.invoke(userId, messageId)
}
