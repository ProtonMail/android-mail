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
import ch.protonmail.android.mailcomposer.domain.model.DraftBody
import ch.protonmail.android.mailcomposer.domain.model.DraftFields
import ch.protonmail.android.mailcomposer.domain.model.MessageWithDecryptedBody
import ch.protonmail.android.mailcomposer.domain.model.OriginalHtmlQuote
import ch.protonmail.android.mailcomposer.domain.model.RecipientsBcc
import ch.protonmail.android.mailcomposer.domain.model.RecipientsCc
import ch.protonmail.android.mailcomposer.domain.model.RecipientsTo
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.model.Subject
import ch.protonmail.android.mailcomposer.presentation.R
import ch.protonmail.android.mailmessage.domain.model.DecryptedMessageBody
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.model.Message
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import ch.protonmail.android.mailmessage.domain.model.MimeType
import ch.protonmail.android.mailmessage.domain.model.Recipient
import ch.protonmail.android.mailsettings.domain.model.MobileFooter
import ch.protonmail.android.mailsettings.domain.model.Signature
import ch.protonmail.android.mailsettings.domain.model.SignatureValue
import ch.protonmail.android.mailsettings.domain.usecase.identity.GetAddressSignature
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model.toPlainText
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.usecase.GetMobileFooter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.UserAddress
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class ParentMessageToDraftFields @Inject constructor(
    @ApplicationContext private val context: Context,
    private val observeUserAddresses: ObserveUserAddresses,
    private val formatExtendedTime: FormatExtendedTime,
    private val getAddressSignature: GetAddressSignature,
    private val getMobileFooter: GetMobileFooter,
    private val subjectWithPrefixForAction: SubjectWithPrefixForAction
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
        val senderAddressSignature =
            getAddressSignature(userId, sender.value).getOrElse { Signature(enabled = false, SignatureValue("")) }
        val mobileFooter = getMobileFooter(userId).getOrNull() ?: return DataError.Local.Unknown.left()

        return DraftFields(
            sender,
            Subject(subjectWithPrefixForAction(action, message.subject)),
            buildQuotedPlainTextBody(message, decryptedBody, senderAddressSignature, mobileFooter),
            RecipientsTo(recipientsForAction(action, messageWithDecryptedBody.messageWithBody)),
            RecipientsCc(ccRecipientsForAction(action, message, sender)),
            RecipientsBcc(bccRecipientsForAction(action, message)),
            buildQuotedHtmlBody(message, decryptedBody)
        ).right()
    }

    private fun buildQuotedPlainTextBody(
        message: Message,
        decryptedBody: DecryptedMessageBody,
        senderAddressSignature: Signature,
        mobileFooter: MobileFooter
    ): DraftBody {
        val senderSignatureEnabled = senderAddressSignature.enabled
        val senderSignatureValue = senderAddressSignature.value.toPlainText()
        val mobileFooterEnabled = mobileFooter.enabled
        val mobileFooterValue = mobileFooter.value

        if (decryptedBody.mimeType != MimeType.PlainText) {
            // HTML quote is fully created elsewhere, but we still need to inject signature
            //  and mobile footer into editable body
            StringBuilder().apply {
                if (senderSignatureEnabled && senderSignatureValue.isNotBlank()) {
                    append(SignatureFooterSeparator)
                    append(senderSignatureValue)
                }

                if (mobileFooterEnabled && mobileFooterValue.isNotBlank()) {
                    append(SignatureFooterSeparator)
                    append(mobileFooterValue)
                }
            }.let { return DraftBody(it.toString()) }
        }

        val bodyQuoted = decryptedBody.value
            .split("\n")
            .joinToString(separator = PlainTextNewLine) { "$PlainTextQuotePrefix $it" }

        val raw = StringBuilder().apply {
            if (senderSignatureEnabled && senderSignatureValue.isNotBlank()) {
                append(SignatureFooterSeparator)
                append(senderSignatureValue)
            }

            if (mobileFooterEnabled && mobileFooterValue.isNotBlank()) {
                append(SignatureFooterSeparator)
                append(mobileFooterValue)
            }

            append(PlainTextNewLine)
            append(PlainTextNewLine)
            append(PlainTextNewLine)
            append(buildOriginalMessageQuote())
            append(PlainTextNewLine)
            append(buildSenderQuote(message))
            append(PlainTextNewLine)
            append(PlainTextNewLine)
            append(bodyQuoted)
        }
        return DraftBody(raw.toString())
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

    private fun recipientsForAction(action: DraftAction, messageWithBody: MessageWithBody): List<Recipient> {
        val allRecipients = when (action) {
            is DraftAction.PrefillForShare,
            is DraftAction.Compose,
            is DraftAction.ComposeToAddresses, // will be handled via VM
            is DraftAction.Forward -> emptyList()

            is DraftAction.Reply -> if (messageWithBody.message.isSent()) {
                messageWithBody.message.toList
            } else {
                listOf(messageWithBody.messageBody.replyTo)
            }

            is DraftAction.ReplyAll -> if (messageWithBody.message.isSent()) {
                messageWithBody.message.toList
            } else {
                listOf(messageWithBody.messageBody.replyTo)
            }
        }
        return allRecipients
    }

    private fun ccRecipientsForAction(
        action: DraftAction,
        message: Message,
        senderEmail: SenderEmail
    ) = when (action) {
        is DraftAction.PrefillForShare,
        is DraftAction.Compose,
        is DraftAction.ComposeToAddresses,
        is DraftAction.Forward,
        is DraftAction.Reply -> emptyList()

        is DraftAction.ReplyAll -> if (message.isSent()) {
            message.ccList
        } else {
            (message.toList + message.ccList).filter { it.address != senderEmail.value }
        }
    }

    private fun bccRecipientsForAction(action: DraftAction, message: Message) = when (action) {
        is DraftAction.PrefillForShare,
        is DraftAction.Compose,
        is DraftAction.ComposeToAddresses,
        is DraftAction.Forward,
        is DraftAction.Reply -> emptyList()

        is DraftAction.ReplyAll -> if (message.isSent()) message.bccList else emptyList()
    }

    companion object {

        const val ProtonMailQuote = "<div class=\"protonmail_quote\">"
        const val ProtonMailBlockquote = "<blockquote class=\"protonmail_quote\">"
        const val CloseProtonMailQuote = "</div>"
        const val CloseProtonMailBlockquote = "</blockquote>"
        const val LineBreak = "<br>"
        const val PlainTextNewLine = "\n"
        const val PlainTextQuotePrefix = "> "
        const val SignatureFooterSeparator = "\n\n"
    }
}
