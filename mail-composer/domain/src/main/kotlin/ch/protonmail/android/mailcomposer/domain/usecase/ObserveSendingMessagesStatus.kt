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
import ch.protonmail.android.mailcomposer.domain.model.MessageSendingStatus
import ch.protonmail.android.mailmessage.domain.repository.DraftStateRepository
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class ObserveSendingMessagesStatus @Inject constructor(
    private val draftStateRepository: DraftStateRepository
) {

    operator fun invoke(userId: UserId) = draftStateRepository.observeAll(userId).map { draftStates ->
        val unconfirmedDraftStates = draftStates.filter { !it.sendingStatusConfirmed }

        val erroredState = unconfirmedDraftStates.firstOrNull { it.state == DraftSyncState.ErrorSending }
        if (erroredState != null) {
            return@map MessageSendingStatus.SendMessageError(erroredState.sendingError)
        }

        if (unconfirmedDraftStates.any { it.state == DraftSyncState.ErrorUploadAttachments }) {
            return@map MessageSendingStatus.UploadAttachmentsError
        }

        if (unconfirmedDraftStates.any { it.state == DraftSyncState.Sent }) {
            return@map MessageSendingStatus.MessageSent
        }

        MessageSendingStatus.None
    }.distinctUntilChanged()

}
