/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.maildetail.presentation.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.maildetail.presentation.model.ParticipantUiModel
import ch.protonmail.android.maildetail.presentation.usecase.print.GetPrintHeaderStyle
import ch.protonmail.android.maildetail.presentation.usecase.print.GetPrintHeaderStyleError
import ch.protonmail.android.maildetail.presentation.usecase.print.PrintMessageHeaderBuilder
import ch.protonmail.android.mailmessage.domain.model.AttachmentListExpandCollapseMode
import ch.protonmail.android.mailmessage.presentation.model.attachment.AttachmentGroupUiModel
import ch.protonmail.android.mailmessage.presentation.sample.AttachmentMetadataUiModelSamples
import ch.protonmail.android.testdata.maildetail.MessageDetailHeaderUiModelTestData
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.collections.immutable.toImmutableList
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
internal class PrintMessageHeaderBuilderTest {

    private lateinit var builder: PrintMessageHeaderBuilder
    private val getPrintHeaderStyle = mockk<GetPrintHeaderStyle>()

    @BeforeTest
    fun setup() {
        builder = PrintMessageHeaderBuilder(RuntimeEnvironment.getApplication().applicationContext, getPrintHeaderStyle)
    }

    @AfterTest
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should provide the unformatted print layout on style fetch failure`() {
        // Given
        every { getPrintHeaderStyle.invoke() } returns GetPrintHeaderStyleError.left()
        val header = MessageDetailHeaderUiModelTestData.messageDetailHeaderUiModel.copy(
            toRecipients = toRecipients,
            ccRecipients = noRecipients,
            bccRecipients = noRecipients
        )
        val attachments = null
        val subject = "subject"

        @Suppress("MaxLineLength")
        val expectedBody =
            "<div class='print-header'><div class='print-header-title'>subject</div><div class='print-header-row'><span class='print-header-label'>From: </span><span class='print-header-value'>Sender &lt;sender@pm.com&gt;</span></div><div class='print-header-row'><span class='print-header-label'>To: </span><span class='print-header-value'><span class='print-header-recipient'>RecipientTo1 &lt;recipientTo1@pm.com&gt;</span>, <span class='print-header-recipient'>RecipientTo2 &lt;recipientTo2@pm.com&gt;</span></span></div><div class='print-header-row'><span class='print-header-label'>Date: </span><span class='print-header-value'>08/11/2022, 17:16</span></div></div>"

        // When
        val actual = builder.buildHeader(subject, header, attachments)

        // Then
        assertEquals(expectedBody, actual)
    }

    @Test
    fun `should provide the formatted print layout (To)`() {
        // Given
        every { getPrintHeaderStyle.invoke() } returns "style".right()
        val header = MessageDetailHeaderUiModelTestData.messageDetailHeaderUiModel.copy(
            toRecipients = toRecipients,
            ccRecipients = noRecipients,
            bccRecipients = noRecipients
        )
        val attachments = null
        val subject = "subject"

        @Suppress("MaxLineLength")
        val expectedBody =
            "<style>style</style><div class='print-header'><div class='print-header-title'>subject</div><div class='print-header-row'><span class='print-header-label'>From: </span><span class='print-header-value'>Sender &lt;sender@pm.com&gt;</span></div><div class='print-header-row'><span class='print-header-label'>To: </span><span class='print-header-value'><span class='print-header-recipient'>RecipientTo1 &lt;recipientTo1@pm.com&gt;</span>, <span class='print-header-recipient'>RecipientTo2 &lt;recipientTo2@pm.com&gt;</span></span></div><div class='print-header-row'><span class='print-header-label'>Date: </span><span class='print-header-value'>08/11/2022, 17:16</span></div></div>"

        // When
        val actual = builder.buildHeader(subject, header, attachments)

        // Then
        assertEquals(expectedBody, actual)
    }

    @Test
    fun `should provide the formatted print layout (Cc)`() {
        // Given
        every { getPrintHeaderStyle.invoke() } returns "style".right()
        val header = MessageDetailHeaderUiModelTestData.messageDetailHeaderUiModel.copy(
            toRecipients = noRecipients,
            ccRecipients = ccRecipients,
            bccRecipients = noRecipients
        )
        val attachments = null
        val subject = "subject"

        @Suppress("MaxLineLength")
        val expectedBody =
            "<style>style</style><div class='print-header'><div class='print-header-title'>subject</div><div class='print-header-row'><span class='print-header-label'>From: </span><span class='print-header-value'>Sender &lt;sender@pm.com&gt;</span></div><div class='print-header-row'><span class='print-header-label'>Cc: </span><span class='print-header-value'><span class='print-header-recipient'>RecipientCc1 &lt;recipientCc1@pm.com&gt;</span>, <span class='print-header-recipient'>RecipientCc2 &lt;recipientCc2@pm.com&gt;</span>, <span class='print-header-recipient'>RecipientCc3 &lt;recipientCc3@pm.com&gt;</span></span></div><div class='print-header-row'><span class='print-header-label'>Date: </span><span class='print-header-value'>08/11/2022, 17:16</span></div></div>"

        // When
        val actual = builder.buildHeader(subject, header, attachments)

        // Then
        assertEquals(expectedBody, actual)
    }

    @Test
    fun `should provide the formatted print layout (all fields)`() {
        // Given
        every { getPrintHeaderStyle.invoke() } returns "style".right()
        val header = MessageDetailHeaderUiModelTestData.messageDetailHeaderUiModel.copy(
            toRecipients = toRecipients,
            ccRecipients = ccRecipients,
            bccRecipients = bccRecipients
        )
        val attachments = AttachmentGroupUiModel(
            limit = 3,
            listOf(AttachmentMetadataUiModelSamples.Invoice, AttachmentMetadataUiModelSamples.Calendar),
            AttachmentListExpandCollapseMode.NotApplicable
        )
        val subject = "subject"

        @Suppress("MaxLineLength")
        val expectedBody =
            "<style>style</style><div class='print-header'><div class='print-header-title'>subject</div><div class='print-header-row'><span class='print-header-label'>From: </span><span class='print-header-value'>Sender &lt;sender@pm.com&gt;</span></div><div class='print-header-row'><span class='print-header-label'>To: </span><span class='print-header-value'><span class='print-header-recipient'>RecipientTo1 &lt;recipientTo1@pm.com&gt;</span>, <span class='print-header-recipient'>RecipientTo2 &lt;recipientTo2@pm.com&gt;</span></span></div><div class='print-header-row'><span class='print-header-label'>Cc: </span><span class='print-header-value'><span class='print-header-recipient'>RecipientCc1 &lt;recipientCc1@pm.com&gt;</span>, <span class='print-header-recipient'>RecipientCc2 &lt;recipientCc2@pm.com&gt;</span>, <span class='print-header-recipient'>RecipientCc3 &lt;recipientCc3@pm.com&gt;</span></span></div><div class='print-header-row'><span class='print-header-label'>Bcc: </span><span class='print-header-value'><span class='print-header-recipient'>RecipientBcc1 &lt;recipientBcc1@pm.com&gt;</span>, <span class='print-header-recipient'>RecipientBcc2 &lt;recipientBcc2@pm.com&gt;</span>, <span class='print-header-recipient'>RecipientBcc3 &lt;recipientBcc3@pm.com&gt;</span></span></div><div class='print-header-row'><span class='print-header-label'>Date: </span><span class='print-header-value'>08/11/2022, 17:16</span></div><div class='print-header-attachment'>2 Attachments (6.9 kB)</div></div>"

        // When
        val actual = builder.buildHeader(subject, header, attachments)

        // Then
        assertEquals(expectedBody, actual)
    }

    @Test
    fun `should escape HTML in subject and participant fields to prevent markup injection`() {
        // Given
        every { getPrintHeaderStyle.invoke() } returns GetPrintHeaderStyleError.left()
        val header = MessageDetailHeaderUiModelTestData.messageDetailHeaderUiModel.copy(
            sender = MessageDetailHeaderUiModelTestData.buildParticipant(
                "Test <img src=\"https://example.test/x\">",
                "a&b@pm.com"
            ),
            toRecipients = noRecipients,
            ccRecipients = noRecipients,
            bccRecipients = noRecipients
        )
        val attachments = null
        val subject = "<img src='https://example.test/p'>"

        // When
        val actual = builder.buildHeader(subject, header, attachments)

        // Then
        assertEquals(false, actual.contains("<img"))
        assertEquals(true, actual.contains("&lt;img src=&#39;https://example.test/p&#39;&gt;"))
        assertEquals(true, actual.contains("Test &lt;img src=&quot;https://example.test/x&quot;&gt;"))
        assertEquals(true, actual.contains("a&amp;b@pm.com"))
    }

    private companion object {

        val toRecipients = listOf(
            MessageDetailHeaderUiModelTestData.buildParticipant("RecipientTo1", "recipientTo1@pm.com"),
            MessageDetailHeaderUiModelTestData.buildParticipant("RecipientTo2", "recipientTo2@pm.com")
        ).toImmutableList()

        val ccRecipients = listOf(
            MessageDetailHeaderUiModelTestData.buildParticipant("RecipientCc1", "recipientCc1@pm.com"),
            MessageDetailHeaderUiModelTestData.buildParticipant("RecipientCc2", "recipientCc2@pm.com"),
            MessageDetailHeaderUiModelTestData.buildParticipant("RecipientCc3", "recipientCc3@pm.com")
        ).toImmutableList()

        val bccRecipients = listOf(
            MessageDetailHeaderUiModelTestData.buildParticipant("RecipientBcc1", "recipientBcc1@pm.com"),
            MessageDetailHeaderUiModelTestData.buildParticipant("RecipientBcc2", "recipientBcc2@pm.com"),
            MessageDetailHeaderUiModelTestData.buildParticipant("RecipientBcc3", "recipientBcc3@pm.com")
        ).toImmutableList()

        val noRecipients = emptyList<ParticipantUiModel>().toImmutableList()
    }
}
