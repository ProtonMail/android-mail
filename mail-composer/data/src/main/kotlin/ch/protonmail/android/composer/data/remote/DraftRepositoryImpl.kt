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

package ch.protonmail.android.composer.data.remote

import java.util.UUID
import arrow.core.continuations.either
import ch.protonmail.android.mailcomposer.domain.repository.DraftRepository
import ch.protonmail.android.mailcomposer.domain.repository.DraftStateRepository
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import kotlinx.coroutines.flow.first
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

internal class DraftRepositoryImpl @Inject constructor(
    private val messageRepository: MessageRepository,
    private val draftStateRepository: DraftStateRepository,
    private val draftRemoteDataSource: DraftRemoteDataSource
) : DraftRepository {

    override suspend fun sync(userId: UserId, messageId: MessageId) = either {
        val message = messageRepository.observeMessageWithBody(userId, messageId).first().bind()
        val draftState = draftStateRepository.observe(userId, messageId).first().bind()

        if (isMessageLocal(messageId)) {
            draftRemoteDataSource.create(userId, message, draftState.action).bind().also { syncDraft ->
                draftStateRepository.saveCreatedState(userId, messageId, syncDraft.message.messageId)
            }
        } else {
            draftRemoteDataSource.update(userId, message).bind()
        }
    }

    private fun isMessageLocal(messageId: MessageId) = try {
        UUID.fromString(messageId.id)
        true
    } catch (e: IllegalArgumentException) {
        Timber.d("Given messageId ($this) is not a local id (not in UUID format). $e")
        false
    }
}
