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

package ch.protonmail.android.maildetail.domain.usecase

import arrow.core.Either
import arrow.core.raise.either
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.usecase.GetDecryptedMessageBody
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

class ReportPhishingMessage @Inject constructor(
    private val repository: MessageRepository,
    private val moveMessage: MoveMessage,
    private val decryptedMessageBody: GetDecryptedMessageBody
) {

    suspend operator fun invoke(userId: UserId, messageId: MessageId): Either<ReportPhishingError, Unit> = either {
        val decryptedMessageBody = decryptedMessageBody(userId, messageId)
            .mapLeft { ReportPhishingError.FailedToGetDecryptedMessage }
            .bind()

        repository.reportPhishing(userId, decryptedMessageBody)
            .mapLeft {
                Timber.e("Failed to report phishing message: $it")
                ReportPhishingError.FailedToReportPhishing
            }
            .bind()

        moveMessage(userId, messageId, SystemLabelId.Spam.labelId).mapLeft {
            Timber.e("Failed to move phishing message to spam folder: $it")
            ReportPhishingError.FailedToMoveToSpam
        }.bind()
    }

    sealed interface ReportPhishingError {
        object FailedToGetDecryptedMessage : ReportPhishingError
        object FailedToReportPhishing : ReportPhishingError
        object FailedToMoveToSpam : ReportPhishingError
    }
}
