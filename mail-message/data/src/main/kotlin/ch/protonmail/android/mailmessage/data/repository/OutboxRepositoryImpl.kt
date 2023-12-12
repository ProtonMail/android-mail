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

package ch.protonmail.android.mailmessage.data.repository

import ch.protonmail.android.mailmessage.data.local.MessageDatabase
import ch.protonmail.android.mailmessage.domain.model.DraftState
import ch.protonmail.android.mailmessage.domain.model.OutboxStates
import ch.protonmail.android.mailmessage.domain.repository.OutboxRepository
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OutboxRepositoryImpl @Inject constructor(private val db: MessageDatabase) : OutboxRepository {

    private val outboxDao = db.outboxDao()

    override suspend fun observeAll(userId: UserId): Flow<List<DraftState>> = outboxDao
        .getMessagesInOutbox(userId, OutboxStates.stateList)
}
