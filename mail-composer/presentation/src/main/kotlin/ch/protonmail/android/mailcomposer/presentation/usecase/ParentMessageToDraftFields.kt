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

package ch.protonmail.android.mailcomposer.presentation.usecase

import android.content.Context
import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.usecase.ObserveUserAddresses
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.usecase.FormatExtendedTime
import ch.protonmail.android.mailcomposer.domain.model.AddressSignature
import ch.protonmail.android.mailcomposer.domain.model.DraftAction
import ch.protonmail.android.mailcomposer.domain.model.DraftBody
import ch.protonmail.android.mailcomposer.domain.model.DraftFields
import ch.protonmail.android.mailcomposer.domain.model.MessageWithDecryptedBody
import ch.protonmail.android.mailcomposer.domain.model.OriginalHtmlQuote
import ch.protonmail.android.mailcomposer.domain.model.RecipientsBcc
import ch.protonmail.android.mailcomposer.domain.model.RecipientsCc
import ch.protonmail.android.mailcomposer.domain.model.RecipientsTo
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.model.Subject
import ch.protonmail.android.mailcomposer.domain.usecase.GetAddressSignature
import ch.protonmail.android.mailcomposer.presentation.R
import ch.protonmail.android.mailmessage.domain.model.DecryptedMessageBody
import ch.protonmail.android.mailmessage.domain.model.Message
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import ch.protonmail.android.mailmessage.domain.model.MimeType
import ch.protonmail.android.mailmessage.domain.model.Recipient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.UserAddress
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class ParentMessageToDraftFields @Inject constructor(
    @ApplicationContext private val context: Context,
    private val observeUserAddresses: ObserveUserAddresses,
    private val formatExtendedTime: FormatExtendedTime,
    private val getAddressSignature: GetAddressSignature,
    private val getMobileFooter: GetMobileFooter
) {

    suspend operator fun invoke(
        userId: UserId,
        messageWithDecryptedBody: MessageWithDecryptedBody,
        action: DraftAction
    ): Either<DataError.Local, DraftFields> {
        val message = messageWithDecryptedBody.messageWithBody.message
        val decryptedBody = messageWithDecryptedBody.decryptedMessageBody
        val userAddresses = observeUserAddresses(userId).firstOrNull() ?: return DataError.Local.NoDataCached.left()
        val sender = getSenderEmail(userAddresses, message)
        val senderAddressSignature = getAddressSignature(userId, sender).getOrElse { AddressSignature.BlankSignature }
        val mobileFooter = getMobileFooter(userId).getOrNull() ?: return DataError.Local.Unknown.left()

        return DraftFields(
            sender,
            Subject("${subjectPrefixForAction(action)} ${message.subject}"),
            buildQuotedPlainTextBody(message, decryptedBody, senderAddressSignature, mobileFooter),
            RecipientsTo(recipientsForAction(action, messageWithDecryptedBody.messageWithBody, sender)),
            RecipientsCc(ccRecipientsForAction(action, message)),
            RecipientsBcc(emptyList()),
            buildQuotedHtmlBody(message, decryptedBody)
        ).right()
    }

    private fun buildQuotedPlainTextBody(
        message: Message,
        decryptedBody: DecryptedMessageBody,
        senderAddressSignature: AddressSignature,
        mobileFooter: String
    ): DraftBody {
        if (decryptedBody.mimeType != MimeType.PlainText &&
            (senderAddressSignature.plaintext.isNotBlank() || mobileFooter.isNotBlank())
        ) {
            // HTML quote is fully created elsewhere, but we still need to inject signature
            //  and mobile footer into editable body
            return DraftBody(senderAddressSignature.plaintext + mobileFooter)
        } else if (decryptedBody.mimeType != MimeType.PlainText) {
            return DraftBody("")
        }

        Timber.d("Decrypted body ${decryptedBody.value} \n splitted: ${decryptedBody.value.split("\n")}")
        val bodyQuoted = decryptedBody.value
            .split("\n")
            .joinToString(separator = PlainTextNewLine) { "$PlainTextQuotePrefix $it" }

        val raw = StringBuilder()
            .append(senderAddressSignature.plaintext.ifBlank { "" })
            .append(mobileFooter.ifBlank { "" })
            .append(PlainTextNewLine)
            .append(PlainTextNewLine)
            .append(PlainTextNewLine)
            .append(buildOriginalMessageQuote())
            .append(PlainTextNewLine)
            .append(buildSenderQuote(message))
            .append(PlainTextNewLine)
            .append(PlainTextNewLine)
            .append(bodyQuoted)
            .toString()
        return DraftBody(raw)
    }

    private fun buildQuotedHtmlBody(message: Message, decryptedBody: DecryptedMessageBody): OriginalHtmlQuote? {
        if (decryptedBody.mimeType == MimeType.PlainText) {
            return null
        }

        val raw = StringBuilder()
            .append(ProtonMailQuote)
            .append(LineBreak)
            .append(LineBreak)
            .append(buildOriginalMessageQuote())
            .append(LineBreak)
            .append(buildSenderQuote(message))
            .append(LineBreak)
            .append(ProtonMailBlockquote)
            .append(decryptedBody.value)
            .append(CloseProtonMailBlockquote)
            .append(CloseProtonMailQuote)
            .toString()
        return OriginalHtmlQuote(raw)
    }

    private fun buildOriginalMessageQuote() =
        "-------- ${context.getString(R.string.composer_original_message_quote)} --------"

    private fun buildSenderQuote(message: Message): String {
        val formattedTime = formatExtendedTime(message.time.seconds) as? TextUiModel.Text
        return context.getString(R.string.composer_sender_quote).format(
            formattedTime?.value, message.sender.name, message.sender.address
        )
    }

    private fun getSenderEmail(addresses: List<UserAddress>, message: Message): SenderEmail {
        val address = addresses.firstOrNull { it.addressId == message.addressId }?.email
            ?: addresses.minBy { it.order }.email
        return SenderEmail(address)
    }

    private fun recipientsForAction(
        action: DraftAction,
        messageWithBody: MessageWithBody,
        senderEmail: SenderEmail
    ): List<Recipient> {
        val allRecipients = when (action) {
            is DraftAction.Compose,
            is DraftAction.Forward -> emptyList()
            is DraftAction.Reply -> listOf(messageWithBody.messageBody.replyTo)
            is DraftAction.ReplyAll -> listOf(messageWithBody.messageBody.replyTo) + messageWithBody.message.toList
        }
        return allRecipients.filterNot { it.address == senderEmail.value }
    }

    private fun ccRecipientsForAction(action: DraftAction, message: Message) = when (action) {
        is DraftAction.Compose,
        is DraftAction.Forward,
        is DraftAction.Reply -> emptyList()
        is DraftAction.ReplyAll -> message.ccList
    }

    private fun subjectPrefixForAction(action: DraftAction) = when (action) {
        is DraftAction.Compose -> ""
        is DraftAction.Forward -> "Fw:"
        is DraftAction.Reply,
        is DraftAction.ReplyAll -> "Re:"
    }

    companion object {
        const val ProtonMailQuote = "<div class=\"protonmail_quote\">"
        const val ProtonMailBlockquote = "<blockquote class=\"protonmail_quote\">"
        const val CloseProtonMailQuote = "</div>"
        const val CloseProtonMailBlockquote = "</blockquote>"
        const val LineBreak = "<br>"
        const val PlainTextNewLine = "\n"
        const val PlainTextQuotePrefix = "> "
    }
}
