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

package ch.protonmail.android.composer.data.usecase

import arrow.core.Either
import arrow.core.continuations.either
import ch.protonmail.android.composer.data.remote.DraftRemoteDataSource
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcomposer.domain.model.DraftState
import ch.protonmail.android.mailcomposer.domain.repository.DraftStateRepository
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import kotlinx.coroutines.flow.first
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

internal class SyncDraft @Inject constructor(
    private val messageRepository: MessageRepository,
    private val draftStateRepository: DraftStateRepository,
    private val draftRemoteDataSource: DraftRemoteDataSource
) {

    suspend operator fun invoke(userId: UserId, messageId: MessageId): Either<DataError, Unit> = either {
        val message = messageRepository.observeMessageWithBody(userId, messageId).first().onLeft {
            Timber.w("Sync draft failure $messageId: No message found")
        }.bind()
        val draftState = draftStateRepository.observe(userId, messageId).first().onLeft {
            Timber.w("Sync draft failure $messageId: No draft state found")
        }.bind()

        if (draftState.isLocal()) {
            val syncDraft = draftRemoteDataSource.create(userId, message, draftState.action).onLeft {
                Timber.w("Sync draft failure $messageId: Create API call error $it")
            }.bind()

            val remoteDraftId = syncDraft.message.messageId
            messageRepository.updateDraftMessageId(userId, messageId, remoteDraftId)
            draftStateRepository.saveSynchedState(userId, messageId, remoteDraftId)
        } else {
            draftRemoteDataSource.update(userId, message).onLeft {
                Timber.w("Sync draft failure $messageId: Update API call error $it")
            }.bind()
            draftStateRepository.saveSynchedState(userId, messageId, messageId)
        }
    }

    private fun DraftState.isLocal() = this.apiMessageId == null
}
