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

import java.util.UUID
import ch.protonmail.android.mailconversation.data.local.ConversationDatabase
import ch.protonmail.android.mailconversation.data.local.UnreadConversationsCountLocalDataSource
import ch.protonmail.android.mailconversation.data.remote.resource.UnreadConversationCountResource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.proton.core.eventmanager.domain.EventListener
import me.proton.core.eventmanager.domain.EventManagerConfig
import me.proton.core.eventmanager.domain.entity.Action
import me.proton.core.eventmanager.domain.entity.Event
import me.proton.core.eventmanager.domain.entity.EventsResponse
import me.proton.core.util.kotlin.deserialize
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class UnreadConversationsCountEventListener @Inject constructor(
    private val db: ConversationDatabase,
    private val localDataSource: UnreadConversationsCountLocalDataSource
) : EventListener<String, UnreadConversationCountResource>() {

    override val type = Type.Core
    override val order = 1

    override suspend fun deserializeEvents(
        config: EventManagerConfig,
        response: EventsResponse
    ): List<Event<String, UnreadConversationCountResource>>? {
        Timber.d("Unread count event: Deserializing $response for conversations")
        return response.body.deserialize<UnreadConversationEvents>().events?.map {
            val eventId = UUID.randomUUID().toString()
            Event(Action.Update, eventId, it)
        }
    }

    override suspend fun <R> inTransaction(block: suspend () -> R): R = db.inTransaction(block)

    override suspend fun onCreate(config: EventManagerConfig, entities: List<UnreadConversationCountResource>) {
        localDataSource.saveConversationCounters(entities.map { it.toUnreadCountConversationsEntity(config.userId) })
    }

    override suspend fun onUpdate(config: EventManagerConfig, entities: List<UnreadConversationCountResource>) {
        localDataSource.saveConversationCounters(entities.map { it.toUnreadCountConversationsEntity(config.userId) })
    }

    override suspend fun onPartial(config: EventManagerConfig, entities: List<UnreadConversationCountResource>) {
        localDataSource.saveConversationCounters(entities.map { it.toUnreadCountConversationsEntity(config.userId) })
    }

    override suspend fun onDelete(config: EventManagerConfig, keys: List<String>) { }

    override suspend fun onResetAll(config: EventManagerConfig) { }
}

@Serializable
data class UnreadConversationEvents(
    @SerialName("ConversationCounts")
    val events: List<UnreadConversationCountResource>? = null
)
