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

import java.io.File
import arrow.core.Either
import arrow.core.raise.either
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcomposer.domain.repository.DraftStateRepository
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.repository.AttachmentRepository
import kotlinx.coroutines.flow.first
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

class ReadAttachmentsFromStorage @Inject constructor(
    private val attachmentRepository: AttachmentRepository,
    private val draftStateRepository: DraftStateRepository
) {

    suspend operator fun invoke(
        userId: UserId,
        messageId: MessageId,
        attachmentIds: List<AttachmentId>
    ): Either<DataError, Map<AttachmentId, File>> = either {
        val apiMessageId = draftStateRepository.observe(userId, messageId).first().onLeft {
            Timber.e("No draft state found for $messageId when reading attachments from storage")
        }.getOrNull()?.apiMessageId ?: shift(DataError.Local.NoDataCached)

        attachmentIds.associateWith {
            attachmentRepository.readFileFromStorage(userId, apiMessageId, it).bind()
        }
    }
}
