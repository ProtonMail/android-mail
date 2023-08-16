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
import ch.protonmail.android.mailcomposer.domain.model.DraftBody
import ch.protonmail.android.mailcomposer.domain.model.DraftFields
import ch.protonmail.android.mailcomposer.domain.model.RecipientsBcc
import ch.protonmail.android.mailcomposer.domain.model.RecipientsCc
import ch.protonmail.android.mailcomposer.domain.model.RecipientsTo
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.model.Subject
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.usecase.GetDecryptedMessageBody
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

class GetDecryptedDraftFields @Inject constructor(
    private val messageRepository: MessageRepository,
    private val getDecryptedMessageBody: GetDecryptedMessageBody
) {

    suspend operator fun invoke(userId: UserId, messageId: MessageId): Either<DataError, DraftFields> {
        Timber.d("Get decrypted draft data for $userId $messageId")
        val message = getMessageWithBody(userId, messageId)?.message ?: return DataError.Local.NoDataCached.left()

        val decryptedMessageBody = getDecryptedMessageBody(userId, messageId).getOrElse {
            return DataError.Local.DecryptionError.left()
        }

        return DraftFields(
            SenderEmail(message.sender.address),
            Subject(message.subject),
            DraftBody(decryptedMessageBody.value),
            RecipientsTo(message.toList),
            RecipientsCc(message.ccList),
            RecipientsBcc(message.bccList)
        ).right()
    }

    private suspend fun getMessageWithBody(userId: UserId, messageId: MessageId) =
        messageRepository.fetchAndStoreMessageWithBody(userId, messageId).getOrElse {
            Timber.d("Couldn't fetch message with body: $it. Trying to get message locally...")
            messageRepository.getLocalMessageWithBody(userId, messageId)
        }
}
