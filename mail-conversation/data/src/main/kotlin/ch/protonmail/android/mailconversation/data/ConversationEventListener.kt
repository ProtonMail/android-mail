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

package ch.protonmail.android.mailconversation.data

import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailconversation.data.remote.resource.ConversationResource
import ch.protonmail.android.mailconversation.domain.repository.ConversationLocalDataSource
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import ch.protonmail.android.mailpagination.domain.model.PageKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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
data class ConversationsEvents(
    @SerialName("Conversations")
    val conversations: List<ConversationEvent>? = null
)

@Serializable
data class ConversationEvent(
    @SerialName("ID")
    val id: String,
    @SerialName("Action")
    val action: Int,
    @SerialName("Conversation")
    val conversation: ConversationResource? = null
)

@Singleton
open class ConversationEventListener @Inject constructor(
    private val db: LabelDatabase,
    private val localDataSource: ConversationLocalDataSource,
    private val repository: ConversationRepository
) : EventListener<String, ConversationResource>() {

    override val type = Type.Core
    override val order = 2

    override suspend fun deserializeEvents(
        config: EventManagerConfig,
        response: EventsResponse
    ): List<Event<String, ConversationResource>>? {
        return response.body.deserialize<ConversationsEvents>().conversations?.map {
            Event(requireNotNull(Action.map[it.action]), it.id, it.conversation)
        }
    }

    override suspend fun <R> inTransaction(block: suspend () -> R): R = db.inTransaction(block)

    override suspend fun onCreate(config: EventManagerConfig, entities: List<ConversationResource>) {
        localDataSource.upsertConversations(entities.map { it.toConversation(config.userId) })
    }

    override suspend fun onUpdate(config: EventManagerConfig, entities: List<ConversationResource>) {
        localDataSource.upsertConversations(entities.map { it.toConversation(config.userId) })
    }

    override suspend fun onPartial(config: EventManagerConfig, entities: List<ConversationResource>) {
        localDataSource.upsertConversations(entities.map { it.toConversation(config.userId) })
    }

    override suspend fun onDelete(config: EventManagerConfig, keys: List<String>) {
        localDataSource.deleteConversation(config.userId, keys.map { ConversationId(it) })
    }

    override suspend fun onResetAll(config: EventManagerConfig) {
        localDataSource.deleteAllConversations(config.userId)
        repository.getRemoteConversations(config.userId, PageKey())
    }
}
