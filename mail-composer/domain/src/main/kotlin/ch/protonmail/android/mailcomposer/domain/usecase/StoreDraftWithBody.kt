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

import android.text.SpannableStringBuilder
import androidx.core.text.HtmlCompat
import arrow.core.Either
import arrow.core.raise.either
import ch.protonmail.android.mailcommon.domain.usecase.ResolveUserAddress
import ch.protonmail.android.mailcommon.domain.util.mapFalse
import ch.protonmail.android.mailcomposer.domain.Transactor
import ch.protonmail.android.mailcomposer.domain.model.DraftBody
import ch.protonmail.android.mailcomposer.domain.model.OriginalHtmlQuote
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import ch.protonmail.android.mailmessage.domain.model.MimeType
import ch.protonmail.android.mailmessage.domain.model.Sender
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.UserAddress
import timber.log.Timber
import javax.inject.Inject

class StoreDraftWithBody @Inject constructor(
    private val getLocalDraft: GetLocalDraft,
    private val encryptDraftBody: EncryptDraftBody,
    private val saveDraft: SaveDraft,
    private val resolveUserAddress: ResolveUserAddress,
    private val transactor: Transactor
) {

    suspend operator fun invoke(
        messageId: MessageId,
        draftBody: DraftBody,
        quotedHtmlBody: OriginalHtmlQuote?,
        senderEmail: SenderEmail,
        userId: UserId
    ): Either<StoreDraftWithBodyError, Unit> = either {

        val senderAddress = resolveUserAddress(userId, senderEmail.value)
            .mapLeft { StoreDraftWithBodyError.DraftResolveUserAddressError }
            .bind()

        transactor.performTransaction {
            val draftWithBody = getLocalDraft(userId, messageId, senderEmail)
                .mapLeft { StoreDraftWithBodyError.DraftReadError }
                .bind()

            // If the original mime type is text/html OR we are quoting an HTML body
            // we need to convert the text/plain mime type into text/html.
            val updatedDraftBody = if (draftWithBody.messageBody.mimeType == MimeType.Html || quotedHtmlBody != null) {
                draftBody.convertToHtml()
            } else {
                draftBody
            }

            val encryptedDraftBody = encryptDraftBody(updatedDraftBody.appendQuotedHtml(quotedHtmlBody), senderAddress)
                .mapLeft {
                    Timber.e("Encrypt draft $messageId body to store to local DB failed")
                    StoreDraftWithBodyError.DraftBodyEncryptionError
                }
                .bind()

            val updatedDraft = draftWithBody
                .updateWith(senderAddress, encryptedDraftBody)
                .updateMimeWhenQuotingHtml(quotedHtmlBody)

            saveDraft(updatedDraft, userId)
                .mapFalse {
                    Timber.e("Store draft $messageId body to local DB failed")
                    StoreDraftWithBodyError.DraftSaveError
                }
                .bind()
        }
    }

    private fun DraftBody.appendQuotedHtml(quotedHtmlBody: OriginalHtmlQuote?) =
        quotedHtmlBody?.let { quotedHtml -> DraftBody("${this.value}${quotedHtml.value}") } ?: this

    private fun MessageWithBody.updateMimeWhenQuotingHtml(quotedHtmlBody: OriginalHtmlQuote?): MessageWithBody {
        quotedHtmlBody ?: return this
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

    private fun DraftBody.convertToHtml(): DraftBody {
        val builder = SpannableStringBuilder()
        value.lines().forEachIndexed { index, line ->
            builder.append(line)
            if (index < value.lines().size - 1) {
                builder.append("\n")
            }
        }
        val htmlMessageBody = HtmlCompat.toHtml(builder, HtmlCompat.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
        return DraftBody(value = htmlMessageBody)
    }
}

sealed interface StoreDraftWithBodyError {
    data object DraftBodyEncryptionError : StoreDraftWithBodyError
    data object DraftSaveError : StoreDraftWithBodyError
    data object DraftReadError : StoreDraftWithBodyError
    data object DraftResolveUserAddressError : StoreDraftWithBodyError
}
