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
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.entity.Sender
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class StoreDraftWithSender @Inject constructor(
    private val getLocalDraft: GetLocalDraft,
    private val saveDraft: SaveDraft,
    private val resolveUserAddress: ResolveUserAddress
) {
    suspend operator fun invoke(
        messageId: MessageId,
        senderEmail: SenderEmail,
        userId: UserId
    ): Either<Error, Unit> = either {
        val draftWithBody = getLocalDraft(userId, messageId, senderEmail)
            .mapLeft { Error.DraftReadError }
            .bind()

        val senderAddress = resolveUserAddress(userId, senderEmail)
            .mapLeft { Error.ResolveUserAddressError }
            .bind()

        val updatedDraft = draftWithBody.copy(
            message = draftWithBody.message.copy(
                sender = Sender(senderAddress.email, senderAddress.displayName.orEmpty()),
                addressId = senderAddress.addressId
            )
        )
        saveDraft(updatedDraft, userId)
            .mapFalse { Error.DraftSaveError }
            .bind()
    }

    private fun Boolean.mapFalse(block: () -> Error): Either<Error, Unit> = if (this) Unit.right() else block().left()

    sealed interface Error {
        object DraftSaveError : Error
        object ResolveUserAddressError : Error
        object DraftReadError : Error
    }
}
