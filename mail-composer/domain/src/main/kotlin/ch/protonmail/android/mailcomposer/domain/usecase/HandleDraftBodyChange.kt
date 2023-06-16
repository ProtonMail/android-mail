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
import arrow.core.continuations.either
import arrow.core.getOrElse
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcomposer.domain.model.DraftBody
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import me.proton.core.user.domain.entity.UserAddress
import javax.inject.Inject

class HandleDraftBodyChange @Inject constructor(
    private val createEmptyDraft: CreateEmptyDraft,
    private val encryptDraftBody: EncryptDraftBody,
    private val saveDraft: SaveDraft,
    private val messageRepository: MessageRepository,
    private val observePrimaryUserId: ObservePrimaryUserId
) {

    suspend operator fun invoke(
        messageId: MessageId,
        draftBody: DraftBody,
        senderAddress: UserAddress
    ): Either<DraftBodyEncryptionFailure, Unit> = either {
        val userId = observePrimaryUserId().filterNotNull().first()
        val draftWithBody = messageRepository.getMessageWithBody(userId, messageId)
            .getOrElse { createEmptyDraft(messageId, userId, senderAddress) }
        val encryptedDraftBody = encryptDraftBody(draftBody, senderAddress).bind()
        val updatedDraft = draftWithBody.copy(
            messageBody = draftWithBody.messageBody.copy(
                body = encryptedDraftBody.value
            )
        )
        saveDraft(updatedDraft, userId).right()
    }
}
