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
import arrow.core.raise.either
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcomposer.domain.Transactor
import ch.protonmail.android.mailcomposer.domain.model.MessageExpirationTime
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.repository.MessageExpirationTimeRepository
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration

class SaveMessageExpirationTime @Inject constructor(
    private val getLocalDraft: GetLocalDraft,
    private val messageExpirationTimeRepository: MessageExpirationTimeRepository,
    private val messageRepository: MessageRepository,
    private val saveDraft: SaveDraft,
    private val transactor: Transactor
) {

    suspend operator fun invoke(
        userId: UserId,
        messageId: MessageId,
        senderEmail: SenderEmail,
        expiresIn: Duration
    ): Either<DataError.Local, Unit> = transactor.performTransaction {
        either {
            val draft = getLocalDraft(userId, messageId, senderEmail)
                .mapLeft { DataError.Local.NoDataCached }
                .bind()
            // Verify that draft exists in db, if not create it
            val messageWithBody = messageRepository.getLocalMessageWithBody(userId, draft.message.messageId)
            if (messageWithBody == null) {
                val success = saveDraft(draft, userId)
                if (!success) {
                    Timber.d("Failed to save draft")
                    raise(DataError.Local.Unknown)
                }
            }

            val messageExpirationTime = MessageExpirationTime(userId, draft.message.messageId, expiresIn)
            messageExpirationTimeRepository.saveMessageExpirationTime(messageExpirationTime).bind()
        }
    }
}
