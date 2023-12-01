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
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailmessage.data.local.entity.toDraftStateEntity
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailmessage.domain.model.DraftState
import ch.protonmail.android.mailmessage.domain.model.MessageId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

class DraftStateLocalDataSourceImpl @Inject constructor(
    draftStateDatabase: DraftStateDatabase
) : DraftStateLocalDataSource {

    private val draftStateDao = draftStateDatabase.draftStateDao()

    override fun observe(userId: UserId, messageId: MessageId): Flow<Either<DataError, DraftState>> =
        draftStateDao.observeDraftState(userId, messageId).map {
            when (it) {
                null -> DataError.Local.NoDataCached.left()
                else -> it.toDraftState().right()
            }
        }

    override fun observeAll(userId: UserId): Flow<List<DraftState>> = draftStateDao.observeAllDraftsState(userId).map {
        it.map { it.toDraftState() }
    }

    override suspend fun save(state: DraftState): Either<DataError, Unit> {
        return Either.catch {
            draftStateDao.insertOrUpdate(state.toDraftStateEntity())
        }.mapLeft {
            Timber.e("Unexpected error writing draft state to DB. $it")
            DataError.Local.Unknown
        }
    }

    override suspend fun delete(state: DraftState) {
        draftStateDao.delete(state.toDraftStateEntity())
    }

}
