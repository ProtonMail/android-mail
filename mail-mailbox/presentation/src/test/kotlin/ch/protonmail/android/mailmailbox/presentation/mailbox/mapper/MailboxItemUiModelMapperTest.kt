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

package ch.protonmail.android.mailmailbox.presentation.mailbox.mapper

import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailconversation.domain.entity.Recipient
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmailbox.presentation.mailbox.usecase.FormatMailboxItemTime
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxItemTimeFormatter
import ch.protonmail.android.testdata.mailbox.MailboxTestData
import ch.protonmail.android.testdata.mailbox.MailboxTestData.buildMailboxItem
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import org.junit.Test
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class MailboxItemUiModelMapperTest {

    private val formatMailboxItemTime: FormatMailboxItemTime = mockk()

    private val mapper = MailboxItemUiModelMapper(formatMailboxItemTime)

    @BeforeTest
    fun setup() {
        mockkConstructor(Duration::class)
        every { formatMailboxItemTime(anyConstructed()) } returns TextUiModel.Text("21 Feb")
    }

    @AfterTest
    fun teardown() {
        unmockkConstructor(Duration::class)
    }

    @Test
    fun `when mailbox message item was replied ui model shows reply icon`() {
        // Given
        val mailboxItem = MailboxTestData.repliedMailboxItem
        // When
        val actual = mapper.toUiModel(mailboxItem)
        // Then
        assertTrue(actual.shouldShowRepliedIcon)
    }

    @Test
    fun `when mailbox message item was replied all ui model shows reply all icon`() {
        // Given
        val mailboxItem = MailboxTestData.repliedAllMailboxItem
        // When
        val actual = mapper.toUiModel(mailboxItem)
        // Then
        assertTrue(actual.shouldShowRepliedAllIcon)
        assertFalse(actual.shouldShowRepliedIcon)
    }

    @Test
    fun `when mailbox message item was forwarded ui model shows forwarded icon`() {
        // Given
        val mailboxItem = MailboxTestData.allActionsMailboxItem
        // When
        val actual = mapper.toUiModel(mailboxItem)
        // Then
        assertTrue(actual.shouldShowForwardedIcon)
    }

    @Test
    fun `mailbox items of conversation type never show any of reply, reply-all, forwarded icon`() {
        // Given
        val mailboxItem = MailboxTestData.mailboxConversationItem
        // When
        val actual = mapper.toUiModel(mailboxItem)
        // Then
        assertFalse(actual.shouldShowRepliedIcon)
        assertFalse(actual.shouldShowRepliedAllIcon)
        assertFalse(actual.shouldShowForwardedIcon)
    }

    @Test
    fun `when mailbox item is not in sent or drafts ui model shows senders as participants`() {
        // Given
        val senders = listOf(
            Recipient("sender@proton.ch", "sender"),
            Recipient("sender1@proton.ch", "sender1"),
        )
        val mailboxItem = buildMailboxItem(
            labelIds = listOf(SystemLabelId.Inbox.labelId.id),
            senders = senders
        )
        // When
        val actual = mapper.toUiModel(mailboxItem)
        // Then
        assertEquals(senders, actual.participants)
    }

    @Test
    fun `when mailbox item is in sent or drafts ui model shows recipients as participants`() {
        // Given
        val recipients = listOf(
            Recipient("recipient@proton.ch", "recipient"),
            Recipient("recipient1@proton.ch", "recipient1"),
        )
        val mailboxItem = buildMailboxItem(
            labelIds = listOf(SystemLabelId.Sent.labelId.id),
            recipients = recipients
        )
        // When
        val actual = mapper.toUiModel(mailboxItem)
        // Then
        assertEquals(recipients, actual.participants)
    }

    @Test
    fun `mailbox item time is formatted in the ui model`() {
        // Given
        val time: Long = 1658851202
        val mailboxItem = buildMailboxItem(time = time)
        val result = TextUiModel.Text("18:00")
        every { formatMailboxItemTime.invoke(time.seconds) } returns result
        // When
        val actual = mapper.toUiModel(mailboxItem)
        // Then
        assertEquals(result, actual.time)
    }

    @Test
    fun `when mailbox item of conversation type contains two or more messages show messages number`() {
        val mailboxItem = buildMailboxItem(type = MailboxItemType.Conversation, numMessages = 2)

        val actual = mapper.toUiModel(mailboxItem)

        assertEquals(2, actual.numMessages)
    }

    @Test
    fun `when mailbox item is starred show starred`() {
        val mailboxItem = buildMailboxItem(labelIds = listOf(SystemLabelId.Starred.labelId.id))

        val actual = mapper.toUiModel(mailboxItem)

        assertTrue(actual.showStar)
    }

    @Test
    fun `when mailbox item is not starred do not show starred`() {
        val labelIds = listOf(SystemLabelId.Drafts.labelId.id, SystemLabelId.Archive.labelId.id)
        val mailboxItem = buildMailboxItem(labelIds = labelIds)

        val actual = mapper.toUiModel(mailboxItem)

        assertFalse(actual.showStar)
    }
}
