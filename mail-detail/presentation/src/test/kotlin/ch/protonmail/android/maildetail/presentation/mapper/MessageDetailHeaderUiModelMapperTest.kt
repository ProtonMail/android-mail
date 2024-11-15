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

package ch.protonmail.android.maildetail.presentation.mapper

import android.content.Context
import android.text.format.Formatter
import androidx.compose.ui.graphics.Color
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.sample.LabelSample
import ch.protonmail.android.mailcommon.presentation.R.drawable.ic_proton_archive_box
import ch.protonmail.android.mailcommon.presentation.R.drawable.ic_proton_lock
import ch.protonmail.android.mailcommon.presentation.mapper.ColorMapper
import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.usecase.FormatExtendedTime
import ch.protonmail.android.mailcommon.presentation.usecase.FormatShortTime
import ch.protonmail.android.maildetail.presentation.R.string.undisclosed_recipients
import ch.protonmail.android.maildetail.presentation.model.MessageDetailHeaderUiModel
import ch.protonmail.android.maildetail.presentation.model.MessageIdUiModel
import ch.protonmail.android.maildetail.presentation.model.MessageLocationUiModel
import ch.protonmail.android.maildetail.presentation.model.ParticipantUiModel
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.presentation.sample.LabelUiModelSample
import ch.protonmail.android.mailmessage.domain.model.AttachmentCount
import ch.protonmail.android.mailmessage.domain.model.MessageWithLabels
import ch.protonmail.android.mailmessage.domain.usecase.ResolveParticipantName
import ch.protonmail.android.mailmessage.domain.usecase.ResolveParticipantNameResult
import ch.protonmail.android.mailsettings.domain.model.AutoDeleteSetting
import ch.protonmail.android.mailsettings.domain.model.FolderColorSettings
import ch.protonmail.android.testdata.contact.ContactTestData
import ch.protonmail.android.testdata.label.LabelTestData
import ch.protonmail.android.testdata.message.MessageTestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class MessageDetailHeaderUiModelMapperTest {

    private val folderColorSettings = FolderColorSettings(useFolderColor = false)
    private val autoDeleteSetting: AutoDeleteSetting = AutoDeleteSetting.Disabled

    private val avatarUiModel = AvatarUiModel.ParticipantInitial("S")
    private val messageLocationUiModel = MessageLocationUiModel("Archive", ic_proton_archive_box)
    private val shortTimeTextUiModel = TextUiModel.Text("08/11/2022")
    private val extendedTimeTestUiModel = TextUiModel.Text("08/11/2022, 17:16")
    private val senderUiModel =
        ParticipantUiModel("Sender", "sender@pm.com", ic_proton_lock, shouldShowOfficialBadge = false)
    private val participant1UiModel =
        ParticipantUiModel("Recipient1", "recipient1@pm.com", ic_proton_lock, shouldShowOfficialBadge = false)
    private val participant2UiModel =
        ParticipantUiModel("Recipient2", "recipient2@pm.com", ic_proton_lock, shouldShowOfficialBadge = false)
    private val participant3UiModel =
        ParticipantUiModel("Recipient3", "recipient3@pm.com", ic_proton_lock, shouldShowOfficialBadge = false)

    private val message = MessageTestData.starredMessageInArchiveWithAttachments
    private val labels = listOf(
        LabelTestData.buildLabel(id = "id1"),
        LabelTestData.buildLabel(id = "id2"),
        LabelTestData.buildLabel(id = "id3")
    )
    private val messageWithLabels = MessageWithLabels(
        message = message,
        labels = labels
    )
    private val expectedResult = MessageDetailHeaderUiModel(
        avatar = avatarUiModel,
        sender = senderUiModel,
        shouldShowTrackerProtectionIcon = true,
        shouldShowAttachmentIcon = true,
        shouldShowStar = true,
        location = messageLocationUiModel,
        time = shortTimeTextUiModel,
        extendedTime = extendedTimeTestUiModel,
        shouldShowUndisclosedRecipients = false,
        allRecipients = TextUiModel.Text("Recipient1, Recipient2, Recipient3"),
        toRecipients = listOf(participant1UiModel, participant2UiModel).toImmutableList(),
        ccRecipients = listOf(participant3UiModel).toImmutableList(),
        bccRecipients = emptyList<ParticipantUiModel>().toImmutableList(),
        labels = labels.map(LabelUiModelSample::build).toImmutableList(),
        size = "12 MB",
        encryptionPadlock = ic_proton_lock,
        encryptionInfo = "End-to-end encrypted and signed message",
        messageIdUiModel = MessageIdUiModel(message.id)
    )

    private val colorMapper: ColorMapper = mockk {
        every { toColor(any()) } returns Color.Red.right()
    }
    private val context: Context = mockk()
    private val detailAvatarUiModelMapper: DetailAvatarUiModelMapper = mockk {
        every { this@mockk(any(), MessageTestData.sender.name) } returns avatarUiModel
    }
    private val formatExtendedTime: FormatExtendedTime = mockk {
        every { this@mockk(message.time.seconds) } returns extendedTimeTestUiModel
    }
    private val formatShortTime: FormatShortTime = mockk {
        every { this@mockk(message.time.seconds) } returns shortTimeTextUiModel
    }
    private val messageLocationUiModelMapper: MessageLocationUiModelMapper = mockk {
        coEvery { this@mockk(any(), any(), any(), any()) } returns messageLocationUiModel
    }
    private val participantUiModelMapper: ParticipantUiModelMapper = mockk {
        every { senderToUiModel(MessageTestData.sender, ContactTestData.contacts) } returns senderUiModel
        every { recipientToUiModel(MessageTestData.recipient1, ContactTestData.contacts) } returns participant1UiModel
        every { recipientToUiModel(MessageTestData.recipient2, ContactTestData.contacts) } returns participant2UiModel
        every { recipientToUiModel(MessageTestData.recipient3, ContactTestData.contacts) } returns participant3UiModel
    }
    private val resolveParticipantName: ResolveParticipantName = mockk {
        every {
            this@mockk.invoke(
                MessageTestData.sender,
                ContactTestData.contacts
            )
        } returns ResolveParticipantNameResult(MessageTestData.sender.name, isProton = false)
        every {
            this@mockk.invoke(
                MessageTestData.recipient1,
                ContactTestData.contacts
            )
        } returns ResolveParticipantNameResult(MessageTestData.recipient1.name, isProton = false)
        every {
            this@mockk.invoke(
                MessageTestData.recipient2,
                ContactTestData.contacts
            )
        } returns ResolveParticipantNameResult(MessageTestData.recipient2.name, isProton = false)
        every {
            this@mockk.invoke(
                MessageTestData.recipient3,
                ContactTestData.contacts
            )
        } returns ResolveParticipantNameResult(MessageTestData.recipient3.name, isProton = false)
    }

    private val messageDetailHeaderUiModelMapper = MessageDetailHeaderUiModelMapper(
        colorMapper = colorMapper,
        context = context,
        detailAvatarUiModelMapper = detailAvatarUiModelMapper,
        formatExtendedTime = formatExtendedTime,
        formatShortTime = formatShortTime,
        messageLocationUiModelMapper = messageLocationUiModelMapper,
        participantUiModelMapper = participantUiModelMapper,
        resolveParticipantName = resolveParticipantName
    )

    @BeforeTest
    fun setUp() {
        mockkStatic(Formatter::class)
        every { Formatter.formatShortFileSize(any(), any()) } returns "12 MB"
    }

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `map to ui model returns a correct model`() = runTest {
        // When
        val result = messageDetailHeaderUiModelMapper.toUiModel(
            messageWithLabels = messageWithLabels,
            contacts = ContactTestData.contacts,
            folderColorSettings = folderColorSettings,
            autoDeleteSetting = autoDeleteSetting
        )
        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when there are no attachments that are not calendar attachments, don't show attachment icon`() = runTest {
        // Given
        val messageWithLabels =
            messageWithLabels.copy(
                message = message.copy(
                    numAttachments = 1,
                    attachmentCount = AttachmentCount(1)
                )
            )
        val expectedResult = expectedResult.copy(shouldShowAttachmentIcon = false)
        // When
        val result = messageDetailHeaderUiModelMapper.toUiModel(
            messageWithLabels = messageWithLabels,
            contacts = ContactTestData.contacts,
            folderColorSettings = folderColorSettings,
            autoDeleteSetting = autoDeleteSetting
        )
        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when the message is not starred, don't show star icon`() = runTest {
        // Given
        val messageWithLabels = messageWithLabels.copy(
            message = message.copy(
                labelIds = listOf(
                    SystemLabelId.Archive.labelId,
                    SystemLabelId.AllMail.labelId
                )
            )
        )
        val expectedResult = expectedResult.copy(shouldShowStar = false)
        // When
        val result = messageDetailHeaderUiModelMapper.toUiModel(
            messageWithLabels = messageWithLabels,
            contacts = ContactTestData.contacts,
            folderColorSettings = folderColorSettings,
            autoDeleteSetting = autoDeleteSetting
        )
        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when TO, CC and BCC lists are empty, show undisclosed recipients`() = runTest {
        // Given
        val messageWithLabels = messageWithLabels.copy(
            message = message.copy(
                toList = emptyList(),
                ccList = emptyList()
            )
        )
        val expectedResult = expectedResult.copy(
            shouldShowUndisclosedRecipients = true,
            allRecipients = TextUiModel.TextRes(undisclosed_recipients),
            toRecipients = emptyList<ParticipantUiModel>().toImmutableList(),
            ccRecipients = emptyList<ParticipantUiModel>().toImmutableList()
        )
        // When
        val result = messageDetailHeaderUiModelMapper.toUiModel(
            messageWithLabels = messageWithLabels,
            contacts = ContactTestData.contacts,
            folderColorSettings = folderColorSettings,
            autoDeleteSetting = autoDeleteSetting
        )
        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `ui models contains the correct labels, without folders`() = runTest {
        // Given
        val input = messageWithLabels.copy(
            labels = listOf(
                LabelSample.Archive,
                LabelSample.Document,
                LabelSample.Inbox,
                LabelSample.News
            )
        )
        val expected = listOf(
            LabelUiModelSample.Document,
            LabelUiModelSample.News
        )

        // When
        val result = messageDetailHeaderUiModelMapper.toUiModel(
            input,
            ContactTestData.contacts,
            folderColorSettings,
            autoDeleteSetting
        )

        // Then
        assertEquals(expected, result.labels)
    }
}
