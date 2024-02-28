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

package ch.protonmail.android.mailmessage.data

import ch.protonmail.android.mailmessage.data.local.MessageLocalDataSource
import ch.protonmail.android.mailmessage.data.remote.resource.MessageResource
import ch.protonmail.android.mailmessage.data.usecase.ExcludeDraftMessagesAlreadyInOutbox
import ch.protonmail.android.mailmessage.data.usecase.GetMessageIdsInDraftState
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailpagination.domain.model.PageKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.proton.core.domain.entity.UserId
import me.proton.core.eventmanager.domain.EventListener
import me.proton.core.eventmanager.domain.EventManagerConfig
import me.proton.core.eventmanager.domain.entity.Action
import me.proton.core.eventmanager.domain.entity.Event
import me.proton.core.eventmanager.domain.entity.EventsResponse
import me.proton.core.label.data.local.LabelDatabase
import me.proton.core.util.kotlin.deserialize
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class MessagesEvents(
    @SerialName("Messages")
    val messages: List<MessageEvent>? = null
)

@Serializable
data class MessageEvent(
    @SerialName("ID")
    val id: String,
    @SerialName("Action")
    val action: Int,
    @SerialName("Message")
    val message: MessageResource? = null
)

@Singleton
open class MessageEventListener @Inject constructor(
    private val db: LabelDatabase,
    private val localDataSource: MessageLocalDataSource,
    private val repository: MessageRepository,
    private val excludeDraftMessagesAlreadyInOutbox: ExcludeDraftMessagesAlreadyInOutbox,
    private val getMessageIdsInDraftState: GetMessageIdsInDraftState
) : EventListener<String, MessageResource>() {

    override val type = Type.Core
    override val order = 1

    override suspend fun deserializeEvents(
        config: EventManagerConfig,
        response: EventsResponse
    ): List<Event<String, MessageResource>>? {
        return response.body.deserialize<MessagesEvents>().messages?.map {
            Event(requireNotNull(Action.map[it.action]), it.id, it.message)
        }
    }

    override suspend fun <R> inTransaction(block: suspend () -> R): R = db.inTransaction(block)

    override suspend fun onCreate(config: EventManagerConfig, entities: List<MessageResource>) {
        excludeDraftsInOutboxAndUpsert(config.userId, entities)
    }

    override suspend fun onUpdate(config: EventManagerConfig, entities: List<MessageResource>) {
        excludeDraftsInOutboxAndUpsert(config.userId, entities)
    }

    override suspend fun onPartial(config: EventManagerConfig, entities: List<MessageResource>) {
        excludeDraftsInOutboxAndUpsert(config.userId, entities)
    }

    override suspend fun onDelete(config: EventManagerConfig, keys: List<String>) {
        localDataSource.deleteMessages(config.userId, keys.map { MessageId(it) })
    }

    override suspend fun onResetAll(config: EventManagerConfig) {
        // We should not delete the messages in draft state. That can cause sending failure
        // since the local data will be lost
        val draftMessagesToExclude = getMessageIdsInDraftState(config.userId)
        localDataSource.deleteAllMessagesExcept(config.userId, draftMessagesToExclude)
        repository.getRemoteMessages(config.userId, PageKey())
    }

    private suspend fun excludeDraftsInOutboxAndUpsert(userId: UserId, messages: List<MessageResource>) {
        val messagesToUpsert = excludeDraftMessagesAlreadyInOutbox(userId, messages.map { it.toMessage(userId) })
        if (messagesToUpsert.isNotEmpty()) {
            localDataSource.upsertMessages(messagesToUpsert)
        }
    }
}
