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

import androidx.compose.ui.graphics.Color
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.sample.LabelSample
import ch.protonmail.android.mailcommon.presentation.mapper.ColorMapper
import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.usecase.FormatShortTime
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.presentation.R
import ch.protonmail.android.maillabel.presentation.model.LabelUiModel
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.domain.usecase.GetParticipantsResolvedNames
import ch.protonmail.android.mailmailbox.domain.usecase.ParticipantsResolvedNamesResult
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemLocationUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.ParticipantUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.ParticipantsUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.usecase.GetMailboxItemLocationIcons
import ch.protonmail.android.mailmessage.domain.usecase.ResolveParticipantNameResult
import ch.protonmail.android.mailsettings.domain.model.FolderColorSettings
import ch.protonmail.android.testdata.contact.ContactTestData
import ch.protonmail.android.testdata.mailbox.MailboxTestData
import ch.protonmail.android.testdata.mailbox.MailboxTestData.buildMailboxItem
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.test.runTest
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.label.domain.entity.LabelType
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class MailboxItemUiModelMapperTest {

    private val defaultFolderColorSettings = FolderColorSettings()

    private val mailboxAvatarUiModelMapper: MailboxAvatarUiModelMapper = mockk {
        every { this@mockk.invoke(any(), any()) } returns mockk()
    }
    private val colorMapper: ColorMapper = mockk {
        every { toColor(any()) } returns Color.Unspecified.right()
    }
    private val getMailboxItemLocationIcons = mockk<GetMailboxItemLocationIcons> {
        coEvery { this@mockk(any(), defaultFolderColorSettings, false) } returns GetMailboxItemLocationIcons.Result.None
    }
    private val formatMailboxItemTime: FormatShortTime = mockk()

    private val getParticipantsResolvedNames = mockk<GetParticipantsResolvedNames> {
        every {
            this@mockk.invoke(any(), any())
        } returns ParticipantsResolvedNamesResult.Senders(
            listOf(
                ResolveParticipantNameResult(
                    "default mocked name",
                    isProton = false
                )
            )
        )
    }

    private val mapper = MailboxItemUiModelMapper(
        mailboxAvatarUiModelMapper = mailboxAvatarUiModelMapper,
        colorMapper = colorMapper,
        formatMailboxItemTime = formatMailboxItemTime,
        getMailboxItemLocationIcons = getMailboxItemLocationIcons,
        getParticipantsResolvedNames = getParticipantsResolvedNames
    )

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
    fun `when mailbox message item was replied ui model shows reply icon`() = runTest {
        // Given
        val mailboxItem = MailboxTestData.repliedMailboxItem
        // When
        val actual = mapper.toUiModel(mailboxItem, ContactTestData.contacts, defaultFolderColorSettings, false)
        // Then
        assertTrue(actual.shouldShowRepliedIcon)
    }

    @Test
    fun `when mailbox message item was replied all ui model shows reply all icon`() = runTest {
        // Given
        val mailboxItem = MailboxTestData.repliedAllMailboxItem
        // When
        val actual = mapper.toUiModel(mailboxItem, ContactTestData.contacts, defaultFolderColorSettings, false)
        // Then
        assertTrue(actual.shouldShowRepliedAllIcon)
        assertFalse(actual.shouldShowRepliedIcon)
    }

    @Test
    fun `when mailbox message item was forwarded ui model shows forwarded icon`() = runTest {
        // Given
        val mailboxItem = MailboxTestData.allActionsMailboxItem
        // When
        val actual = mapper.toUiModel(mailboxItem, ContactTestData.contacts, defaultFolderColorSettings, false)
        // Then
        assertTrue(actual.shouldShowForwardedIcon)
    }

    @Test
    fun `mailbox items of conversation type never show any of reply, reply-all, forwarded icon`() = runTest {
        // Given
        val mailboxItem = MailboxTestData.mailboxConversationItem
        // When
        val actual = mapper.toUiModel(mailboxItem, ContactTestData.contacts, defaultFolderColorSettings, false)
        // Then
        assertFalse(actual.shouldShowRepliedIcon)
        assertFalse(actual.shouldShowRepliedAllIcon)
        assertFalse(actual.shouldShowForwardedIcon)
    }

    @Test
    fun `participant names are correctly resolved in the ui model`() = runTest {
        // Given
        val mailboxItem = buildMailboxItem()
        val resolvedNames = listOf(
            ResolveParticipantNameResult("contact name", isProton = false),
            ResolveParticipantNameResult("display name", isProton = true)
        )
        val expected = ParticipantsUiModel.Participants(
            listOf(
                ParticipantUiModel("contact name", shouldShowOfficialBadge = false),
                ParticipantUiModel("display name", shouldShowOfficialBadge = true)
            ).toImmutableList()
        )
        every {
            getParticipantsResolvedNames.invoke(mailboxItem, ContactTestData.contacts)
        } returns ParticipantsResolvedNamesResult.Recipients(resolvedNames)
        // When
        val actual = mapper.toUiModel(mailboxItem, ContactTestData.contacts, defaultFolderColorSettings, false)
        // Then
        assertEquals(expected, actual.participants)
    }

    @Test
    fun `empty recipient is correctly resolved to No Recipient in the ui model`() = runTest {
        // Given
        val mailboxItem = buildMailboxItem()
        val resolvedNames = listOf(ResolveParticipantNameResult("", isProton = false))
        val expected = ParticipantsUiModel.NoParticipants(
            TextUiModel(ch.protonmail.android.mailmailbox.presentation.R.string.mailbox_default_recipient)
        )
        every {
            getParticipantsResolvedNames.invoke(mailboxItem, ContactTestData.contacts)
        } returns ParticipantsResolvedNamesResult.Recipients(resolvedNames)
        // When
        val actual = mapper.toUiModel(mailboxItem, ContactTestData.contacts, defaultFolderColorSettings, false)
        // Then
        assertEquals(expected, actual.participants)
    }

    @Test
    fun `empty sender is correctly resolved to No Sender in the ui model`() = runTest {
        // Given
        val mailboxItem = buildMailboxItem()
        val resolvedNames = listOf(ResolveParticipantNameResult("", isProton = false))
        val expected = ParticipantsUiModel.NoParticipants(
            TextUiModel(ch.protonmail.android.mailmailbox.presentation.R.string.mailbox_default_sender)
        )
        every {
            getParticipantsResolvedNames.invoke(mailboxItem, ContactTestData.contacts)
        } returns ParticipantsResolvedNamesResult.Senders(resolvedNames)
        // When
        val actual = mapper.toUiModel(mailboxItem, ContactTestData.contacts, defaultFolderColorSettings, false)
        // Then
        assertEquals(expected, actual.participants)
    }

    @Test
    fun `mailbox item time is formatted in the ui model`() = runTest {
        // Given
        val time: Long = 1_658_851_202
        val mailboxItem = buildMailboxItem(time = time)
        val result = TextUiModel.Text("18:00")
        every { formatMailboxItemTime.invoke(time.seconds) } returns result
        // When
        val actual = mapper.toUiModel(mailboxItem, ContactTestData.contacts, defaultFolderColorSettings, false)
        // Then
        assertEquals(result, actual.time)
    }

    @Test
    fun `when mailbox item of conversation type contains two or more messages show messages number`() = runTest {
        // Given
        val mailboxItem = buildMailboxItem(type = MailboxItemType.Conversation, numMessages = 2)
        // When
        val actual = mapper.toUiModel(mailboxItem, ContactTestData.contacts, defaultFolderColorSettings, false)
        // Then
        assertEquals(2, actual.numMessages)
    }

    @Test
    fun `when mailbox item contains less than two messages do not show messages number`() = runTest {
        // Given
        val mailboxItem = buildMailboxItem(type = MailboxItemType.Conversation, numMessages = 1)
        // When
        val actual = mapper.toUiModel(mailboxItem, ContactTestData.contacts, defaultFolderColorSettings, false)
        // Then
        assertNull(actual.numMessages)
    }

    @Test
    fun `when mailbox item is starred show starred`() = runTest {
        // Given
        val mailboxItem = buildMailboxItem(labelIds = listOf(SystemLabelId.Starred.labelId))
        // When
        val actual = mapper.toUiModel(mailboxItem, ContactTestData.contacts, defaultFolderColorSettings, false)
        // Then
        assertTrue(actual.showStar)
    }

    @Test
    fun `when mailbox item is not starred do not show starred`() = runTest {
        // Given
        val labelIds = listOf(SystemLabelId.Drafts.labelId, SystemLabelId.Archive.labelId)
        val mailboxItem = buildMailboxItem(labelIds = labelIds)
        // When
        val actual = mapper.toUiModel(mailboxItem, ContactTestData.contacts, defaultFolderColorSettings, false)
        // Then
        assertFalse(actual.showStar)
    }

    @Test
    fun `when use case returns location icons to be shown they are mapped to the ui model`() = runTest {
        // Given
        val labelIds = listOf(SystemLabelId.Inbox.labelId, SystemLabelId.Drafts.labelId)
        val mailboxItem = buildMailboxItem(type = MailboxItemType.Conversation, labelIds = labelIds)
        val inboxIconRes = MailboxItemLocationUiModel(R.drawable.ic_proton_inbox)
        val draftsIconRes = MailboxItemLocationUiModel(R.drawable.ic_proton_file_lines)
        val icons = GetMailboxItemLocationIcons.Result.Icons(inboxIconRes, draftsIconRes)
        coEvery { getMailboxItemLocationIcons.invoke(mailboxItem, defaultFolderColorSettings, false) } returns icons
        // When
        val actual = mapper.toUiModel(mailboxItem, ContactTestData.contacts, defaultFolderColorSettings, false)
        // Then
        val expectedIconsRes = listOf(inboxIconRes, draftsIconRes)
        assertEquals(expectedIconsRes, actual.locations)
    }

    @Test
    fun `when use case returns no location icons to be shown empty list is mapped to the ui model`() = runTest {
        // Given
        val mailboxItem = buildMailboxItem()
        coEvery {
            getMailboxItemLocationIcons.invoke(mailboxItem, defaultFolderColorSettings, false)
        } returns GetMailboxItemLocationIcons.Result.None
        // When
        val actual = mapper.toUiModel(mailboxItem, ContactTestData.contacts, defaultFolderColorSettings, false)
        // Then
        assertEquals(emptyList(), actual.locations)
    }

    @Test
    fun `when mailbox item has attachments show paperclip icon`() = runTest {
        // Given
        val mailboxItem = buildMailboxItem(hasAttachments = true)
        // When
        val actual = mapper.toUiModel(mailboxItem, ContactTestData.contacts, defaultFolderColorSettings, false)
        // Then
        assertTrue(actual.shouldShowAttachmentIcon)
    }

    @Test
    fun `when mailbox item has no attachments do not show paperclip icon`() = runTest {
        // Given
        val mailboxItem = buildMailboxItem(hasAttachments = false)
        // When
        val actual = mapper.toUiModel(mailboxItem, ContactTestData.contacts, defaultFolderColorSettings, false)
        // Then
        assertFalse(actual.shouldShowAttachmentIcon)
    }

    @Test
    fun `avatar ui model should be received from the use case`() = runTest {
        // Given
        val avatarUiModel = AvatarUiModel.ParticipantInitial(value = "T")
        val mailboxItem = buildMailboxItem()
        val resolvedNames = listOf(
            ResolveParticipantNameResult("contact name", isProton = false),
            ResolveParticipantNameResult("display name", isProton = false)
        )
        every {
            getParticipantsResolvedNames.invoke(mailboxItem, ContactTestData.contacts)
        } returns ParticipantsResolvedNamesResult.Senders(resolvedNames)
        every { mailboxAvatarUiModelMapper(mailboxItem, resolvedNames.map { it.name }) } returns avatarUiModel
        // When
        val actual = mapper.toUiModel(mailboxItem, ContactTestData.contacts, defaultFolderColorSettings, false)
        // Then
        assertEquals(avatarUiModel, actual.avatar)
    }

    @Test
    fun `when mailbox item has expiration time show expiration label`() = runTest {
        // Given
        val mailboxItem = buildMailboxItem(expirationTime = 1000L)
        // When
        val mailboxItemUiModel = mapper.toUiModel(
            mailboxItem, ContactTestData.contacts, defaultFolderColorSettings, false
        )
        // Then
        assertTrue(mailboxItemUiModel.shouldShowExpirationLabel)
    }

    @Test
    fun `when mailbox item has no expiration time don't show expiration label`() = runTest {
        // Given
        val mailboxItem = buildMailboxItem(expirationTime = 0L)
        // When
        val mailboxItemUiModel = mapper.toUiModel(
            mailboxItem, ContactTestData.contacts, defaultFolderColorSettings, false
        )
        // Then
        assertFalse(mailboxItemUiModel.shouldShowExpirationLabel)
    }

    @Test
    fun `when mailbox item has calendar attachments, show calendar icon`() = runTest {
        // Given
        val mailboxItem = buildMailboxItem(calendarAttachmentCount = 1)
        // When
        val mailboxItemUiModel = mapper.toUiModel(
            mailboxItem, ContactTestData.contacts, defaultFolderColorSettings, false
        )
        // Then
        assertTrue(mailboxItemUiModel.shouldShowCalendarIcon)
    }

    @Test
    fun `when mailbox item has no calendar attachments, don't show calendar icon`() = runTest {
        // Given
        val mailboxItem = buildMailboxItem(calendarAttachmentCount = 0)
        // When
        val mailboxItemUiModel = mapper.toUiModel(
            mailboxItem, ContactTestData.contacts, defaultFolderColorSettings, false
        )
        // Then
        assertFalse(mailboxItemUiModel.shouldShowCalendarIcon)
    }

    @Test
    fun `labels doesn't include folders and contacts groups`() = runTest {
        // given
        val mailboxItemLabels = listOf(
            LabelSample.build(labelId = LabelId("label"), type = LabelType.MessageLabel),
            LabelSample.build(labelId = LabelId("folder"), type = LabelType.MessageFolder),
            LabelSample.build(labelId = LabelId("group"), type = LabelType.ContactGroup)
        )
        val mailboxItem = buildMailboxItem(labels = mailboxItemLabels)

        // when
        val result = mapper.toUiModel(mailboxItem, ContactTestData.contacts, defaultFolderColorSettings, false)

        // then
        val expectedLabels = listOf(
            LabelUiModel(
                name = "label",
                color = Color.Unspecified,
                id = "label"
            )
        )
        assertEquals(expectedLabels, result.labels)
    }

    @Test
    fun `when mailbox item has all-drafts label, should open as draft is true`() = runTest {
        // Given
        val mailboxItem = buildMailboxItem(labelIds = listOf(SystemLabelId.AllDrafts.labelId))
        // When
        val mailboxItemUiModel = mapper.toUiModel(
            mailboxItem, ContactTestData.contacts, defaultFolderColorSettings, false
        )
        // Then
        assertTrue(mailboxItemUiModel.shouldOpenInComposer)
    }

    @Test
    fun `when mailbox item does not have all-drafts label, should open as draft is false`() = runTest {
        // Given
        val mailboxItem = buildMailboxItem(labelIds = listOf(SystemLabelId.AllMail.labelId))
        // When
        val mailboxItemUiModel = mapper.toUiModel(
            mailboxItem, ContactTestData.contacts, defaultFolderColorSettings, false
        )
        // Then
        assertFalse(mailboxItemUiModel.shouldOpenInComposer)
    }
}
