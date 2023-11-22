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

import ch.protonmail.android.mailcomposer.domain.repository.AttachmentStateRepository
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.AttachmentSyncState
import ch.protonmail.android.mailmessage.domain.model.MessageId
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

class CreateOrUpdateParentAttachmentStates @Inject constructor(
    private val attachmentStateRepository: AttachmentStateRepository
) {

    suspend operator fun invoke(
        userId: UserId,
        messageId: MessageId,
        attachmentIds: List<AttachmentId>
    ) {
        attachmentIds
            .map { attachmentId ->
                attachmentId to attachmentStateRepository.getAttachmentState(userId, messageId, attachmentId)
                    .mapLeft { Timber.e("Failed to load attachmentId: $it") }
                    .getOrNull()
            }
            .filter { it.second == null || it.second?.state == AttachmentSyncState.External }
            .map { it.first }
            .let { filteredAttachmentIds ->
                attachmentStateRepository.createOrUpdateLocalStates(
                    userId,
                    messageId,
                    filteredAttachmentIds,
                    AttachmentSyncState.ExternalUploaded
                ).mapLeft { Timber.e("Failed to create or update local attachment state: $it") }
            }
    }
}
