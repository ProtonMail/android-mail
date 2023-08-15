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
import ch.protonmail.android.mailcommon.presentation.mapper.ColorMapper
import ch.protonmail.android.mailcommon.presentation.mapper.ExpirationTimeMapper
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.usecase.FormatExtendedTime
import ch.protonmail.android.mailcommon.presentation.usecase.FormatShortTime
import ch.protonmail.android.mailmessage.domain.model.DecryptedMessageBody
import ch.protonmail.android.maildetail.domain.sample.MessageWithLabelsSample
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel
import ch.protonmail.android.maildetail.presentation.sample.ConversationDetailMessageUiModelSample
import ch.protonmail.android.maildetail.presentation.sample.MessageLocationUiModelSample
import ch.protonmail.android.mailmessage.domain.model.MimeType
import ch.protonmail.android.mailmessage.domain.sample.MessageSample
import ch.protonmail.android.mailmessage.domain.sample.RecipientSample
import ch.protonmail.android.mailmessage.domain.usecase.ResolveParticipantName
import ch.protonmail.android.mailsettings.domain.model.FolderColorSettings
import ch.protonmail.android.testdata.contact.ContactSample
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import java.util.UUID
import ch.protonmail.android.mailmessage.domain.usecase.ResolveParticipantNameResult
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class ConversationDetailMessageUiModelMapperTest {

    private val colorMapper: ColorMapper = mockk()
    private val folderColorSettings: FolderColorSettings = FolderColorSettings(useFolderColor = false)

    private val avatarUiModelMapper: DetailAvatarUiModelMapper = mockk {
        every { this@mockk(message = any(), senderResolvedName = any()) } returns
            ConversationDetailMessageUiModelSample.AugWeatherForecast.avatar
        every { this@mockk(message = MessageSample.ExpiringInvitation, senderResolvedName = any()) } returns
            ConversationDetailMessageUiModelSample.ExpiringInvitation.avatar
    }
    private val expirationTimeMapper: ExpirationTimeMapper = mockk {
        every { toUiModel(epochTime = any()) } returns
            requireNotNull(ConversationDetailMessageUiModelSample.ExpiringInvitation.expiration)
    }
    private val formatShortTime: FormatShortTime = mockk {
        every { this@mockk(itemTime = any()) } returns
            requireNotNull(ConversationDetailMessageUiModelSample.AugWeatherForecast.shortTime)
    }
    private val formatExtendedTime: FormatExtendedTime = mockk {
        every { this@mockk(duration = any()) } returns TextUiModel("Aug 1, 2021")
    }
    private val messageLocationUiModelMapper: MessageLocationUiModelMapper = mockk {
        coEvery {
            this@mockk(labelIds = any(), labels = any(), colorSettings = any())
        } returns MessageLocationUiModelSample.AllMail
    }
    private val resolveParticipantName: ResolveParticipantName = mockk {
        every {
            this@mockk(contacts = any(), participant = RecipientSample.Doe)
        } returns ResolveParticipantNameResult(ContactSample.Doe.name, isProton = false)
        every {
            this@mockk(contacts = any(), participant = RecipientSample.John)
        } returns ResolveParticipantNameResult(ContactSample.John.name, isProton = false)
        every {
            this@mockk(contacts = any(), participant = RecipientSample.PreciWeather)
        } returns ResolveParticipantNameResult(RecipientSample.PreciWeather.name, isProton = false)
    }
    private val messageDetailHeaderUiModelMapper = spyk(
        MessageDetailHeaderUiModelMapper(
            colorMapper = colorMapper,
            context = mockk(),
            detailAvatarUiModelMapper = avatarUiModelMapper,
            formatExtendedTime = formatExtendedTime,
            formatShortTime = formatShortTime,
            messageLocationUiModelMapper = messageLocationUiModelMapper,
            participantUiModelMapper = ParticipantUiModelMapper(ResolveParticipantName()),
            resolveParticipantName = resolveParticipantName
        )
    )
    private val messageBodyUiModelMapper: MessageBodyUiModelMapper = mockk {
        coEvery { toUiModel(any(), any<DecryptedMessageBody>()) } returns mockk()
    }
    private val participantUiModelMapper: ParticipantUiModelMapper = mockk {
        every {
            senderToUiModel(RecipientSample.Doe, any())
        } returns ConversationDetailMessageUiModelSample
            .InvoiceWithoutLabelsCustomFolderExpanded
            .messageDetailHeaderUiModel
            .sender
        every {
            senderToUiModel(RecipientSample.John, any())
        } returns ConversationDetailMessageUiModelSample.ExpiringInvitation.sender
        every {
            senderToUiModel(RecipientSample.PreciWeather, any())
        } returns ConversationDetailMessageUiModelSample.AugWeatherForecast.sender
    }
    private val mapper = ConversationDetailMessageUiModelMapper(
        avatarUiModelMapper = avatarUiModelMapper,
        expirationTimeMapper = expirationTimeMapper,
        formatShortTime = formatShortTime,
        colorMapper = colorMapper,
        messageLocationUiModelMapper = messageLocationUiModelMapper,
        resolveParticipantName = resolveParticipantName,
        messageDetailHeaderUiModelMapper = messageDetailHeaderUiModelMapper,
        messageBodyUiModelMapper = messageBodyUiModelMapper,
        participantUiModelMapper = participantUiModelMapper
    )

    @BeforeTest
    fun setUp() {
        mockkStatic(Formatter::class)
        every { Formatter.formatShortFileSize(any(), any()) } returns "12 MB"
    }

    @AfterTest
    fun tearDown() {
        unmockkStatic(Formatter::class)
    }

    @Test
    fun `map to ui model returns collapsed model`() = runTest {
        // given
        val messageWithLabels = MessageWithLabelsSample.AugWeatherForecast
        val expected = ConversationDetailMessageUiModelSample.AugWeatherForecast

        // when
        val result = mapper.toUiModel(
            messageWithLabels = messageWithLabels,
            contacts = emptyList(),
            folderColorSettings = folderColorSettings
        )

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `map to ui model returns expanded model`() = runTest {
        // given
        val messageWithLabels = MessageWithLabelsSample.AugWeatherForecast
        val contactsList = listOf(ContactSample.John, ContactSample.Doe)
        val decryptedMessageBody = DecryptedMessageBody(UUID.randomUUID().toString(), MimeType.Html)

        // when
        val result = mapper.toUiModel(
            messageWithLabels,
            contacts = contactsList,
            decryptedMessageBody = decryptedMessageBody,
            folderColorSettings = folderColorSettings
        )

        // then
        assertEquals(result.isUnread, messageWithLabels.message.unread)
        assertEquals(result.messageId, messageWithLabels.message.messageId)
        coVerify { messageDetailHeaderUiModelMapper.toUiModel(messageWithLabels, contactsList, folderColorSettings) }
        coVerify { messageBodyUiModelMapper.toUiModel(messageWithLabels.message.userId, decryptedMessageBody) }
    }

    @Test
    fun `when message is forwarded, ui model contains forwarded icon`() = runTest {
        // given
        val message = MessageSample.AugWeatherForecast.copy(isForwarded = true)
        val messageWithLabels = MessageWithLabelsSample.AugWeatherForecast.copy(message = message)
        val expected = with(ConversationDetailMessageUiModelSample) {
            AugWeatherForecast.copy(
                forwardedIcon = ConversationDetailMessageUiModel.ForwardedIcon.Forwarded
            )
        }

        // when
        val result = mapper.toUiModel(messageWithLabels, contacts = emptyList(), folderColorSettings)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `when message is replied, ui model contains replied icon`() = runTest {
        // given
        val message = MessageSample.AugWeatherForecast.copy(isReplied = true)
        val messageWithLabels = MessageWithLabelsSample.AugWeatherForecast.copy(message = message)
        val expected = with(ConversationDetailMessageUiModelSample) {
            AugWeatherForecast.copy(
                repliedIcon = ConversationDetailMessageUiModel.RepliedIcon.Replied
            )
        }

        // when
        val result = mapper.toUiModel(messageWithLabels, contacts = emptyList(), folderColorSettings)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `when message is replied all, ui model contains replied all icon`() = runTest {
        // given
        val message = MessageSample.AugWeatherForecast.copy(isRepliedAll = true)
        val messageWithLabels = MessageWithLabelsSample.AugWeatherForecast.copy(message = message)
        val expected = with(ConversationDetailMessageUiModelSample) {
            AugWeatherForecast.copy(
                repliedIcon = ConversationDetailMessageUiModel.RepliedIcon.RepliedAll
            )
        }

        // when
        val result = mapper.toUiModel(messageWithLabels, contacts = emptyList(), folderColorSettings)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `when message is replied and replied all, ui model contains replied all icon`() = runTest {
        // given
        val message = MessageSample.AugWeatherForecast.copy(
            isReplied = true,
            isRepliedAll = true
        )
        val messageWithLabels = MessageWithLabelsSample.AugWeatherForecast.copy(message = message)
        val expected = with(ConversationDetailMessageUiModelSample) {
            AugWeatherForecast.copy(
                repliedIcon = ConversationDetailMessageUiModel.RepliedIcon.RepliedAll
            )
        }

        // when
        val result = mapper.toUiModel(messageWithLabels, contacts = emptyList(), folderColorSettings)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `when message has expiration, ui model contains formatted time`() = runTest {
        // given
        val message = MessageSample.ExpiringInvitation
        val messageWithLabels = MessageWithLabelsSample.ExpiringInvitation.copy(message = message)
        val expected = ConversationDetailMessageUiModelSample.ExpiringInvitation

        // when
        val result = mapper.toUiModel(messageWithLabels, contacts = emptyList(), folderColorSettings)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `when message has only calendar attachments, ui model has not attachments`() = runTest {
        // given
        val message = MessageSample.ExpiringInvitation
        val messageWithLabels = MessageWithLabelsSample.ExpiringInvitation.copy(message = message)
        val expected = ConversationDetailMessageUiModelSample.ExpiringInvitation

        // when
        val result = mapper.toUiModel(messageWithLabels, contacts = emptyList(), folderColorSettings)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `when message is updated then unread and header is updated`() = runTest {
        // Given
        val previousMessage = ConversationDetailMessageUiModelSample.InvoiceWithoutLabelsCustomFolderExpanded

        val messageWithLabels = MessageWithLabelsSample.InvoiceWithoutLabels.copy(
            message = MessageWithLabelsSample.InvoiceWithoutLabels.message.copy(
                unread = true
            )
        )
        val folderColorSettings = FolderColorSettings(useFolderColor = false)

        // When
        val result = mapper.toUiModel(
            message = previousMessage,
            messageWithLabels = messageWithLabels,
            contacts = listOf(ContactSample.John),
            folderColorSettings = folderColorSettings
        )

        // Then
        assertEquals(true, result.isUnread)
        assertNull(result.messageDetailHeaderUiModel.location.color)
    }
}
