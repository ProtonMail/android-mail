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

import java.util.UUID
import android.text.format.Formatter
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcommon.presentation.mapper.ColorMapper
import ch.protonmail.android.mailcommon.presentation.mapper.ExpirationTimeMapper
import ch.protonmail.android.mailcommon.presentation.model.AvatarImageUiModel
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.usecase.FormatExtendedTime
import ch.protonmail.android.mailcommon.presentation.usecase.FormatShortTime
import ch.protonmail.android.maildetail.domain.repository.InMemoryConversationStateRepository
import ch.protonmail.android.maildetail.presentation.mapper.rsvp.RsvpEventUiModelMapper
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel
import ch.protonmail.android.maildetail.presentation.model.RsvpEventUiModel
import ch.protonmail.android.maildetail.presentation.model.RsvpWidgetUiModel
import ch.protonmail.android.maildetail.presentation.sample.ConversationDetailMessageUiModelSample
import ch.protonmail.android.maildetail.presentation.sample.MessageDetailBodyUiModelSample
import ch.protonmail.android.maildetail.presentation.sample.MessageLocationUiModelSample
import ch.protonmail.android.maildetail.presentation.viewmodel.EmailBodyTestSamples
import ch.protonmail.android.maillabel.domain.model.CategoryLocation
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.usecase.ResolveSystemLabelId
import ch.protonmail.android.mailmessage.domain.model.AttachmentListExpandCollapseMode
import ch.protonmail.android.mailmessage.domain.model.AvatarImageState
import ch.protonmail.android.mailmessage.domain.model.DecryptedMessageBody
import ch.protonmail.android.mailmessage.domain.model.MessageBodyTransformations
import ch.protonmail.android.mailmessage.domain.model.MimeType
import ch.protonmail.android.mailmessage.domain.model.RsvpAnswer
import ch.protonmail.android.mailmessage.domain.model.RsvpEvent
import ch.protonmail.android.mailmessage.domain.sample.MessageSample
import ch.protonmail.android.mailmessage.domain.sample.RecipientSample
import ch.protonmail.android.mailmessage.domain.usecase.ResolveParticipantName
import ch.protonmail.android.mailmessage.presentation.mapper.AvatarImageUiModelMapper
import ch.protonmail.android.mailmessage.presentation.model.ViewModePreference
import ch.protonmail.android.mailmessage.presentation.model.attachment.AttachmentGroupUiModel
import ch.protonmail.android.mailmessage.presentation.sample.AttachmentMetadataUiModelSamples
import ch.protonmail.android.testdata.maildetail.MessageBannersUiModelTestData.messageBannersUiModel
import ch.protonmail.android.testdata.message.DecryptedMessageBodyTestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal class ConversationDetailMessageUiModelMapperTest {

    private val colorMapper: ColorMapper = mockk()
    private val primaryUserAddress = UserAddressSample.PrimaryAddress.email

    private val avatarUiModelMapper: DetailAvatarUiModelMapper = mockk {
        every {
            this@mockk(
                any(),
                any(),
                any()
            )
        } returns ConversationDetailMessageUiModelSample.AugWeatherForecast.avatar
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
        coEvery { this@mockk(messageLocation = any()) } returns MessageLocationUiModelSample.AllMail
        every { this@mockk(any<CategoryLocation>()) } returns null
    }
    private val avatarImageUiModelMapper: AvatarImageUiModelMapper = mockk {
        every { this@mockk.toUiModel(avatarImageState = any()) } returns AvatarImageUiModel.NoImageAvailable
    }
    private val resolveSystemLabelId: ResolveSystemLabelId = mockk {
        coEvery { this@mockk(userId = any(), labelId = any()) } returns SystemLabelId.Inbox.right()
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
            avatarImageUiModelMapper = avatarImageUiModelMapper
        )
    )
    private val messageDetailFooterUiModelMapper = spyk(MessageDetailFooterUiModelMapper())
    private val messageIdUiModelMapper = MessageIdUiModelMapper()
    private val messageBannersUiModelMapper = mockk<MessageBannersUiModelMapper> {
        every { toUiModel(any()) } returns messageBannersUiModel
    }

    private val userId = UserIdSample.Primary

    private val messageBodyUiModel = MessageDetailBodyUiModelSample.build(
        messageBody = EmailBodyTestSamples.BodyWithoutQuotes
    )
    private val messageBodyUiModelMapper: MessageBodyUiModelMapper = mockk {
        coEvery { toUiModel(any(), any()) } returns messageBodyUiModel
    }
    private val participantUiModelMapper: ParticipantUiModelMapper = mockk {
        every {
            senderToUiModel(RecipientSample.Doe)
        } returns ConversationDetailMessageUiModelSample
            .InvoiceWithoutLabelsCustomFolderExpanded
            .messageDetailHeaderUiModel
            .sender
        every {
            senderToUiModel(RecipientSample.John)
        } returns ConversationDetailMessageUiModelSample.ExpiringInvitation.sender
        every {
            senderToUiModel(RecipientSample.PreciWeather)
        } returns ConversationDetailMessageUiModelSample.AugWeatherForecast.sender
    }
    private val rsvpEventUiModelMapper = mockk<RsvpEventUiModelMapper>()
    private val mapper = ConversationDetailMessageUiModelMapper(
        avatarUiModelMapper = avatarUiModelMapper,
        expirationTimeMapper = expirationTimeMapper,
        formatShortTime = formatShortTime,
        colorMapper = colorMapper,
        messageLocationUiModelMapper = messageLocationUiModelMapper,
        messageDetailHeaderUiModelMapper = messageDetailHeaderUiModelMapper,
        messageDetailFooterUiModelMapper = messageDetailFooterUiModelMapper,
        messageBannersUiModelMapper = messageBannersUiModelMapper,
        messageBodyUiModelMapper = messageBodyUiModelMapper,
        participantUiModelMapper = participantUiModelMapper,
        messageIdUiModelMapper = messageIdUiModelMapper,
        avatarImageUiModelMapper = avatarImageUiModelMapper,
        rsvpEventUiModelMapper = rsvpEventUiModelMapper,
        resolveSystemLabelId = resolveSystemLabelId
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
        val message = MessageSample.AugWeatherForecast
        val expected = ConversationDetailMessageUiModelSample.AugWeatherForecast

        // when
        val result: ConversationDetailMessageUiModel.Collapsed = mapper.toUiModel(
            message = message,
            avatarImageState = AvatarImageState.NoImageAvailable,
            primaryUserAddress = primaryUserAddress
        )

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `map to ui model returns expanded model`() = runTest {
        // given
        val message = MessageSample.AugWeatherForecast
        val decryptedMessageBody = DecryptedMessageBody(
            message.messageId,
            UUID.randomUUID().toString(),
            isUnread = true,
            MimeType.Html,
            hasQuotedText = false,
            hasCalendarInvite = false,
            banners = emptyList(),
            transformations = MessageBodyTransformations.MessageDetailsDefaults
        )
        val avatarImageState = AvatarImageState.NoImageAvailable

        // when
        val result = mapper.toUiModel(
            message = message,
            avatarImageState = avatarImageState,
            primaryUserAddress = primaryUserAddress,
            decryptedMessageBody = decryptedMessageBody,
            attachmentListExpandCollapseMode = AttachmentListExpandCollapseMode.Collapsed,
            rsvpEventState = null,
            primaryUserId = userId
        )

        // then
        assertEquals(result.isUnread, message.isUnread)
        assertEquals(result.messageId.id, message.messageId.id)
        coVerify {
            messageDetailHeaderUiModelMapper.toUiModel(
                message, primaryUserAddress, avatarImageState, ViewModePreference.ThemeDefault, true
            )
        }
        coVerify {
            messageBodyUiModelMapper.toUiModel(decryptedMessageBody, AttachmentListExpandCollapseMode.Collapsed)
        }
    }

    @Test
    fun `when message is forwarded, ui model contains forwarded icon`() = runTest {
        // given
        val message = MessageSample.AugWeatherForecast.copy(isForwarded = true)
        val expected = with(ConversationDetailMessageUiModelSample) {
            AugWeatherForecast.copy(
                forwardedIcon = ConversationDetailMessageUiModel.ForwardedIcon.Forwarded
            )
        }
        val avatarImageState = AvatarImageState.NoImageAvailable

        // when
        val result = mapper.toUiModel(message, avatarImageState, primaryUserAddress)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `when message is replied, ui model contains replied icon`() = runTest {
        // given
        val message = MessageSample.AugWeatherForecast.copy(isReplied = true)
        val expected = with(ConversationDetailMessageUiModelSample) {
            AugWeatherForecast.copy(
                repliedIcon = ConversationDetailMessageUiModel.RepliedIcon.Replied
            )
        }
        val avatarImageState = AvatarImageState.NoImageAvailable

        // when
        val result = mapper.toUiModel(message, avatarImageState, primaryUserAddress)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `when message is replied all, ui model contains replied all icon`() = runTest {
        // given
        val message = MessageSample.AugWeatherForecast.copy(isRepliedAll = true)
        val expected = with(ConversationDetailMessageUiModelSample) {
            AugWeatherForecast.copy(
                repliedIcon = ConversationDetailMessageUiModel.RepliedIcon.RepliedAll
            )
        }
        val avatarImageState = AvatarImageState.NoImageAvailable

        // when
        val result = mapper.toUiModel(message, avatarImageState, primaryUserAddress)

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
        val expected = with(ConversationDetailMessageUiModelSample) {
            AugWeatherForecast.copy(
                repliedIcon = ConversationDetailMessageUiModel.RepliedIcon.RepliedAll
            )
        }
        val avatarImageState = AvatarImageState.NoImageAvailable

        // when
        val result = mapper.toUiModel(message, avatarImageState, primaryUserAddress)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `when message has expiration, ui model contains formatted time`() = runTest {
        // given
        val message = MessageSample.ExpiringInvitation
        val expected = ConversationDetailMessageUiModelSample.ExpiringInvitation
        every {
            avatarUiModelMapper(
                any(),
                any(),
                any()
            )
        } returns ConversationDetailMessageUiModelSample.ExpiringInvitation.avatar
        val avatarImageState = AvatarImageState.NoImageAvailable

        // when
        val result = mapper.toUiModel(message, avatarImageState, primaryUserAddress)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `when message has only calendar attachments, ui model has not attachments`() = runTest {
        // given
        val message = MessageSample.ExpiringInvitation
        val expected = ConversationDetailMessageUiModelSample.ExpiringInvitation
        every {
            avatarUiModelMapper(
                any(),
                any(),
                any()
            )
        } returns ConversationDetailMessageUiModelSample.ExpiringInvitation.avatar
        val avatarImageState = AvatarImageState.NoImageAvailable

        // when
        val result = mapper.toUiModel(message, avatarImageState, primaryUserAddress)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `when message is updated then unread and header is updated`() = runTest {
        // Given
        val previousMessage = ConversationDetailMessageUiModelSample.InvoiceWithoutLabelsCustomFolderExpanded
        val message = MessageSample.Invoice.copy(isUnread = true)
        val avatarImageState = AvatarImageState.NoImageAvailable

        // When
        val result = mapper.toUiModel(
            messageUiModel = previousMessage,
            message = message,
            avatarImageState = avatarImageState,
            userId = userId
        )

        // Then
        assertEquals(true, result.isUnread)
        assertNull(result.messageDetailHeaderUiModel.location.color)
    }

    @Test
    fun `when message is a draft then ui model draft is true`() = runTest {
        // given
        val message = MessageSample.AugWeatherForecast.copy(
            isDraft = true
        )
        val expected = with(ConversationDetailMessageUiModelSample) {
            AugWeatherForecast.copy(
                isDraft = true
            )
        }
        val avatarImageState = AvatarImageState.NoImageAvailable

        // when
        val result = mapper.toUiModel(message, avatarImageState, primaryUserAddress)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `map to ui model sets attachment expand collapse mode`() = runTest {
        // Given
        val message = MessageSample.AugWeatherForecast
        val decryptedMessageBody = DecryptedMessageBodyTestData.htmlInvoice.copy(
            messageId = message.messageId
        )
        val expectedAttachmentsUiModel = AttachmentGroupUiModel(
            attachments = listOf(
                AttachmentMetadataUiModelSamples.Document,
                AttachmentMetadataUiModelSamples.Invoice,
                AttachmentMetadataUiModelSamples.Image,
                AttachmentMetadataUiModelSamples.Video
            ),
            expandCollapseMode = AttachmentListExpandCollapseMode.Expanded
        )
        val expectedIds = expectedAttachmentsUiModel.attachments.map { it.id }

        val avatarImageState = AvatarImageState.NoImageAvailable
        val attachmentMode = AttachmentListExpandCollapseMode.Expanded

        coEvery {
            messageBodyUiModelMapper.toUiModel(
                decryptedMessageBody, AttachmentListExpandCollapseMode.Expanded
            )
        } returns messageBodyUiModel.copy(attachments = expectedAttachmentsUiModel)

        // When
        val result = mapper.toUiModel(
            message = message,
            avatarImageState = avatarImageState,
            primaryUserAddress = primaryUserAddress,
            decryptedMessageBody = decryptedMessageBody,
            attachmentListExpandCollapseMode = attachmentMode,
            rsvpEventState = null,
            primaryUserId = userId
        )

        // Then
        val resultAttachments = result.messageBodyUiModel.attachments
        assertNotNull(resultAttachments)
        assertEquals(attachmentMode, resultAttachments.expandCollapseMode)
        val actualIds = resultAttachments.attachments.map { it.id }
        assertEquals(expectedIds, actualIds)
    }

    @Test
    fun `map to ui model sets rsvp widget ui model loading`() = runTest {
        // Given
        val message = MessageSample.AugWeatherForecast
        val decryptedMessageBody = DecryptedMessageBody(
            message.messageId,
            UUID.randomUUID().toString(),
            isUnread = true,
            MimeType.Html,
            hasQuotedText = false,
            hasCalendarInvite = true,
            banners = emptyList(),
            transformations = MessageBodyTransformations.MessageDetailsDefaults
        )

        // When
        val result = mapper.toUiModel(
            message = message,
            avatarImageState = AvatarImageState.NoImageAvailable,
            primaryUserAddress = primaryUserAddress,
            decryptedMessageBody = decryptedMessageBody,
            attachmentListExpandCollapseMode = AttachmentListExpandCollapseMode.Collapsed,
            rsvpEventState = InMemoryConversationStateRepository.RsvpEventState.Loading,
            primaryUserId = userId
        )

        // then
        assertEquals(result.messageRsvpWidgetUiModel, RsvpWidgetUiModel.Loading)
    }

    @Test
    fun `map to ui model sets rsvp widget ui model error`() = runTest {
        // Given
        val message = MessageSample.AugWeatherForecast
        val decryptedMessageBody = DecryptedMessageBody(
            message.messageId,
            UUID.randomUUID().toString(),
            isUnread = true,
            MimeType.Html,
            hasQuotedText = false,
            hasCalendarInvite = true,
            banners = emptyList(),
            transformations = MessageBodyTransformations.MessageDetailsDefaults
        )

        // When
        val result = mapper.toUiModel(
            message = message,
            avatarImageState = AvatarImageState.NoImageAvailable,
            primaryUserAddress = primaryUserAddress,
            decryptedMessageBody = decryptedMessageBody,
            attachmentListExpandCollapseMode = AttachmentListExpandCollapseMode.Collapsed,
            rsvpEventState = InMemoryConversationStateRepository.RsvpEventState.Error,
            primaryUserId = userId
        )

        // then
        assertEquals(result.messageRsvpWidgetUiModel, RsvpWidgetUiModel.Error)
    }

    @Test
    fun `map to ui model sets rsvp widget ui model shown`() = runTest {
        // Given
        val message = MessageSample.AugWeatherForecast
        val decryptedMessageBody = DecryptedMessageBody(
            message.messageId,
            UUID.randomUUID().toString(),
            isUnread = true,
            MimeType.Html,
            hasQuotedText = false,
            hasCalendarInvite = true,
            banners = emptyList(),
            transformations = MessageBodyTransformations.MessageDetailsDefaults
        )
        val rsvpEvent = mockk<RsvpEvent>()
        val rsvpEventUiModel = mockk<RsvpEventUiModel>()
        every { rsvpEventUiModelMapper.toUiModel(rsvpEvent) } returns rsvpEventUiModel

        // When
        val result = mapper.toUiModel(
            message = message,
            avatarImageState = AvatarImageState.NoImageAvailable,
            primaryUserAddress = primaryUserAddress,
            decryptedMessageBody = decryptedMessageBody,
            attachmentListExpandCollapseMode = AttachmentListExpandCollapseMode.Collapsed,
            rsvpEventState = InMemoryConversationStateRepository.RsvpEventState.Shown(rsvpEvent),
            primaryUserId = userId
        )

        // then
        assertEquals(result.messageRsvpWidgetUiModel, RsvpWidgetUiModel.Shown(rsvpEventUiModel))
        verify { rsvpEventUiModelMapper.toUiModel(rsvpEvent) }
    }

    @Test
    fun `map to ui model sets rsvp widget ui model shown with answer`() = runTest {
        // Given
        val message = MessageSample.AugWeatherForecast
        val decryptedMessageBody = DecryptedMessageBody(
            message.messageId,
            UUID.randomUUID().toString(),
            isUnread = true,
            MimeType.Html,
            hasQuotedText = false,
            hasCalendarInvite = true,
            banners = emptyList(),
            transformations = MessageBodyTransformations.MessageDetailsDefaults
        )
        val rsvpAnswer = RsvpAnswer.Yes
        val rsvpEvent = mockk<RsvpEvent>()
        val rsvpEventUiModel = mockk<RsvpEventUiModel>()
        every { rsvpEventUiModelMapper.toUiModel(rsvpEvent, rsvpAnswer) } returns rsvpEventUiModel

        // When
        val result = mapper.toUiModel(
            message = message,
            avatarImageState = AvatarImageState.NoImageAvailable,
            primaryUserAddress = primaryUserAddress,
            decryptedMessageBody = decryptedMessageBody,
            attachmentListExpandCollapseMode = AttachmentListExpandCollapseMode.Collapsed,
            rsvpEventState = InMemoryConversationStateRepository.RsvpEventState.Answering(rsvpEvent, rsvpAnswer),
            primaryUserId = userId
        )

        // then
        assertEquals(result.messageRsvpWidgetUiModel, RsvpWidgetUiModel.Shown(rsvpEventUiModel))
        verify { rsvpEventUiModelMapper.toUiModel(rsvpEvent, rsvpAnswer) }
    }
}
