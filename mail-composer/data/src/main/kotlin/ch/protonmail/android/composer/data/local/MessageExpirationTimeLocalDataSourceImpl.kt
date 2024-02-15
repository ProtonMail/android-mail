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

package ch.protonmail.android.composer.data.local

import arrow.core.Either
import ch.protonmail.android.composer.data.local.entity.toEntity
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcomposer.domain.model.MessageExpirationTime
import ch.protonmail.android.mailmessage.domain.model.MessageId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

class MessageExpirationTimeLocalDataSourceImpl @Inject constructor(
    database: DraftStateDatabase
) : MessageExpirationTimeLocalDataSource {

    private val messageExpirationTimeDao = database.messageExpirationTimeDao()

    override suspend fun save(messageExpirationTime: MessageExpirationTime): Either<DataError.Local, Unit> {
        return Either.catch {
            messageExpirationTimeDao.insertOrUpdate(messageExpirationTime.toEntity())
        }.mapLeft {
            Timber.e("Unexpected error writing message expiration time to DB.", it)
            DataError.Local.DbWriteFailed
        }
    }

    override suspend fun observe(userId: UserId, messageId: MessageId): Flow<MessageExpirationTime?> =
        messageExpirationTimeDao.observe(userId, messageId).mapLatest { it?.toDomainModel() }
}
