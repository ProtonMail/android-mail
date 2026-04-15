/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailmessage.domain.usecase

import arrow.core.Either
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailmessage.domain.model.Message
import ch.protonmail.android.mailmessage.domain.model.RemoteMessageId
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi

class GetMessageByRemoteId @Inject constructor(
    private val messageRepository: MessageRepository
) {

    @OptIn(ExperimentalAtomicApi::class)
    suspend operator fun invoke(userId: UserId, messageId: RemoteMessageId): Either<DataError, Message> {
        val attempts = AtomicInt(0)

        while (attempts.addAndFetch(1) < MAX_RETRY_ATTEMPTS) {
            val message = getByRemoteId(userId, messageId)
            if (message.isRight()) {
                Timber.tag("GetMessageByRemoteId").d("getting message $messageId succeeded (${attempts.load()})")
                return message
            }
            Timber.tag("GetMessageByRemoteId").d("Error: getting message $messageId failed (${attempts.load()})")
        }

        val result = getByRemoteId(userId, messageId)
        Timber.tag("GetMessageByRemoteId").d("getting message $messageId last attempt result: $result")
        return result
    }

    private suspend fun getByRemoteId(userId: UserId, messageId: RemoteMessageId): Either<DataError, Message> =
        messageRepository.getMessageByRemoteId(userId, messageId)
}

private const val MAX_RETRY_ATTEMPTS = 3
