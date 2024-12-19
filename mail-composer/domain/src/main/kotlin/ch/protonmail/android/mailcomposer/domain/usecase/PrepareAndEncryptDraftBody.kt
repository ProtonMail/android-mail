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
import arrow.core.raise.either
import ch.protonmail.android.mailcommon.domain.usecase.ResolveUserAddress
import ch.protonmail.android.mailcomposer.domain.model.DraftBody
import ch.protonmail.android.mailcomposer.domain.model.OriginalHtmlQuote
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import ch.protonmail.android.mailmessage.domain.model.MimeType
import ch.protonmail.android.mailmessage.domain.model.Sender
import ch.protonmail.android.mailmessage.domain.usecase.ConvertPlainTextIntoHtml
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.UserAddress
import timber.log.Timber
import javax.inject.Inject

class PrepareAndEncryptDraftBody @Inject constructor(
    private val getLocalDraft: GetLocalDraft,
    private val resolveUserAddress: ResolveUserAddress,
    private val convertPlainTextIntoHtml: ConvertPlainTextIntoHtml,
    private val encryptDraftBody: EncryptDraftBody
) {

    suspend operator fun invoke(
        userId: UserId,
        messageId: MessageId,
        draftBody: DraftBody,
        quotedHtmlBody: OriginalHtmlQuote?,
        senderEmail: SenderEmail
    ): Either<PrepareDraftBodyError, MessageWithBody> = either {

        val senderAddress = resolveUserAddress(userId, senderEmail.value)
            .mapLeft { PrepareDraftBodyError.DraftResolveUserAddressError }
            .bind()

        val draftWithBody = getLocalDraft(userId, messageId, senderEmail)
            .mapLeft { PrepareDraftBodyError.DraftReadError }
            .bind()

        val isOriginalMimeHtml = draftWithBody.messageBody.mimeType == MimeType.Html
        val hasQuotedBody = quotedHtmlBody != null
        val updatedDraftBody = if (isOriginalMimeHtml || hasQuotedBody) {
            draftBody.convertToHtml()
        } else {
            draftBody
        }

        val encryptedDraftBody = encryptDraftBody(updatedDraftBody.appendQuotedHtml(quotedHtmlBody), senderAddress)
            .mapLeft {
                Timber.e("Encrypt draft $messageId body to store to local DB failed")
                PrepareDraftBodyError.DraftBodyEncryptionError
            }
            .bind()

        val updatedDraft = draftWithBody
            .updateWith(senderAddress, encryptedDraftBody)
            .updateMimeWhenQuotingHtml(quotedHtmlBody)

        updatedDraft
    }

    private fun DraftBody.convertToHtml() = DraftBody(
        value = convertPlainTextIntoHtml(this.value, autoTransformLinks = false)
    )

    private fun DraftBody.appendQuotedHtml(quotedHtmlBody: OriginalHtmlQuote?) =
        quotedHtmlBody?.let { quotedHtml -> DraftBody("${this.value}${quotedHtml.value}") } ?: this

    private fun MessageWithBody.updateMimeWhenQuotingHtml(quotedHtmlBody: OriginalHtmlQuote?): MessageWithBody {
        if (quotedHtmlBody == null) {
            return this
        }

        return this.copy(messageBody = this.messageBody.copy(mimeType = MimeType.Html))
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
}

sealed interface PrepareDraftBodyError {
    data object DraftBodyEncryptionError : PrepareDraftBodyError
    data object DraftReadError : PrepareDraftBodyError
    data object DraftResolveUserAddressError : PrepareDraftBodyError
}
