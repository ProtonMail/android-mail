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
import androidx.annotation.StringRes
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcommon.domain.usecase.ObserveUserAddresses
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.usecase.FormatExtendedTime
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailcomposer.domain.model.MessageWithDecryptedBody
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.model.Subject
import ch.protonmail.android.mailcomposer.presentation.R
import ch.protonmail.android.mailcomposer.presentation.usecase.ParentMessageToDraftFields.Companion.CloseProtonMailBlockquote
import ch.protonmail.android.mailcomposer.presentation.usecase.ParentMessageToDraftFields.Companion.CloseProtonMailQuote
import ch.protonmail.android.mailcomposer.presentation.usecase.ParentMessageToDraftFields.Companion.LineBreak
import ch.protonmail.android.mailcomposer.presentation.usecase.ParentMessageToDraftFields.Companion.ProtonMailBlockquote
import ch.protonmail.android.mailcomposer.presentation.usecase.ParentMessageToDraftFields.Companion.ProtonMailQuote
import ch.protonmail.android.mailmessage.domain.model.Recipient
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageSample
import ch.protonmail.android.mailmessage.domain.sample.MessageWithBodySample
import ch.protonmail.android.mailmessage.domain.sample.RecipientSample
import ch.protonmail.android.mailsettings.domain.model.MobileFooter
import ch.protonmail.android.mailsettings.domain.model.Signature
import ch.protonmail.android.mailsettings.domain.model.SignatureValue
import ch.protonmail.android.mailsettings.domain.usecase.identity.GetAddressSignature
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model.toPlainText
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.usecase.GetMobileFooter
import ch.protonmail.android.testdata.message.DecryptedMessageBodyTestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.user.domain.entity.UserAddress
import org.junit.After
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class ParentMessageToDraftFieldsTest {

    private val observeUserAddresses = mockk<ObserveUserAddresses>()
    private val context = mockk<Context>()
    private val formatTime = mockk<FormatExtendedTime>()
    private val getAddressSignatureMock = mockk<GetAddressSignature>()
    private val getMobileFooterMock = mockk<GetMobileFooter>()
    private val subjectWithPrefixForAction = SubjectWithPrefixForAction()

    private val expectedOriginalMessageRes = expectStringRes(R.string.composer_original_message_quote) {
        "Original Message"
    }
    private val expectedSenderQuoteRes = expectStringRes(R.string.composer_sender_quote) {
        "On %s, %s &lt; %s&gt; wrote:"
    }

    private val paidMobileFooter = ""
    private val freeMobileFooter = "Sent from Proton Mail Android"

    private val parentMessageToDraftFields = ParentMessageToDraftFields(
        context,
        observeUserAddresses,
        formatTime,
        getAddressSignatureMock,
        getMobileFooterMock,
        subjectWithPrefixForAction
    )

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `returns quoted draft body with injected sender and body data`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val expectedAction = DraftAction.Reply(MessageIdSample.HtmlInvoice)
        val expectedDecryptedMessage = MessageWithDecryptedBody(
            MessageWithBodySample.HtmlInvoice,
            DecryptedMessageBodyTestData.htmlInvoice
        )
        val expectedTime = expectFormattedTime(MessageSample.HtmlInvoice.time.seconds) {
            TextUiModel.Text("Sep 13, 2023 3:36 PM")
        }
        val expectedOriginalMessageQuote = "-------- $expectedOriginalMessageRes --------"
        val expectedSenderQuote = expectedSenderQuoteRes.format(
            expectedTime.value,
            expectedDecryptedMessage.messageWithBody.message.sender.name,
            expectedDecryptedMessage.messageWithBody.message.sender.address
        )
        expectedUserAddresses(userId) { listOf(UserAddressSample.PrimaryAddress) }
        val expectedBody = expectedDecryptedMessage.decryptedMessageBody.value
        expectBlankSignatureForSenderAddress(userId, SenderEmail(UserAddressSample.PrimaryAddress.email))
        expectMobileFooter(userId, isUserPaid = true)

        // When
        val actual = parentMessageToDraftFields(userId, expectedDecryptedMessage, expectedAction).getOrNull()!!

        // Then
        val expectedQuotedHtmlBody = StringBuilder()
            .append(ProtonMailQuote)
            .append(LineBreak)
            .append(LineBreak)
            .append(expectedOriginalMessageQuote)
            .append(LineBreak)
            .append(expectedSenderQuote)
            .append(LineBreak)
            .append(ProtonMailBlockquote)
            .append(expectedBody)
            .append(CloseProtonMailBlockquote)
            .append(CloseProtonMailQuote)
            .toString()
        assertEquals(expectedQuotedHtmlBody, actual.originalHtmlQuote?.value)
    }

    @Test
    fun `returns draft body with injected sender signature for plaintext message, paid user`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val expectedAction = DraftAction.Reply(MessageIdSample.Invoice)
        val expectedDecryptedMessage = MessageWithDecryptedBody(
            MessageWithBodySample.Invoice,
            DecryptedMessageBodyTestData.PlainTextDecryptedBody
        )
        val expectedTime = expectFormattedTime(MessageSample.Invoice.time.seconds) {
            TextUiModel.Text("Sep 13, 2023 3:36 PM")
        }
        val expectedOriginalMessageQuote = "-------- $expectedOriginalMessageRes --------"
        val expectedSenderQuote = expectedSenderQuoteRes.format(
            expectedTime.value,
            expectedDecryptedMessage.messageWithBody.message.sender.name,
            expectedDecryptedMessage.messageWithBody.message.sender.address
        )
        expectedUserAddresses(userId) { listOf(UserAddressSample.PrimaryAddress) }
        val expectedBody = "${ParentMessageToDraftFields.PlainTextQuotePrefix} " +
            expectedDecryptedMessage.decryptedMessageBody.value
        val expectedSignature = expectSignatureForSenderAddress(
            userId,
            SenderEmail(UserAddressSample.PrimaryAddress.email)
        )
        expectMobileFooter(userId, isUserPaid = true)

        // When
        val actual = parentMessageToDraftFields(userId, expectedDecryptedMessage, expectedAction).getOrNull()!!

        // Then
        val expectedQuotedPlaintextBody = StringBuilder()
            .append(ParentMessageToDraftFields.SignatureFooterSeparator)
            .append(expectedSignature.value.toPlainText())
            .append(ParentMessageToDraftFields.PlainTextNewLine)
            .append(ParentMessageToDraftFields.PlainTextNewLine)
            .append(ParentMessageToDraftFields.PlainTextNewLine)
            .append(expectedOriginalMessageQuote)
            .append(ParentMessageToDraftFields.PlainTextNewLine)
            .append(expectedSenderQuote)
            .append(ParentMessageToDraftFields.PlainTextNewLine)
            .append(ParentMessageToDraftFields.PlainTextNewLine)
            .append(expectedBody)
            .toString()

        assertEquals(expectedQuotedPlaintextBody, actual.body.value)
    }

    @Test
    fun `returns draft body with injected sender signature for plaintext message, free user`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val expectedAction = DraftAction.Reply(MessageIdSample.Invoice)
        val expectedDecryptedMessage = MessageWithDecryptedBody(
            MessageWithBodySample.Invoice,
            DecryptedMessageBodyTestData.PlainTextDecryptedBody
        )
        val expectedTime = expectFormattedTime(MessageSample.Invoice.time.seconds) {
            TextUiModel.Text("Sep 13, 2023 3:36 PM")
        }
        val expectedOriginalMessageQuote = "-------- $expectedOriginalMessageRes --------"
        val expectedSenderQuote = expectedSenderQuoteRes.format(
            expectedTime.value,
            expectedDecryptedMessage.messageWithBody.message.sender.name,
            expectedDecryptedMessage.messageWithBody.message.sender.address
        )
        expectedUserAddresses(userId) { listOf(UserAddressSample.PrimaryAddress) }
        val expectedBody = "${ParentMessageToDraftFields.PlainTextQuotePrefix} " +
            expectedDecryptedMessage.decryptedMessageBody.value
        val expectedSignature = expectSignatureForSenderAddress(
            userId,
            SenderEmail(UserAddressSample.PrimaryAddress.email)
        )
        val expectedMobileFooter = expectMobileFooter(userId, isUserPaid = false)

        // When
        val actual = parentMessageToDraftFields(userId, expectedDecryptedMessage, expectedAction).getOrNull()!!

        // Then
        val expectedQuotedPlaintextBody = StringBuilder()
            .append(ParentMessageToDraftFields.SignatureFooterSeparator)
            .append(expectedSignature.value.toPlainText())
            .append(ParentMessageToDraftFields.SignatureFooterSeparator)
            .append(expectedMobileFooter)
            .append(ParentMessageToDraftFields.PlainTextNewLine)
            .append(ParentMessageToDraftFields.PlainTextNewLine)
            .append(ParentMessageToDraftFields.PlainTextNewLine)
            .append(expectedOriginalMessageQuote)
            .append(ParentMessageToDraftFields.PlainTextNewLine)
            .append(expectedSenderQuote)
            .append(ParentMessageToDraftFields.PlainTextNewLine)
            .append(ParentMessageToDraftFields.PlainTextNewLine)
            .append(expectedBody)
            .toString()

        assertEquals(expectedQuotedPlaintextBody, actual.body.value)
    }

    @Test
    fun `returns draft body with injected blank sender signature for plaintext message`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val expectedAction = DraftAction.Reply(MessageIdSample.Invoice)
        val expectedDecryptedMessage = MessageWithDecryptedBody(
            MessageWithBodySample.Invoice,
            DecryptedMessageBodyTestData.PlainTextDecryptedBody
        )
        val expectedTime = expectFormattedTime(MessageSample.Invoice.time.seconds) {
            TextUiModel.Text("Sep 13, 2023 3:36 PM")
        }
        val expectedOriginalMessageQuote = "-------- $expectedOriginalMessageRes --------"
        val expectedSenderQuote = expectedSenderQuoteRes.format(
            expectedTime.value,
            expectedDecryptedMessage.messageWithBody.message.sender.name,
            expectedDecryptedMessage.messageWithBody.message.sender.address
        )
        expectedUserAddresses(userId) { listOf(UserAddressSample.PrimaryAddress) }
        val expectedBody = "${ParentMessageToDraftFields.PlainTextQuotePrefix} " +
            expectedDecryptedMessage.decryptedMessageBody.value
        expectBlankSignatureForSenderAddress(
            userId,
            SenderEmail(UserAddressSample.PrimaryAddress.email)
        )
        expectMobileFooter(userId, isUserPaid = true)

        // When
        val actual = parentMessageToDraftFields(userId, expectedDecryptedMessage, expectedAction).getOrNull()!!

        // Then
        val expectedQuotedPlaintextBody = StringBuilder()
            .append(ParentMessageToDraftFields.PlainTextNewLine)
            .append(ParentMessageToDraftFields.PlainTextNewLine)
            .append(ParentMessageToDraftFields.PlainTextNewLine)
            .append(expectedOriginalMessageQuote)
            .append(ParentMessageToDraftFields.PlainTextNewLine)
            .append(expectedSenderQuote)
            .append(ParentMessageToDraftFields.PlainTextNewLine)
            .append(ParentMessageToDraftFields.PlainTextNewLine)
            .append(expectedBody)
            .toString()

        assertEquals(expectedQuotedPlaintextBody, actual.body.value)
    }

    @Test
    fun `returns draft body with injected sender signature for HTML message`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val expectedAction = DraftAction.Reply(MessageIdSample.HtmlInvoice)
        val expectedDecryptedMessage = MessageWithDecryptedBody(
            MessageWithBodySample.HtmlInvoice,
            DecryptedMessageBodyTestData.htmlInvoice
        )
        expectFormattedTime(MessageSample.HtmlInvoice.time.seconds) {
            TextUiModel.Text("Sep 13, 2023 3:36 PM")
        }
        expectedUserAddresses(userId) { listOf(UserAddressSample.PrimaryAddress) }
        val expectedSignature = expectSignatureForSenderAddress(
            userId,
            SenderEmail(UserAddressSample.PrimaryAddress.email)
        )
        val footer = expectMobileFooter(userId, isUserPaid = true)
        val expectedBody =
            ParentMessageToDraftFields.SignatureFooterSeparator + expectedSignature.value.toPlainText() + footer

        // When
        val actual = parentMessageToDraftFields(userId, expectedDecryptedMessage, expectedAction).getOrNull()!!

        // Then
        assertEquals(expectedBody, actual.body.value)
    }

    @Test
    fun `returns draft body with injected blank sender signature for HTML message`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val expectedAction = DraftAction.Reply(MessageIdSample.HtmlInvoice)
        val expectedDecryptedMessage = MessageWithDecryptedBody(
            MessageWithBodySample.HtmlInvoice,
            DecryptedMessageBodyTestData.htmlInvoice
        )
        expectFormattedTime(MessageSample.HtmlInvoice.time.seconds) {
            TextUiModel.Text("Sep 13, 2023 3:36 PM")
        }
        expectedUserAddresses(userId) { listOf(UserAddressSample.PrimaryAddress) }
        expectBlankSignatureForSenderAddress(
            userId,
            SenderEmail(UserAddressSample.PrimaryAddress.email)
        )
        expectMobileFooter(userId, isUserPaid = true)
        val expectedBody = ""

        // When
        val actual = parentMessageToDraftFields(userId, expectedDecryptedMessage, expectedAction).getOrNull()!!

        // Then
        assertEquals(expectedBody, actual.body.value)
    }

    @Test
    fun `returns draft body with no sender signature for HTML message when signature is disabled`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val expectedAction = DraftAction.Reply(MessageIdSample.HtmlInvoice)
        val expectedDecryptedMessage = MessageWithDecryptedBody(
            MessageWithBodySample.HtmlInvoice,
            DecryptedMessageBodyTestData.htmlInvoice
        )
        expectFormattedTime(MessageSample.HtmlInvoice.time.seconds) {
            TextUiModel.Text("Sep 13, 2023 3:36 PM")
        }
        expectedUserAddresses(userId) { listOf(UserAddressSample.PrimaryAddress) }
        expectSignatureForSenderAddress(
            userId,
            SenderEmail(UserAddressSample.PrimaryAddress.email),
            enabled = false
        )
        expectMobileFooter(userId, isUserPaid = true)
        val expectedBody = ""

        // When
        val actual = parentMessageToDraftFields(userId, expectedDecryptedMessage, expectedAction).getOrNull()!!

        // Then
        assertEquals(expectedBody, actual.body.value)
    }

    @Test
    fun `returns draft data with prefixed subject based on draft action`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val expectedAction = DraftAction.Reply(MessageIdSample.HtmlInvoice)
        val expectedDecryptedMessage = MessageWithDecryptedBody(
            MessageWithBodySample.HtmlInvoice,
            DecryptedMessageBodyTestData.htmlInvoice
        )
        expectedUserAddresses(userId) { listOf(UserAddressSample.PrimaryAddress) }
        expectFormattedTime(MessageSample.HtmlInvoice.time.seconds) { TextUiModel.Text("Sep 13, 2023 3:36 PM") }
        expectBlankSignatureForSenderAddress(userId, SenderEmail(UserAddressSample.PrimaryAddress.email))
        expectMobileFooter(userId, isUserPaid = true)

        // When
        val actual = parentMessageToDraftFields(userId, expectedDecryptedMessage, expectedAction).getOrNull()!!

        // Then
        val expected = Subject("Re: ${expectedDecryptedMessage.messageWithBody.message.subject}")
        assertEquals(expected, actual.subject)
    }

    @Test
    fun `returns draft data with reply-to address as to list when action is reply`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val expectedAction = DraftAction.Reply(MessageIdSample.HtmlInvoice)
        val expectedDecryptedMessage = MessageWithDecryptedBody(
            MessageWithBodySample.HtmlInvoice,
            DecryptedMessageBodyTestData.htmlInvoice
        )
        expectedUserAddresses(userId) { listOf(UserAddressSample.PrimaryAddress) }
        expectFormattedTime(MessageSample.HtmlInvoice.time.seconds) { TextUiModel.Text("Sep 13, 2023 3:36 PM") }
        expectBlankSignatureForSenderAddress(userId, SenderEmail(UserAddressSample.PrimaryAddress.email))
        expectMobileFooter(userId, isUserPaid = true)

        // When
        val actual = parentMessageToDraftFields(userId, expectedDecryptedMessage, expectedAction).getOrNull()!!

        // Then
        val expected = expectedDecryptedMessage.messageWithBody.messageBody.replyTo
        assertEquals(listOf(expected), actual.recipientsTo.value)
    }

    @Test
    fun `returns draft data with sender and all to and cc recipients when action is reply all`() = runTest {
        // Given
        val expectedAction = DraftAction.ReplyAll(MessageIdSample.HtmlInvoice)
        val expectedTo = listOf(RecipientSample.Billing)
        val expectedCc = listOf(RecipientSample.Alice, RecipientSample.Billing, RecipientSample.Bob)

        val expectedDecryptedMessage = MessageWithDecryptedBody(
            MessageWithBodySample.build(
                replyTo = RecipientSample.Billing,
                message = MessageWithBodySample.HtmlInvoice.message.copy(
                    toList = listOf(RecipientSample.Alice, RecipientSample.Billing),
                    ccList = listOf(RecipientSample.Bob),
                    bccList = listOf(RecipientSample.Doe)
                )
            ),
            DecryptedMessageBodyTestData.htmlInvoice
        )

        // When + Then
        testRecipientFieldsPrefill(
            expectedDecryptedMessage, expectedAction, expectedTo, expectedCc
        )
    }

    @Test
    fun `own address used to reply is removed from toList when action is reply all`() = runTest {
        // Given
        val expectedAction = DraftAction.ReplyAll(MessageIdSample.HtmlInvoice)
        val expectedCc = listOf(RecipientSample.Scammer, RecipientSample.Alice)
        val expectedTo = listOf(RecipientSample.Billing)

        val expectedDecryptedMessage = MessageWithDecryptedBody(
            MessageWithBodySample.build(
                replyTo = RecipientSample.Billing,
                message = MessageWithBodySample.Invoice.message.copy(
                    toList = listOf(RecipientSample.John, RecipientSample.Scammer, RecipientSample.Alice)
                )
            ),
            DecryptedMessageBodyTestData.htmlInvoice
        )

        // When + Then
        testRecipientFieldsPrefill(
            expectedDecryptedMessage, expectedAction, expectedTo, expectedCc
        )
    }

    @Test
    fun `reply all contains replyTo in To field while other recipients are in Cc`() = runTest {
        // Given
        val expectedAction = DraftAction.ReplyAll(MessageIdSample.HtmlInvoice)
        val expectedCc = listOf(RecipientSample.PreciWeather, RecipientSample.Alice)
        val expectedTo = listOf(RecipientSample.Billing)
        val expectedDecryptedMessage = MessageWithDecryptedBody(
            MessageWithBodySample.build(
                replyTo = RecipientSample.Billing,
                message = MessageWithBodySample.Invoice.message.copy(
                    toList = listOf(RecipientSample.PreciWeather, RecipientSample.Alice),
                    ccList = listOf(RecipientSample.John)
                )
            ),
            DecryptedMessageBodyTestData.htmlInvoice
        )

        // When + Then
        testRecipientFieldsPrefill(
            expectedDecryptedMessage, expectedAction, expectedTo, expectedCc
        )
    }

    @Test
    fun `own address is removed from Cc field when action is reply all`() = runTest {
        val expectedAction = DraftAction.ReplyAll(MessageIdSample.HtmlInvoice)
        val expectedTo = listOf(RecipientSample.Billing)
        val expectedCc = listOf(RecipientSample.PreciWeather, RecipientSample.Alice)
        val expectedDecryptedMessage = MessageWithDecryptedBody(
            MessageWithBodySample.build(
                replyTo = RecipientSample.Billing,
                message = MessageWithBodySample.Invoice.message.copy(
                    toList = listOf(RecipientSample.PreciWeather),
                    ccList = listOf(RecipientSample.John, RecipientSample.Alice)
                )
            ),
            DecryptedMessageBodyTestData.htmlInvoice
        )

        // When + Then
        testRecipientFieldsPrefill(
            expectedDecryptedMessage, expectedAction, expectedTo, expectedCc
        )
    }

    @Test
    fun `returns draft fields with TO recipients from original message when replying to a sent message`() = runTest {
        // Given
        val expectedAction = DraftAction.Reply(MessageIdSample.HtmlInvoice)
        val expectedTo = listOf(RecipientSample.John, RecipientSample.Billing, RecipientSample.Alice)
        val expectedCc = emptyList<Recipient>()

        val expectedDecryptedMessage = MessageWithDecryptedBody(
            MessageWithBodySample.HtmlInvoice.copy(
                message = MessageWithBodySample.Invoice.message.copy(
                    toList = listOf(RecipientSample.John, RecipientSample.Billing, RecipientSample.Alice),
                    labelIds = MessageWithBodySample.Invoice.message.labelIds + LabelId("2")
                )
            ),
            DecryptedMessageBodyTestData.htmlInvoice
        )

        // When + Then
        testRecipientFieldsPrefill(
            expectedDecryptedMessage, expectedAction, expectedTo, expectedCc
        )
    }

    @Test
    fun `returns draft fields with BCC recipients from original message when reply all to a sent message`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val johnUserAddress = UserAddressSample.build(email = RecipientSample.John.address)
        val expectedAction = DraftAction.ReplyAll(MessageIdSample.HtmlInvoice)
        val expectedTo = emptyList<Recipient>()
        val expectedCc = emptyList<Recipient>()
        val expectedBcc = listOf(RecipientSample.Billing, RecipientSample.Alice)

        val expectedDecryptedMessage = MessageWithDecryptedBody(
            MessageWithBodySample.HtmlInvoice.copy(
                message = MessageWithBodySample.Invoice.message.copy(
                    bccList = listOf(RecipientSample.Billing, RecipientSample.Alice),
                    labelIds = MessageWithBodySample.Invoice.message.labelIds + LabelId("2")
                )
            ),
            DecryptedMessageBodyTestData.htmlInvoice
        )
        expectedUserAddresses(userId) { listOf(johnUserAddress) }
        expectFormattedTime(MessageSample.HtmlInvoice.time.seconds) { TextUiModel.Text("Sep 13, 2023 3:36 PM") }
        expectBlankSignatureForSenderAddress(userId, SenderEmail(johnUserAddress.email))
        expectMobileFooter(userId, isUserPaid = true)

        // When + Then
        testRecipientFieldsPrefill(
            expectedDecryptedMessage, expectedAction, expectedTo, expectedCc, expectedBcc
        )
    }

    private suspend fun testRecipientFieldsPrefill(
        message: MessageWithDecryptedBody,
        action: DraftAction,
        expectedTo: List<Recipient>,
        expectedCc: List<Recipient>,
        expectedBcc: List<Recipient> = emptyList()
    ) {
        val userId = UserIdSample.Primary
        val johnUserAddress = UserAddressSample.build(email = RecipientSample.John.address)

        expectedUserAddresses(userId) { listOf(johnUserAddress) }
        expectFormattedTime(MessageSample.HtmlInvoice.time.seconds) { TextUiModel.Text("Sep 13, 2023 3:36 PM") }
        expectBlankSignatureForSenderAddress(userId, SenderEmail(johnUserAddress.email))
        expectMobileFooter(userId, isUserPaid = true)

        // When
        val actual = parentMessageToDraftFields(userId, message, action).getOrNull()!!

        // Then
        assertEquals(expectedTo, actual.recipientsTo.value)
        assertEquals(expectedCc, actual.recipientsCc.value)
        assertEquals(expectedBcc, actual.recipientsBcc.value)
    }

    private fun expectFormattedTime(timestamp: Duration, result: () -> TextUiModel.Text) = result().also {
        every { formatTime(timestamp) } returns it
    }

    private fun expectStringRes(@StringRes id: Int, result: () -> String) = result().also {
        every { context.getString(id) } returns it
    }

    private fun expectedUserAddresses(userId: UserId, addresses: () -> List<UserAddress>) = addresses().also {
        every { observeUserAddresses.invoke(userId) } returns flowOf(it)
    }

    private fun expectSignatureForSenderAddress(
        expectedUserId: UserId,
        expectedSenderEmail: SenderEmail,
        enabled: Boolean = true
    ): Signature = Signature(
        enabled = enabled,
        SignatureValue("<div>HTML signature</div>")
    ).also {
        coEvery { getAddressSignatureMock(expectedUserId, expectedSenderEmail.value) } returns it.right()
    }

    private fun expectBlankSignatureForSenderAddress(
        expectedUserId: UserId,
        expectedSenderEmail: SenderEmail
    ): Signature = Signature(
        enabled = true,
        SignatureValue("")
    ).also { coEvery { getAddressSignatureMock(expectedUserId, expectedSenderEmail.value) } returns it.right() }

    private fun expectMobileFooter(expectedUserId: UserId, isUserPaid: Boolean): String {

        val footer = if (isUserPaid) {
            MobileFooter.PaidUserMobileFooter(paidMobileFooter, enabled = true)
        } else {
            MobileFooter.FreeUserMobileFooter(freeMobileFooter)
        }

        coEvery { getMobileFooterMock(expectedUserId) } returns footer.right()
        return footer.value
    }
}
