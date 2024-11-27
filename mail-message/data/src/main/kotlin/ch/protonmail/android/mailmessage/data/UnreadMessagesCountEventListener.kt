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

import java.util.UUID
import ch.protonmail.android.mailmessage.data.local.MessageDatabase
import ch.protonmail.android.mailmessage.data.local.UnreadMessagesCountLocalDataSource
import ch.protonmail.android.mailmessage.data.remote.UnreadMessagesCountRemoteDataSource
import ch.protonmail.android.mailmessage.data.remote.resource.UnreadMessageCountResource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.proton.core.eventmanager.domain.EventListener
import me.proton.core.eventmanager.domain.EventManagerConfig
import me.proton.core.eventmanager.domain.entity.Action
import me.proton.core.eventmanager.domain.entity.Event
import me.proton.core.eventmanager.domain.entity.EventsResponse
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.util.kotlin.deserialize
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class UnreadMessagesCountEventListener @Inject constructor(
    private val db: MessageDatabase,
    private val localDataSource: UnreadMessagesCountLocalDataSource,
    private val remoteDataSource: UnreadMessagesCountRemoteDataSource
) : EventListener<String, UnreadMessageCountResource>() {

    override val type = Type.Core
    override val order = 1

    override suspend fun deserializeEvents(
        config: EventManagerConfig,
        response: EventsResponse
    ): List<Event<String, UnreadMessageCountResource>>? {
        return response.body.deserialize<UnreadMessageEvents>().events?.map {
            val eventId = UUID.randomUUID().toString()
            Event(Action.Update, eventId, it)
        }
    }

    override suspend fun <R> inTransaction(block: suspend () -> R): R = db.inTransaction(block)

    override suspend fun onCreate(config: EventManagerConfig, entities: List<UnreadMessageCountResource>) {
        localDataSource.saveMessageCounters(entities.map { it.toUnreadCountMessagesEntity(config.userId) })
    }

    override suspend fun onUpdate(config: EventManagerConfig, entities: List<UnreadMessageCountResource>) {
        localDataSource.saveMessageCounters(entities.map { it.toUnreadCountMessagesEntity(config.userId) })
    }

    override suspend fun onPartial(config: EventManagerConfig, entities: List<UnreadMessageCountResource>) {
        localDataSource.saveMessageCounters(entities.map { it.toUnreadCountMessagesEntity(config.userId) })
    }

    override suspend fun onDelete(config: EventManagerConfig, keys: List<String>) {
        localDataSource.delete(config.userId, keys.map { LabelId(it) })
    }

    override suspend fun onResetAll(config: EventManagerConfig) {
        localDataSource.deleteAll(config.userId)
        remoteDataSource.getMessageCounters(config.userId)
    }
}

@Serializable
data class UnreadMessageEvents(
    @SerialName("MessageCounts")
    val events: List<UnreadMessageCountResource>? = null
)
