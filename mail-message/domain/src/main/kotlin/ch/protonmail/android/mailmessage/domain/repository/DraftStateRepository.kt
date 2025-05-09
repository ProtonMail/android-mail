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

package ch.protonmail.android.mailmessage.domain.repository

import arrow.core.Either
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.model.DraftState
import ch.protonmail.android.mailmessage.domain.model.DraftSyncState
import ch.protonmail.android.mailmessage.domain.model.SendingError
import ch.protonmail.android.mailmessage.domain.model.MessageId
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId

interface DraftStateRepository {

    fun observe(userId: UserId, messageId: MessageId): Flow<Either<DataError, DraftState>>

    fun observeAll(userId: UserId): Flow<List<DraftState>>


    /**
     * Reads an existing [DraftState], updates it by setting its state to [DraftSyncState.Syncronized]
     * and adding / updating the messageId assigned by the API as [apiMessageId].
     * Errors out if a [DraftState] for the given ID does not exist.
     */
    suspend fun updateApiMessageIdAndSetSyncedState(
        userId: UserId,
        messageId: MessageId,
        apiMessageId: MessageId
    ): Either<DataError, Unit>

    /**
     * Creates a new [DraftState] with a [DraftSyncState.Local].
     * If existing, updates the state to [DraftSyncState.Local]
     */
    suspend fun createOrUpdateLocalState(
        userId: UserId,
        messageId: MessageId,
        action: DraftAction
    ): Either<DataError, Unit>

    /**
     * Updates the [syncState] value in [DraftState].
     */
    suspend fun updateDraftSyncState(
        userId: UserId,
        messageId: MessageId,
        syncState: DraftSyncState,
        sendingError: SendingError?
    ): Either<DataError, Unit>

    /**
     * Updates the [syncState] value in [DraftState].
     */
    suspend fun updateConfirmDraftSendingStatus(
        userId: UserId,
        messageId: MessageId,
        sendingStatusConfirmed: Boolean
    ): Either<DataError, Unit>


    /**
     * Updates the [sendingError] value in [DraftState].
     */
    suspend fun updateSendingError(
        userId: UserId,
        messageId: MessageId,
        sendingError: SendingError?
    ): Either<DataError, Unit>

    /**
     * Deletes [DraftState] from local DB.
     */
    suspend fun deleteDraftState(userId: UserId, messageId: MessageId): Either<DataError, Unit>
}
