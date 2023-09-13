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

package ch.protonmail.android.mailcomposer.domain.usecase

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcomposer.domain.model.MessageWithDecryptedBody
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.usecase.GetDecryptedMessageBody
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

class GetLocalMessageDecrypted @Inject constructor(
    private val messageRepository: MessageRepository,
    private val getDecryptedMessageBody: GetDecryptedMessageBody
) {

    suspend operator fun invoke(userId: UserId, messageId: MessageId): Either<DataError, MessageWithDecryptedBody> {
        Timber.d("Get decrypted local message data for $userId $messageId")
        val messageWithBody = messageRepository.getLocalMessageWithBody(userId, messageId)
        if (messageWithBody == null) {
            Timber.e("Error getting local message decrypted")
            return DataError.Local.NoDataCached.left()
        }

        val decryptedMessageBody = getDecryptedMessageBody(userId, messageId).getOrElse {
            return DataError.Local.DecryptionError.left()
        }

        return MessageWithDecryptedBody(messageWithBody, decryptedMessageBody).right()
    }

}
