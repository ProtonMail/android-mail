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

import android.text.format.Formatter
import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import ch.protonmail.android.mailcommon.presentation.usecase.FormatExtendedTime
import ch.protonmail.android.mailcommon.presentation.usecase.FormatShortTime
import ch.protonmail.android.mailcommon.presentation.R.drawable.ic_proton_archive_box
import ch.protonmail.android.mailcommon.presentation.R.drawable.ic_proton_lock
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maildetail.domain.model.MessageWithLabels
import ch.protonmail.android.maildetail.presentation.model.MessageDetailHeaderUiModel
import ch.protonmail.android.maildetail.presentation.model.MessageLocationUiModel
import ch.protonmail.android.maildetail.presentation.model.ParticipantUiModel
import ch.protonmail.android.maildetail.presentation.R.string.undisclosed_recipients
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmessage.domain.entity.AttachmentCount
import ch.protonmail.android.mailmessage.domain.usecase.ResolveParticipantName
import ch.protonmail.android.testdata.contact.ContactTestData
import ch.protonmail.android.testdata.label.LabelTestData
import ch.protonmail.android.testdata.message.MessageTestData
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

class MessageDetailHeaderUiModelMapperTest {

    private val avatarUiModel = AvatarUiModel.ParticipantInitial("S")
    private val messageLocationUiModel = MessageLocationUiModel("Archive", ic_proton_archive_box)
    private val shortTimeTextUiModel = TextUiModel.Text("08/11/2022")
    private val extendedTimeTestUiModel = TextUiModel.Text("08/11/2022, 17:16")
    private val senderUiModel =
        ParticipantUiModel("Sender", "sender@pm.com", ic_proton_lock)
    private val participant1UiModel =
        ParticipantUiModel("Recipient1", "recipient1@pm.com", ic_proton_lock)
    private val participant2UiModel =
        ParticipantUiModel("Recipient2", "recipient2@pm.com", ic_proton_lock)
    private val participant3UiModel =
        ParticipantUiModel("Recipient3", "recipient3@pm.com", ic_proton_lock)

    private val message = MessageTestData.starredMessageInArchiveWithAttachments
    private val labels = listOf(
        LabelTestData.buildLabel(id = SystemLabelId.Archive.labelId.id),
        LabelTestData.buildLabel(id = SystemLabelId.AllMail.labelId.id),
        LabelTestData.buildLabel(id = SystemLabelId.Starred.labelId.id)
    )
    private val messageWithLabels = MessageWithLabels(
        message = message,
        labels = labels
    )
    private val expectedResult = MessageDetailHeaderUiModel(
        avatar = avatarUiModel,
        sender = senderUiModel,
        shouldShowTrackerProtectionIcon = false,
        shouldShowAttachmentIcon = true,
        shouldShowStar = true,
        location = messageLocationUiModel,
        time = shortTimeTextUiModel,
        extendedTime = extendedTimeTestUiModel,
        shouldShowUndisclosedRecipients = false,
        allRecipients = TextUiModel.Text("Recipient1, Recipient2, Recipient3"),
        toRecipients = listOf(participant1UiModel, participant2UiModel),
        ccRecipients = listOf(participant3UiModel),
        bccRecipients = emptyList(),
        labels = emptyList(),
        size = "12 MB",
        encryptionPadlock = ic_proton_lock,
        encryptionInfo = "End-to-end encrypted and signed message"
    )

    private val detailAvatarUiModelMapper: DetailAvatarUiModelMapper = mockk {
        every { this@mockk.invoke(any(), MessageTestData.sender.name) } returns avatarUiModel
    }
    private val formatExtendedTime: FormatExtendedTime = mockk {
        every { this@mockk.invoke(message.time.milliseconds) } returns extendedTimeTestUiModel
    }
    private val formatShortTime: FormatShortTime = mockk {
        every { this@mockk.invoke(message.time.milliseconds) } returns shortTimeTextUiModel
    }
    private val messageLocationUiModelMapper: MessageLocationUiModelMapper = mockk {
        every { this@mockk.invoke(any(), labels) } returns messageLocationUiModel
    }
    private val participantUiModelMapper: ParticipantUiModelMapper = mockk {
        every { this@mockk.toUiModel(MessageTestData.sender, ContactTestData.contacts) } returns senderUiModel
        every { this@mockk.toUiModel(MessageTestData.recipient1, ContactTestData.contacts) } returns participant1UiModel
        every { this@mockk.toUiModel(MessageTestData.recipient2, ContactTestData.contacts) } returns participant2UiModel
        every { this@mockk.toUiModel(MessageTestData.recipient3, ContactTestData.contacts) } returns participant3UiModel
    }
    private val resolveParticipantName: ResolveParticipantName = mockk {
        every {
            this@mockk.invoke(
                MessageTestData.sender,
                ContactTestData.contacts
            )
        } returns MessageTestData.sender.name
        every {
            this@mockk.invoke(
                MessageTestData.recipient1,
                ContactTestData.contacts
            )
        } returns MessageTestData.recipient1.name
        every {
            this@mockk.invoke(
                MessageTestData.recipient2,
                ContactTestData.contacts
            )
        } returns MessageTestData.recipient2.name
        every {
            this@mockk.invoke(
                MessageTestData.recipient3,
                ContactTestData.contacts
            )
        } returns MessageTestData.recipient3.name
    }

    private val messageDetailHeaderUiModelMapper = MessageDetailHeaderUiModelMapper(
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
    fun `map to ui model returns a correct model`() {
        // When
        val result = messageDetailHeaderUiModelMapper.toUiModel(messageWithLabels, ContactTestData.contacts)
        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when there are no attachments that are not calendar attachments, don't show attachment icon`() {
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
        val result = messageDetailHeaderUiModelMapper.toUiModel(messageWithLabels, ContactTestData.contacts)
        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when the message is not starred, don't show star icon`() {
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
        val result = messageDetailHeaderUiModelMapper.toUiModel(messageWithLabels, ContactTestData.contacts)
        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when TO, CC and BCC lists are empty, show undisclosed recipients`() {
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
            toRecipients = emptyList(),
            ccRecipients = emptyList()
        )
        // When
        val result = messageDetailHeaderUiModelMapper.toUiModel(messageWithLabels, ContactTestData.contacts)
        // Then
        assertEquals(expectedResult, result)
    }
}
