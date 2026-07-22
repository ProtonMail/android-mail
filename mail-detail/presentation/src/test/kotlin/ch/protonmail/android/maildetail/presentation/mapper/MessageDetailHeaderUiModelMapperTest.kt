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
import ch.protonmail.android.mailattachments.domain.model.AttachmentCount
import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.mailcommon.presentation.R.drawable.ic_proton_archive_box
import ch.protonmail.android.mailcommon.presentation.R.drawable.ic_proton_lock
import ch.protonmail.android.mailcommon.presentation.mapper.ColorMapper
import ch.protonmail.android.mailcommon.presentation.model.AvatarImageUiModel
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.sample.ParticipantAvatarSample
import ch.protonmail.android.mailcommon.presentation.usecase.FormatExtendedTime
import ch.protonmail.android.mailcommon.presentation.usecase.FormatShortTime
import ch.protonmail.android.maildetail.presentation.model.MessageDetailHeaderUiModel
import ch.protonmail.android.maildetail.presentation.model.MessageIdUiModel
import ch.protonmail.android.maildetail.presentation.model.MessageLocationUiModel
import ch.protonmail.android.maildetail.presentation.model.ParticipantUiModel
import ch.protonmail.android.maillabel.domain.model.CategoryLocation
import ch.protonmail.android.maillabel.domain.model.ExclusiveLocation
import ch.protonmail.android.maillabel.domain.sample.LabelSample
import ch.protonmail.android.maillabel.presentation.sample.LabelUiModelSample
import ch.protonmail.android.mailmessage.domain.model.AvatarImageState
import ch.protonmail.android.mailmessage.domain.model.Message
import ch.protonmail.android.mailmessage.domain.model.MessageTheme
import ch.protonmail.android.mailmessage.presentation.mapper.AvatarImageUiModelMapper
import ch.protonmail.android.mailmessage.presentation.model.ViewModePreference
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class MessageDetailHeaderUiModelMapperTest {

    private val primaryUserAddress = UserAddressSample.PrimaryAddress.email
    private val avatarUiModel = ParticipantAvatarSample.amazon
    private val messageLocationUiModel = MessageLocationUiModel(TextUiModel.Text("Archive"), ic_proton_archive_box)
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

    private val labels = listOf(
        LabelTestData.buildLabel(id = "id1"),
        LabelTestData.buildLabel(id = "id2"),
        LabelTestData.buildLabel(id = "id3")
    )
    private val message = MessageTestData.starredMessageInArchiveWithAttachments.copy(
        customLabels = labels
    )
    private val expectedResult = MessageDetailHeaderUiModel(
        avatar = avatarUiModel,
        avatarImage = AvatarImageUiModel.NoImageAvailable,
        sender = senderUiModel,
        shouldShowTrackerProtectionIcon = true,
        shouldShowAttachmentIcon = true,
        shouldShowStar = true,
        location = messageLocationUiModel,
        category = null,
        time = shortTimeTextUiModel,
        extendedTime = extendedTimeTestUiModel,
        shouldShowUndisclosedRecipients = false,
        allRecipients = listOf(participant1UiModel, participant2UiModel, participant3UiModel).toImmutableList(),
        toRecipients = listOf(participant1UiModel, participant2UiModel).toImmutableList(),
        ccRecipients = listOf(participant3UiModel).toImmutableList(),
        bccRecipients = emptyList<ParticipantUiModel>().toImmutableList(),
        labels = labels.map(LabelUiModelSample::build).toImmutableList(),
        size = "12 MB",
        encryptionPadlock = ic_proton_lock,
        encryptionInfo = "End-to-end encrypted and signed message",
        messageIdUiModel = MessageIdUiModel(message.messageId.id),
        themeOverride = null,
        shouldShowQuickReply = true,
        hasMoreActions = true,
        subject = "Subject"
    )

    private val colorMapper: ColorMapper = mockk {
        every { toColor(any()) } returns Color.Red.right()
    }
    private val context: Context = mockk()
    private val detailAvatarUiModelMapper: DetailAvatarUiModelMapper = mockk {
        every { this@mockk(any(), any(), any()) } returns avatarUiModel
    }
    private val formatExtendedTime: FormatExtendedTime = mockk {
        every { this@mockk(message.time.seconds) } returns extendedTimeTestUiModel
    }
    private val formatShortTime: FormatShortTime = mockk {
        every { this@mockk(message.time.seconds) } returns shortTimeTextUiModel
    }
    private val messageLocationUiModelMapper: MessageLocationUiModelMapper = mockk {
        coEvery { this@mockk(any<ExclusiveLocation>()) } returns messageLocationUiModel
        every { this@mockk(any<CategoryLocation>()) } returns null
    }
    private val participantUiModelMapper: ParticipantUiModelMapper = mockk {
        every { senderToUiModel(MessageTestData.sender) } returns senderUiModel
        every {
            recipientToUiModel(MessageTestData.recipient1, primaryUserAddress)
        } returns participant1UiModel
        every {
            recipientToUiModel(MessageTestData.recipient2, primaryUserAddress)
        } returns participant2UiModel
        every {
            recipientToUiModel(MessageTestData.recipient3, primaryUserAddress)
        } returns participant3UiModel
    }
    private val avatarImageUiModelMapper: AvatarImageUiModelMapper = mockk {
        every { this@mockk.toUiModel(any()) } returns AvatarImageUiModel.NoImageAvailable
    }

    private val messageDetailHeaderUiModelMapper = MessageDetailHeaderUiModelMapper(
        colorMapper = colorMapper,
        context = context,
        detailAvatarUiModelMapper = detailAvatarUiModelMapper,
        formatExtendedTime = formatExtendedTime,
        formatShortTime = formatShortTime,
        messageLocationUiModelMapper = messageLocationUiModelMapper,
        participantUiModelMapper = participantUiModelMapper,
        avatarImageUiModelMapper = avatarImageUiModelMapper
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
        // Given
        val expectedResult = expectedResult.copy(subject = message.subject)

        // When
        val result = messageDetailHeaderUiModelMapper.toUiModel(
            message = message,
            primaryUserAddress = primaryUserAddress,
            avatarImageState = AvatarImageState.NoImageAvailable,
            viewModePreference = ViewModePreference.ThemeDefault,
            hasMoreActionsEnabled = true
        )
        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when there are no attachments that are not calendar attachments, don't show attachment icon`() = runTest {
        // Given
        val message = message.copy(
            numAttachments = 1,
            attachmentCount = AttachmentCount(1)
        )
        val expectedResult = expectedResult.copy(shouldShowAttachmentIcon = false, subject = message.subject)
        // When
        val result = messageDetailHeaderUiModelMapper.toUiModel(
            message = message,
            primaryUserAddress = primaryUserAddress,
            avatarImageState = AvatarImageState.NoImageAvailable,
            viewModePreference = ViewModePreference.ThemeDefault,
            hasMoreActionsEnabled = true
        )
        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when the message is not starred, don't show star icon`() = runTest {
        // Given
        val message = message.copy(isStarred = false)
        val expectedResult = expectedResult.copy(shouldShowStar = false, subject = message.subject)
        // When
        val result = messageDetailHeaderUiModelMapper.toUiModel(
            message = message,
            primaryUserAddress = primaryUserAddress,
            avatarImageState = AvatarImageState.NoImageAvailable,
            viewModePreference = ViewModePreference.ThemeDefault,
            hasMoreActionsEnabled = true
        )
        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when TO, CC and BCC lists are empty, show undisclosed recipients`() = runTest {
        // Given
        val message = message.copy(
            toList = emptyList(),
            ccList = emptyList()
        )
        val expectedResult = expectedResult.copy(
            shouldShowUndisclosedRecipients = true,
            subject = message.subject,
            allRecipients = emptyList<ParticipantUiModel>().toImmutableList(),
            toRecipients = emptyList<ParticipantUiModel>().toImmutableList(),
            ccRecipients = emptyList<ParticipantUiModel>().toImmutableList()
        )
        // When
        val result = messageDetailHeaderUiModelMapper.toUiModel(
            message = message,
            primaryUserAddress = primaryUserAddress,
            avatarImageState = AvatarImageState.NoImageAvailable,
            viewModePreference = ViewModePreference.ThemeDefault,
            hasMoreActionsEnabled = true
        )
        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `ui models contains the correct labels, without folders`() = runTest {
        // Given
        val input = message.copy(
            customLabels = listOf(
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
        val avatarImageState = AvatarImageState.NoImageAvailable

        // When
        val result = messageDetailHeaderUiModelMapper.toUiModel(
            message = input,
            primaryUserAddress = primaryUserAddress,
            avatarImageState = avatarImageState,
            viewModePreference = ViewModePreference.ThemeDefault,
            hasMoreActionsEnabled = true
        )

        // Then
        assertEquals(expected, result.labels)
    }

    @Test
    fun `sender address should be shown in red when message is phishing and was not marked as legitimate`() {
        // Given
        val input = message.copy(flags = Message.FLAG_PHISHING_AUTO)
        every {
            participantUiModelMapper.senderToUiModel(MessageTestData.sender, isPhishing = true)
        } returns senderUiModel.copy(shouldShowAddressInRed = true)

        // When
        val result = messageDetailHeaderUiModelMapper.toUiModel(
            message = input,
            primaryUserAddress = primaryUserAddress,
            avatarImageState = AvatarImageState.NoImageAvailable,
            viewModePreference = ViewModePreference.ThemeDefault,
            hasMoreActionsEnabled = true
        )

        // Then
        assertTrue(result.sender.shouldShowAddressInRed)
    }

    @Test
    fun `sender address should not be shown in red when message is phishing but was marked as legitimate`() {
        // Given
        val input = message.copy(flags = Message.FLAG_PHISHING_AUTO.and(Message.FLAG_HAM_MANUAL))
        every {
            participantUiModelMapper.senderToUiModel(MessageTestData.sender, isPhishing = false)
        } returns senderUiModel.copy(shouldShowAddressInRed = false)

        // When
        val result = messageDetailHeaderUiModelMapper.toUiModel(
            message = input,
            primaryUserAddress = primaryUserAddress,
            avatarImageState = AvatarImageState.NoImageAvailable,
            viewModePreference = ViewModePreference.ThemeDefault,
            hasMoreActionsEnabled = true
        )

        // Then
        assertFalse(result.sender.shouldShowAddressInRed)
    }

    @Test
    fun `when view mode is LightMode, themeOverride is set to Light`() = runTest {
        // When
        val result = messageDetailHeaderUiModelMapper.toUiModel(
            message = message,
            primaryUserAddress = primaryUserAddress,
            avatarImageState = AvatarImageState.NoImageAvailable,
            viewModePreference = ViewModePreference.LightMode,
            hasMoreActionsEnabled = true
        )

        // Then
        assertEquals(MessageTheme.Light, result.themeOverride)
    }

    @Test
    fun `when view mode is DarkMode, themeOverride is set to Dark`() = runTest {
        // When
        val result = messageDetailHeaderUiModelMapper.toUiModel(
            message = message,
            primaryUserAddress = primaryUserAddress,
            avatarImageState = AvatarImageState.NoImageAvailable,
            viewModePreference = ViewModePreference.DarkMode,
            hasMoreActionsEnabled = true
        )

        // Then
        assertEquals(MessageTheme.Dark, result.themeOverride)
    }

    @Test
    fun `map label is outbox then has more actions is false`() = runTest {
        // When
        val result = messageDetailHeaderUiModelMapper.toUiModel(
            message = message,
            primaryUserAddress = primaryUserAddress,
            avatarImageState = AvatarImageState.NoImageAvailable,
            viewModePreference = ViewModePreference.ThemeDefault,
            hasMoreActionsEnabled = false
        )
        // Then
        assertFalse(result.hasMoreActions)
    }

    @Test
    fun `map label is not outbox then has more actions is true`() = runTest {
        // When
        val result = messageDetailHeaderUiModelMapper.toUiModel(
            message = message,
            primaryUserAddress = primaryUserAddress,
            avatarImageState = AvatarImageState.NoImageAvailable,
            viewModePreference = ViewModePreference.ThemeDefault,
            hasMoreActionsEnabled = true
        )
        // Then
        assertTrue(result.hasMoreActions)
    }

}
