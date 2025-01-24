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

import ch.protonmail.android.mailcomposer.domain.repository.DraftRepository
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.repository.DraftStateRepository
import ch.protonmail.android.mailmessage.domain.usecase.DeleteMessages
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

class DiscardDraft @Inject constructor(
    private val findLocalDraft: FindLocalDraft,
    private val deleteMessages: DeleteMessages,
    private val draftRepository: DraftRepository,
    private val draftStateRepository: DraftStateRepository
) {

    suspend operator fun invoke(userId: UserId, messageId: MessageId) {
        findLocalDraft(userId, messageId)?.message?.messageId?.let {
            draftRepository.cancelUploadDraft(it)
            draftStateRepository.deleteDraftState(userId, it)
            deleteMessages(userId, listOf(it), SystemLabelId.Drafts.labelId)
        } ?: Timber.e("No draft for discard found")
    }

}
