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

import ch.protonmail.android.mailmessage.domain.model.DraftSyncState
import ch.protonmail.android.mailmessage.domain.repository.DraftStateRepository
import kotlinx.coroutines.flow.firstOrNull
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class ResetSendingMessagesStatus @Inject constructor(
    private val draftStateRepository: DraftStateRepository,
    private val resetDraftStateError: ResetDraftStateError,
    private val confirmSendingMessageStatus: ConfirmSendingMessageStatus
) {

    suspend operator fun invoke(userId: UserId) {
        draftStateRepository.observeAll(userId).firstOrNull()?.map {
            if (it.state == DraftSyncState.ErrorSending || it.state == DraftSyncState.ErrorUploadAttachments) {
                resetDraftStateError(it.userId, it.messageId)
                confirmSendingMessageStatus(it.userId, it.messageId)
            } else if (it.state == DraftSyncState.Sent) {
                confirmSendingMessageStatus(it.userId, it.messageId)
            }
        }
    }
}
