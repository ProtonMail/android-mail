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
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.usecase.FormatExtendedTime
import ch.protonmail.android.mailcomposer.domain.model.DraftAction
import ch.protonmail.android.mailcomposer.domain.model.DraftBody
import ch.protonmail.android.mailcomposer.domain.model.DraftFields
import ch.protonmail.android.mailcomposer.domain.model.MessageWithDecryptedBody
import ch.protonmail.android.mailcomposer.domain.model.QuotedHtmlBody
import ch.protonmail.android.mailcomposer.domain.model.RecipientsBcc
import ch.protonmail.android.mailcomposer.domain.model.RecipientsCc
import ch.protonmail.android.mailcomposer.domain.model.RecipientsTo
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.model.Subject
import ch.protonmail.android.mailcomposer.domain.usecase.ObserveUserAddresses
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
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class ParentMessageToDraftFields @Inject constructor(
    @ApplicationContext private val context: Context,
    private val observeUserAddresses: ObserveUserAddresses,
    private val formatExtendedTime: FormatExtendedTime
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

        return DraftFields(
            sender,
            Subject("${subjectPrefixForAction(action)} ${message.subject}"),
            buildQuotedPlainTextBody(message, decryptedBody),
            RecipientsTo(recipientsForAction(action, messageWithDecryptedBody.messageWithBody, sender)),
            RecipientsCc(ccRecipientsForAction(action, message)),
            RecipientsBcc(emptyList()),
            buildQuotedHtmlBody(message, decryptedBody)
        ).right()
    }

    private fun buildQuotedPlainTextBody(message: Message, decryptedBody: DecryptedMessageBody): DraftBody {
        if (decryptedBody.mimeType != MimeType.PlainText) {
            return DraftBody("")
        }

        val raw = StringBuilder()
            .append(context.getString(R.string.composer_original_message_quote))
            .append(buildSenderQuote(message))
            .append(decryptedBody.value)
            .toString()
        return DraftBody(raw)
    }

    private fun buildQuotedHtmlBody(message: Message, decryptedBody: DecryptedMessageBody): QuotedHtmlBody? {
        if (decryptedBody.mimeType == MimeType.PlainText) {
            return null
        }

        val raw = StringBuilder()
            .append(ProtonMailQuote)
            .append(context.getString(R.string.composer_original_message_quote))
            .append(LineBreak)
            .append(buildSenderQuote(message))
            .append(LineBreak)
            .append(ProtonMailBlockquote)
            .append(decryptedBody.value)
            .append(CloseProtonMailBlockquote)
            .append(CloseProtonMailQuote)
            .toString()
        return QuotedHtmlBody(raw)
    }


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
        const val ProtonMailBlockquote = "<blockquote class=\"protonmail_quote\""
        const val CloseProtonMailQuote = "</div>"
        const val CloseProtonMailBlockquote = "</blockquote>"
        const val LineBreak = "<br>"
    }
}
