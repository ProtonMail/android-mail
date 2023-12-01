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

package ch.protonmail.android.mailmessage.data.usecase

import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmessage.data.remote.resource.MessageResource
import ch.protonmail.android.mailmessage.domain.model.Message
import ch.protonmail.android.mailmessage.domain.repository.OutboxRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

/**
 * Filter draft messages which are already in Outbox
 */
class FilterDraftMessagesAlreadyInOutbox @Inject constructor(
    private val outboxRepository: OutboxRepository
) {

    suspend operator fun invoke(userId: UserId, entities: List<MessageResource>): List<Message> {
        // Get outbox messages
        val outboxMessages = outboxRepository
            .observeAll(userId)
            .map { it.map { messageId -> messageId.id } }
            .firstOrNull()
            ?: emptyList()

        return if (outboxMessages.isEmpty()) {
            entities
        } else {
            entities.filter { message ->
                !(message.labelIds.contains(SystemLabelId.AllDrafts.labelId.id) && message.id in outboxMessages)
            }
        }.map { it.toMessage(userId) }
    }
}
