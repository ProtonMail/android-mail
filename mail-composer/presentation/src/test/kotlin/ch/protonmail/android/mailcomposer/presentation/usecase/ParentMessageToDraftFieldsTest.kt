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
import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.usecase.FormatExtendedTime
import ch.protonmail.android.mailcomposer.domain.model.DraftAction
import ch.protonmail.android.mailcomposer.domain.model.DraftBody
import ch.protonmail.android.mailcomposer.domain.model.MessageWithDecryptedBody
import ch.protonmail.android.mailcomposer.domain.model.Subject
import ch.protonmail.android.mailcomposer.domain.usecase.ObserveUserAddresses
import ch.protonmail.android.mailcomposer.presentation.R
import ch.protonmail.android.mailcomposer.presentation.usecase.ParentMessageToDraftFields.Companion.CloseProtonMailBlockquote
import ch.protonmail.android.mailcomposer.presentation.usecase.ParentMessageToDraftFields.Companion.CloseProtonMailQuote
import ch.protonmail.android.mailcomposer.presentation.usecase.ParentMessageToDraftFields.Companion.LineBreak
import ch.protonmail.android.mailcomposer.presentation.usecase.ParentMessageToDraftFields.Companion.ProtonMailBlockquote
import ch.protonmail.android.mailcomposer.presentation.usecase.ParentMessageToDraftFields.Companion.ProtonMailQuote
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageSample
import ch.protonmail.android.mailmessage.domain.sample.MessageWithBodySample
import ch.protonmail.android.mailmessage.domain.sample.RecipientSample
import ch.protonmail.android.testdata.message.DecryptedMessageBodyTestData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.UserAddress
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class ParentMessageToDraftFieldsTest {

    private val observeUserAddresses = mockk<ObserveUserAddresses>()
    private val context = mockk<Context>()
    private val formatTime = mockk<FormatExtendedTime>()

    private val expectedOriginalMessageRes = expectStringRes(R.string.composer_original_message_quote) {
        "-------- Original Message --------"
    }
    private val expectedSenderQuoteRes = expectStringRes(R.string.composer_sender_quote) {
        "On %s, %s &lt; %s&gt; wrote:"
    }

    private val parentMessageToDraftFields = ParentMessageToDraftFields(context, observeUserAddresses, formatTime)

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
        val expectedSenderQuote = expectedSenderQuoteRes.format(
            expectedTime.value,
            expectedDecryptedMessage.messageWithBody.message.sender.name,
            expectedDecryptedMessage.messageWithBody.message.sender.address
        )
        expectedUserAddresses(userId) { listOf(UserAddressSample.PrimaryAddress) }

        // When
        val actual = parentMessageToDraftFields(userId, expectedDecryptedMessage, expectedAction).getOrNull()!!

        // Then
        val expectedQuotedHtmlBody = StringBuilder()
            .append(ProtonMailQuote)
            .append(expectedOriginalMessageRes)
            .append(LineBreak)
            .append(expectedSenderQuote)
            .append(LineBreak)
            .append(ProtonMailBlockquote)
            .append(expectedDecryptedMessage.decryptedMessageBody.value)
            .append(CloseProtonMailBlockquote)
            .append(CloseProtonMailQuote)
            .toString()
        assertEquals(expectedQuotedHtmlBody, actual.quotedHtmlBody?.value)
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

        // When
        val actual = parentMessageToDraftFields(userId, expectedDecryptedMessage, expectedAction).getOrNull()!!

        // Then
        val expected = expectedDecryptedMessage.messageWithBody.messageBody.replyTo
        assertEquals(listOf(expected), actual.recipientsTo.value)
    }

    @Test
    fun `returns draft data with sender and all to and cc recipients when action is reply all`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val expectedAction = DraftAction.ReplyAll(MessageIdSample.HtmlInvoice)
        val expectedDecryptedMessage = MessageWithDecryptedBody(
            MessageWithBodySample.HtmlInvoice.copy(
                MessageWithBodySample.HtmlInvoice.message.copy(
                    toList = listOf(RecipientSample.Alice, RecipientSample.Billing),
                    ccList = listOf(RecipientSample.Bob),
                    bccList = listOf(RecipientSample.Doe)
                )
            ),
            DecryptedMessageBodyTestData.htmlInvoice
        )
        expectedUserAddresses(userId) { listOf(UserAddressSample.PrimaryAddress) }
        expectFormattedTime(MessageSample.HtmlInvoice.time.seconds) { TextUiModel.Text("Sep 13, 2023 3:36 PM") }

        // When
        val actual = parentMessageToDraftFields(userId, expectedDecryptedMessage, expectedAction).getOrNull()!!

        // Then
        val expectedToList = listOf(RecipientSample.John, RecipientSample.Alice, RecipientSample.Billing)
        val expectedCcList = listOf(RecipientSample.Bob)
        assertTrue(actual.recipientsTo.value.containsAll(expectedToList))
        assertEquals(expectedCcList, actual.recipientsCc.value)
        assertEquals(emptyList(), actual.recipientsBcc.value)
    }

    @Test
    fun `own address used to reply is removed from toList when action is reply all`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val johnUserAddress = UserAddressSample.build(email = RecipientSample.John.address)
        val expectedAction = DraftAction.ReplyAll(MessageIdSample.HtmlInvoice)
        val expectedDecryptedMessage = MessageWithDecryptedBody(
            MessageWithBodySample.HtmlInvoice.copy(
                message = MessageWithBodySample.Invoice.message.copy(
                    toList = listOf(RecipientSample.John, RecipientSample.Billing, RecipientSample.Alice)
                )
            ),
            DecryptedMessageBodyTestData.htmlInvoice
        )
        expectedUserAddresses(userId) { listOf(johnUserAddress) }
        expectFormattedTime(MessageSample.HtmlInvoice.time.seconds) { TextUiModel.Text("Sep 13, 2023 3:36 PM") }

        // When
        val actual = parentMessageToDraftFields(userId, expectedDecryptedMessage, expectedAction).getOrNull()!!

        // Then
        val expected = listOf(RecipientSample.Billing, RecipientSample.Alice)
        assertEquals(expected, actual.recipientsTo.value)
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

    companion object {
        object TestData {
            val DraftBody = DraftBody("test unenecrypted body data")
        }
    }
}
