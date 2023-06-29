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
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.entity.Sender
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.UserAddress
import javax.inject.Inject

class StoreDraftWithSender @Inject constructor(
    private val createEmptyDraft: CreateEmptyDraft,
    private val saveDraft: SaveDraft,
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(
        messageId: MessageId,
        senderAddress: UserAddress,
        userId: UserId
    ): Either<Error, Unit> = either {
        val draftWithBody = messageRepository.getMessageWithBody(userId, messageId)
            .getOrElse { createEmptyDraft(messageId, userId, senderAddress) }
        val updatedDraft = draftWithBody.copy(
            message = draftWithBody.message.copy(
                sender = Sender(senderAddress.email, senderAddress.displayName.orEmpty())
            )
        )
        saveDraft(updatedDraft, userId)
            .mapFalse { Error.DraftSaveError }
            .bind()

    }

    private fun Boolean.mapFalse(block: () -> Error): Either<Error, Unit> =
        if (this) Unit.right() else block().left()

    sealed interface Error {
        object DraftSaveError : Error
    }
}
