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
import ch.protonmail.android.mailcomposer.domain.model.DraftBody
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.entity.MessageWithBody
import ch.protonmail.android.mailmessage.domain.entity.Sender
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.UserAddress
import timber.log.Timber
import javax.inject.Inject

class StoreDraftWithBody @Inject constructor(
    private val getLocalDraft: GetLocalDraft,
    private val encryptDraftBody: EncryptDraftBody,
    private val saveDraft: SaveDraft,
    private val resolveUserAddress: ResolveUserAddress
) {

    suspend operator fun invoke(
        messageId: MessageId,
        draftBody: DraftBody,
        senderEmail: SenderEmail,
        userId: UserId
    ): Either<StoreDraftWithBodyError, Unit> = either {
        val draftWithBody = getLocalDraft(userId, messageId, senderEmail)
            .mapLeft { StoreDraftWithBodyError.DraftReadError }
            .bind()

        val senderAddress = resolveUserAddress(userId, senderEmail)
            .mapLeft { StoreDraftWithBodyError.DraftResolveUserAddressError }
            .bind()

        val encryptedDraftBody = encryptDraftBody(draftBody, senderAddress)
            .mapLeft {
                Timber.e("Encrypt draft $messageId body to store to local DB failed")
                StoreDraftWithBodyError.DraftBodyEncryptionError
            }
            .bind()

        val updatedDraft = draftWithBody.updateWith(senderAddress, encryptedDraftBody)
        saveDraft(updatedDraft, userId)
            .mapFalse {
                Timber.e("Store draft $messageId body to local DB failed")
                StoreDraftWithBodyError.DraftSaveError
            }
            .bind()
    }

    private fun MessageWithBody.updateWith(senderAddress: UserAddress, encryptedDraftBody: DraftBody) = this.copy(
        message = this.message.copy(
            sender = Sender(senderAddress.email, senderAddress.displayName.orEmpty()),
            addressId = senderAddress.addressId
        ),
        messageBody = this.messageBody.copy(
            body = encryptedDraftBody.value
        )
    )

    private fun Boolean.mapFalse(block: () -> StoreDraftWithBodyError): Either<StoreDraftWithBodyError, Unit> =
        if (this) Unit.right() else block().left()
}

sealed interface StoreDraftWithBodyError {
    object DraftBodyEncryptionError : StoreDraftWithBodyError
    object DraftSaveError : StoreDraftWithBodyError
    object DraftReadError : StoreDraftWithBodyError
    object DraftResolveUserAddressError : StoreDraftWithBodyError
}
