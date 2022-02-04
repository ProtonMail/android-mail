/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonCore.
 *
 * ProtonCore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonCore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonCore.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailmessage.data

import ch.protonmail.android.mailpagination.domain.entity.PageKey
import ch.protonmail.android.mailmessage.data.remote.resource.MessageResource
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.repository.MessageLocalDataSource
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.proton.core.eventmanager.domain.EventListener
import me.proton.core.eventmanager.domain.EventManagerConfig
import me.proton.core.eventmanager.domain.entity.Action
import me.proton.core.eventmanager.domain.entity.Event
import me.proton.core.eventmanager.domain.entity.EventsResponse
import me.proton.core.label.data.local.LabelDatabase
import me.proton.core.util.kotlin.deserializeOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class MessagesEvents(
    @SerialName("Messages")
    val messages: List<MessageEvent>,
)

@Serializable
data class MessageEvent(
    @SerialName("ID")
    val id: String,
    @SerialName("Action")
    val action: Int,
    @SerialName("Message")
    val message: MessageResource? = null,
)

@Singleton
open class MessageEventListener @Inject constructor(
    private val db: LabelDatabase,
    private val localDataSource: MessageLocalDataSource,
    private val repository: MessageRepository,
) : EventListener<String, MessageResource>() {

    override val type = Type.Core
    override val order = 1

    override suspend fun deserializeEvents(
        config: EventManagerConfig,
        response: EventsResponse,
    ): List<Event<String, MessageResource>>? {
        return response.body.deserializeOrNull<MessagesEvents>()?.messages?.map {
            Event(requireNotNull(Action.map[it.action]), it.id, it.message)
        }
    }

    override suspend fun <R> inTransaction(block: suspend () -> R): R = db.inTransaction(block)

    override suspend fun onCreate(config: EventManagerConfig, entities: List<MessageResource>) {
        localDataSource.upsertMessages(entities.map { it.toMessage(config.userId) })
    }

    override suspend fun onUpdate(config: EventManagerConfig, entities: List<MessageResource>) {
        localDataSource.upsertMessages(entities.map { it.toMessage(config.userId) })
    }

    override suspend fun onPartial(config: EventManagerConfig, entities: List<MessageResource>) {
        localDataSource.upsertMessages(entities.map { it.toMessage(config.userId) })
    }

    override suspend fun onDelete(config: EventManagerConfig, keys: List<String>) {
        localDataSource.deleteMessage(config.userId, keys.map { MessageId(it) })
    }

    override suspend fun onResetAll(config: EventManagerConfig) {
        localDataSource.deleteAllMessages(config.userId)
        repository.getMessages(config.userId, PageKey())
    }
}
