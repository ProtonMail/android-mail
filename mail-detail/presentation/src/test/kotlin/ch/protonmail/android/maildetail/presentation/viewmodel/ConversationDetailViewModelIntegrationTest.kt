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

package ch.protonmail.android.maildetail.presentation.viewmodel

import java.io.ByteArrayInputStream
import java.util.Random
import android.content.Context
import android.net.Uri
import android.text.format.Formatter
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.Event
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import arrow.core.NonEmptyList
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.ConversationIdSample
import ch.protonmail.android.mailcommon.domain.sample.LabelIdSample
import ch.protonmail.android.mailcommon.domain.sample.LabelSample
import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcommon.domain.usecase.GetCurrentEpochTimeDuration
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.mapper.ActionUiModelMapper
import ch.protonmail.android.mailcommon.presentation.mapper.ColorMapper
import ch.protonmail.android.mailcommon.presentation.mapper.ExpirationTimeMapper
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.reducer.BottomBarReducer
import ch.protonmail.android.mailcommon.presentation.ui.delete.DeleteDialogState
import ch.protonmail.android.mailcommon.presentation.usecase.FormatExtendedTime
import ch.protonmail.android.mailcommon.presentation.usecase.FormatShortTime
import ch.protonmail.android.mailcommon.presentation.usecase.GetInitial
import ch.protonmail.android.mailcontact.domain.usecase.ObserveContacts
import ch.protonmail.android.mailconversation.domain.sample.ConversationLabelSample
import ch.protonmail.android.mailconversation.domain.sample.ConversationSample
import ch.protonmail.android.mailconversation.domain.usecase.DeleteConversations
import ch.protonmail.android.mailconversation.domain.usecase.ObserveConversation
import ch.protonmail.android.mailconversation.domain.usecase.StarConversations
import ch.protonmail.android.mailconversation.domain.usecase.UnStarConversations
import ch.protonmail.android.maildetail.domain.model.OpenAttachmentIntentValues
import ch.protonmail.android.maildetail.domain.model.OpenProtonCalendarIntentValues.OpenIcsInProtonCalendar
import ch.protonmail.android.maildetail.domain.model.OpenProtonCalendarIntentValues.OpenProtonCalendarOnPlayStore
import ch.protonmail.android.maildetail.domain.usecase.DoesMessageBodyHaveEmbeddedImages
import ch.protonmail.android.maildetail.domain.usecase.DoesMessageBodyHaveRemoteContent
import ch.protonmail.android.maildetail.domain.usecase.GetAttachmentIntentValues
import ch.protonmail.android.maildetail.domain.usecase.GetDownloadingAttachmentsForMessages
import ch.protonmail.android.maildetail.domain.usecase.IsProtonCalendarInstalled
import ch.protonmail.android.maildetail.domain.usecase.MarkConversationAsUnread
import ch.protonmail.android.maildetail.domain.usecase.MarkMessageAndConversationReadIfAllMessagesRead
import ch.protonmail.android.maildetail.domain.usecase.MoveConversation
import ch.protonmail.android.maildetail.domain.usecase.ObserveConversationDetailActions
import ch.protonmail.android.maildetail.domain.usecase.ObserveConversationMessagesWithLabels
import ch.protonmail.android.maildetail.domain.usecase.ObserveConversationViewState
import ch.protonmail.android.maildetail.domain.usecase.ObserveMessageAttachmentStatus
import ch.protonmail.android.maildetail.domain.usecase.RelabelConversation
import ch.protonmail.android.maildetail.domain.usecase.SetMessageViewState
import ch.protonmail.android.maildetail.domain.usecase.ShouldShowEmbeddedImages
import ch.protonmail.android.maildetail.domain.usecase.ShouldShowRemoteContent
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.mapper.ConversationDetailMessageUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.ConversationDetailMetadataUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.DetailAvatarUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.MessageBannersUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.MessageBodyUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.MessageDetailHeaderUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.MessageIdUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.MessageLocationUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.ParticipantUiModelMapper
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel.Collapsed
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel.Expanded
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel.Expanding
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.ExpandMessage
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.OnAttachmentClicked
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.RequestScrollTo
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.ShowAllAttachmentsForMessage
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailsMessagesState
import ch.protonmail.android.maildetail.presentation.model.ReportPhishingDialogState
import ch.protonmail.android.maildetail.presentation.reducer.ConversationDeleteDialogReducer
import ch.protonmail.android.maildetail.presentation.reducer.ConversationDetailMessagesReducer
import ch.protonmail.android.maildetail.presentation.reducer.ConversationDetailMetadataReducer
import ch.protonmail.android.maildetail.presentation.reducer.ConversationDetailReducer
import ch.protonmail.android.maildetail.presentation.reducer.ConversationReportPhishingDialogReducer
import ch.protonmail.android.maildetail.presentation.sample.ConversationDetailMessageUiModelSample
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailScreen
import ch.protonmail.android.maildetail.presentation.usecase.ExtractMessageBodyWithoutQuote
import ch.protonmail.android.maildetail.presentation.usecase.GetEmbeddedImageAvoidDuplicatedExecution
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabels
import ch.protonmail.android.maillabel.domain.usecase.GetRootLabel
import ch.protonmail.android.maillabel.domain.usecase.ObserveCustomMailLabels
import ch.protonmail.android.maillabel.domain.usecase.ObserveExclusiveDestinationMailLabels
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.DecryptedMessageBody
import ch.protonmail.android.mailmessage.domain.model.GetDecryptedMessageBodyError
import ch.protonmail.android.mailmessage.domain.model.MessageAttachment
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageWithLabels
import ch.protonmail.android.mailmessage.domain.model.MimeType
import ch.protonmail.android.mailmessage.domain.sample.MessageAttachmentSample
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageSample
import ch.protonmail.android.mailmessage.domain.sample.MessageWithLabelsSample
import ch.protonmail.android.mailmessage.domain.usecase.GetDecryptedMessageBody
import ch.protonmail.android.mailmessage.domain.usecase.GetEmbeddedImageResult
import ch.protonmail.android.mailmessage.domain.usecase.ObserveMessage
import ch.protonmail.android.maildetail.domain.usecase.ReportPhishingMessage
import ch.protonmail.android.mailmessage.domain.usecase.ResolveParticipantName
import ch.protonmail.android.mailmessage.presentation.mapper.AttachmentUiModelMapper
import ch.protonmail.android.mailmessage.presentation.mapper.DetailMoreActionsBottomSheetUiMapper
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyExpandCollapseMode
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.DetailMoreActionsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.reducer.BottomSheetReducer
import ch.protonmail.android.mailmessage.presentation.reducer.DetailMoreActionsBottomSheetReducer
import ch.protonmail.android.mailmessage.presentation.reducer.LabelAsBottomSheetReducer
import ch.protonmail.android.mailmessage.presentation.reducer.MailboxMoreActionsBottomSheetReducer
import ch.protonmail.android.mailmessage.presentation.reducer.MoveToBottomSheetReducer
import ch.protonmail.android.mailmessage.presentation.usecase.InjectCssIntoDecryptedMessageBody
import ch.protonmail.android.mailmessage.presentation.usecase.SanitizeHtmlOfDecryptedMessageBody
import ch.protonmail.android.mailsettings.domain.model.FolderColorSettings
import ch.protonmail.android.mailsettings.domain.model.PrivacySettings
import ch.protonmail.android.mailsettings.domain.usecase.ObserveFolderColorSettings
import ch.protonmail.android.mailsettings.domain.usecase.privacy.ObservePrivacySettings
import ch.protonmail.android.mailsettings.domain.usecase.privacy.UpdateLinkConfirmationSetting
import ch.protonmail.android.testdata.maillabel.MailLabelTestData
import ch.protonmail.android.testdata.message.MessageAttachmentMetadataTestData
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.contact.domain.entity.Contact
import me.proton.core.network.domain.NetworkManager
import me.proton.core.network.domain.NetworkStatus
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration

class ConversationDetailViewModelIntegrationTest {

    private val userId = UserIdSample.Primary
    private val conversationId = ConversationIdSample.WeatherForecast
    private val defaultFolderColorSettings = FolderColorSettings()

    // region mock observe use cases
    private val observeContacts: ObserveContacts = mockk {
        every { this@mockk(userId = UserIdSample.Primary) } returns flowOf(emptyList<Contact>().right())
    }
    private val observeConversationUseCase: ObserveConversation = mockk {
        every { this@mockk(UserIdSample.Primary, ConversationIdSample.WeatherForecast, any()) } returns
            flowOf(ConversationSample.WeatherForecast.right())
    }
    private val observeMessage = mockk<ObserveMessage>()
    private val observeConversationMessagesWithLabels: ObserveConversationMessagesWithLabels = mockk {
        every { this@mockk(UserIdSample.Primary, ConversationIdSample.WeatherForecast) } returns flowOf(
            nonEmptyListOf(
                MessageWithLabelsSample.InvoiceWithLabel,
                MessageWithLabelsSample.InvoiceWithTwoLabels
            ).right()
        )
    }
    private val observeConversationDetailActions = mockk<ObserveConversationDetailActions> {
        every { this@mockk(UserIdSample.Primary, ConversationIdSample.WeatherForecast, any()) } returns flowOf(
            listOf(Action.Archive, Action.MarkUnread).right()
        )
    }
    private val observePrimaryUserId: ObservePrimaryUserId = mockk {
        every { this@mockk() } returns flowOf(UserIdSample.Primary)
    }
    private val observeMailLabels = mockk<ObserveExclusiveDestinationMailLabels> {
        every { this@mockk.invoke(UserIdSample.Primary) } returns flowOf(
            MailLabels(
                systemLabels = listOf(MailLabel.System(MailLabelId.System.Spam)),
                folders = listOf(MailLabelTestData.buildCustomFolder(id = "folder1")),
                labels = listOf()
            )
        )
    }
    private val observeFolderColorSettings =
        mockk<ObserveFolderColorSettings> {
            every { this@mockk.invoke(UserIdSample.Primary) } returns flowOf(defaultFolderColorSettings)
        }
    private val observeCustomMailLabelsUseCase = mockk<ObserveCustomMailLabels> {
        every { this@mockk.invoke(UserIdSample.Primary) } returns flowOf(
            MailLabelTestData.listOfCustomLabels.right()
        )
    }
    private val reportPhishingMessage = mockk<ReportPhishingMessage>()

    // Privacy settings for link confirmation dialog
    private val observePrivacySettings = mockk<ObservePrivacySettings> {
        coEvery { this@mockk.invoke(any()) } returns flowOf(
            PrivacySettings(
                autoShowRemoteContent = false,
                autoShowEmbeddedImages = false,
                preventTakingScreenshots = false,
                requestLinkConfirmation = false,
                allowBackgroundSync = false
            ).right()
        )
    }
    private val updateLinkConfirmationSetting = mockk<UpdateLinkConfirmationSetting>()

    private val observeAttachmentStatus = mockk<ObserveMessageAttachmentStatus>()
    private val getDownloadingAttachmentsForMessages = mockk<GetDownloadingAttachmentsForMessages>()
    private val getAttachmentIntentValues = mockk<GetAttachmentIntentValues>()
    private val getEmbeddedImageAvoidDuplicatedExecution = mockk<GetEmbeddedImageAvoidDuplicatedExecution>()
    // endregion

    // region mock action use cases
    private val markConversationAsUnread: MarkConversationAsUnread = mockk()
    private val move: MoveConversation = mockk()
    private val relabelConversation: RelabelConversation = mockk()
    private val deleteConversations: DeleteConversations = mockk()
    private val savedStateHandle: SavedStateHandle = mockk {
        every { get<String>(ConversationDetailScreen.ConversationIdKey) } returns conversationId.id
        every { get<String>(ConversationDetailScreen.ScrollToMessageIdKey) } returns "null"
    }
    private val starConversations: StarConversations = mockk()
    private val unStarConversations: UnStarConversations = mockk()
    private val getDecryptedMessageBody: GetDecryptedMessageBody = mockk {
        coEvery { this@mockk.invoke(any(), any()) } returns DecryptedMessageBody(
            MessageId("default"), "", MimeType.Html, emptyList(), UserAddressSample.PrimaryAddress
        ).right()
    }

    private val markMessageAndConversationReadIfAllRead: MarkMessageAndConversationReadIfAllMessagesRead =
        mockk {
            coEvery { this@mockk.invoke(any(), any(), any()) } returns Unit.right()
        }
    private val getCurrentEpochTimeDuration: GetCurrentEpochTimeDuration = mockk {
        coEvery { this@mockk.invoke() } returns Duration.parse("PT0S")
    }
    private val getRootLabel: GetRootLabel = mockk()
    private val shouldShowEmbeddedImages = mockk<ShouldShowEmbeddedImages> {
        coEvery { this@mockk.invoke(userId) } returns true
    }
    private val shouldShowRemoteContent = mockk<ShouldShowRemoteContent> {
        coEvery { this@mockk.invoke(userId) } returns true
    }
    private val isProtonCalendarInstalled = mockk<IsProtonCalendarInstalled>()

    private val messageIdUiModelMapper = MessageIdUiModelMapper()
    private val attachmentUiModelMapper = AttachmentUiModelMapper()
    private val doesMessageBodyHaveEmbeddedImages = DoesMessageBodyHaveEmbeddedImages()
    private val doesMessageBodyHaveRemoteContent = DoesMessageBodyHaveRemoteContent()
    // endregion

    // region mappers
    private val actionUiModelMapper = ActionUiModelMapper()
    private val colorMapper = ColorMapper()
    private val resolveParticipantName = ResolveParticipantName()
    private val messageLocationUiModelMapper = MessageLocationUiModelMapper(colorMapper, getRootLabel)
    private val formatShortTime: FormatShortTime =
        mockk { every { this@mockk.invoke(any()) } returns TextUiModel("10:00") }
    private val formatExtendedTime: FormatExtendedTime =
        mockk { every { this@mockk.invoke(any()) } returns TextUiModel("10:00") }
    private val getInitial = GetInitial()
    private val context = mockk<Context> {
        every { resources } returns mockk {
            every {
                openRawResource(R.raw.css_reset_with_media_scheme_plus_custom_props)
            } returns ByteArrayInputStream("".toByteArray())
        }
    }
    private val injectCssIntoDecryptedMessageBody = InjectCssIntoDecryptedMessageBody(context)
    private val sanitizeHtmlOfDecryptedMessageBody = SanitizeHtmlOfDecryptedMessageBody()
    private val extractMessageBodyWithoutQuote = ExtractMessageBodyWithoutQuote()
    private val conversationMessageMapper = ConversationDetailMessageUiModelMapper(
        avatarUiModelMapper = DetailAvatarUiModelMapper(getInitial),
        expirationTimeMapper = ExpirationTimeMapper(getCurrentEpochTimeDuration),
        colorMapper = colorMapper,
        formatShortTime = formatShortTime,
        messageLocationUiModelMapper = messageLocationUiModelMapper,
        resolveParticipantName = resolveParticipantName,
        messageDetailHeaderUiModelMapper = MessageDetailHeaderUiModelMapper(
            colorMapper = colorMapper,
            context = mockk(),
            detailAvatarUiModelMapper = DetailAvatarUiModelMapper(getInitial),
            formatExtendedTime = formatExtendedTime,
            formatShortTime = formatShortTime,
            messageLocationUiModelMapper = messageLocationUiModelMapper,
            participantUiModelMapper = ParticipantUiModelMapper(resolveParticipantName),
            resolveParticipantName = resolveParticipantName
        ),
        messageBannersUiModelMapper = MessageBannersUiModelMapper(context),
        messageBodyUiModelMapper = MessageBodyUiModelMapper(
            attachmentUiModelMapper = attachmentUiModelMapper,
            doesMessageBodyHaveEmbeddedImages = doesMessageBodyHaveEmbeddedImages,
            doesMessageBodyHaveRemoteContent = doesMessageBodyHaveRemoteContent,
            injectCssIntoDecryptedMessageBody = injectCssIntoDecryptedMessageBody,
            sanitizeHtmlOfDecryptedMessageBody = sanitizeHtmlOfDecryptedMessageBody,
            shouldShowEmbeddedImages = shouldShowEmbeddedImages,
            shouldShowRemoteContent = shouldShowRemoteContent,
            extractMessageBodyWithoutQuote = extractMessageBodyWithoutQuote
        ),
        participantUiModelMapper = ParticipantUiModelMapper(resolveParticipantName),
        messageIdUiModelMapper = messageIdUiModelMapper
    )

    private val conversationMetadataMapper = ConversationDetailMetadataUiModelMapper()
    // endregion

    private val reducer = ConversationDetailReducer(
        bottomBarReducer = BottomBarReducer(),
        metadataReducer = ConversationDetailMetadataReducer(),
        messagesReducer = ConversationDetailMessagesReducer(),
        bottomSheetReducer = BottomSheetReducer(
            moveToBottomSheetReducer = MoveToBottomSheetReducer(),
            labelAsBottomSheetReducer = LabelAsBottomSheetReducer(),
            mailboxMoreActionsBottomSheetReducer = MailboxMoreActionsBottomSheetReducer(),
            detailMoreActionsBottomSheetReducer = DetailMoreActionsBottomSheetReducer(
                DetailMoreActionsBottomSheetUiMapper()
            )
        ),
        deleteDialogReducer = ConversationDeleteDialogReducer(),
        reportPhishingDialogReducer = ConversationReportPhishingDialogReducer()
    )

    private val inMemoryConversationStateRepository = FakeInMemoryConversationStateRepository()
    private val setMessageViewState = SetMessageViewState(inMemoryConversationStateRepository)
    private val observeConversationViewState = ObserveConversationViewState(inMemoryConversationStateRepository)
    private val networkManager = mockk<NetworkManager>()
    private var testDispatcher: TestDispatcher? = null

    @BeforeTest
    fun setUp() {
        testDispatcher = StandardTestDispatcher().apply {
            Dispatchers.setMain(this)
        }
        mockkStatic(Formatter::formatShortFileSize)
        every { Formatter.formatShortFileSize(any(), any()) } returns "0"

        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns mockk()
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
        unmockkStatic(Formatter::formatShortFileSize)
        unmockkStatic(Uri::class)
    }

    @Test
    fun `Should emit updated requestLinkConfirmation flag`() = runTest {
        // given
        val privacySettings = PrivacySettings(
            autoShowRemoteContent = false,
            autoShowEmbeddedImages = false,
            preventTakingScreenshots = false,
            requestLinkConfirmation = true,
            allowBackgroundSync = false
        )
        coEvery { observePrivacySettings(userId) } returns flowOf(privacySettings.right())

        // When
        buildConversationDetailViewModel().state.test {
            skipItems(3)

            // then
            val convDetailsState = awaitItem()
            assertIs<ConversationDetailState>(convDetailsState)
            assertTrue { convDetailsState.requestLinkConfirmation }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Should disable requestLinkConfirmation flag when user checks do not ask again`() = runTest {
        // given
        val initialPrivacySettings = PrivacySettings(
            autoShowRemoteContent = false,
            autoShowEmbeddedImages = false,
            preventTakingScreenshots = false,
            requestLinkConfirmation = true,
            allowBackgroundSync = false
        )
        coEvery { observePrivacySettings(userId) } returns flowOf(initialPrivacySettings.right())
        coEvery { updateLinkConfirmationSetting(any()) } returns Unit.right()

        // When
        val viewModel = buildConversationDetailViewModel()
        viewModel.state.test {
            skipItems(3)

            // then
            val convDetailsState = awaitItem()
            assertIs<ConversationDetailState>(convDetailsState)
            assertTrue { convDetailsState.requestLinkConfirmation }

            // when
            viewModel.submit(ConversationDetailViewAction.DoNotAskLinkConfirmationAgain)

            // then
            awaitItem()
            coVerify { updateLinkConfirmationSetting(false) }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Should expand the message in the conversation after initial scroll and handle attachments`() = runTest {
        // given
        val messages = nonEmptyListOf(
            ConversationDetailMessageUiModelSample.invoiceExpandedWithAttachments(3)
        )
        val messageId = MessageId(messages.first().messageId.id)

        coEvery { getDecryptedMessageBody.invoke(userId, any()) } returns DecryptedMessageBody(
            messageId = messageId,
            value = EmailBodyTestSamples.BodyWithoutQuotes,
            mimeType = MimeType.Html,
            attachments = listOf(
                MessageAttachmentSample.document,
                MessageAttachmentSample.documentWithReallyLongFileName,
                MessageAttachmentSample.invoice,
                MessageAttachmentSample.image
            ),
            userAddress = UserAddressSample.PrimaryAddress
        ).right()
        coEvery { observeAttachmentStatus.invoke(userId, messageId, any()) } returns flowOf()

        val viewModel = buildConversationDetailViewModel()
        viewModel.state.test {
            // The initial states
            skipItems(3)

            // when
            val newState = awaitItem().messagesState as ConversationDetailsMessagesState.Data

            // then
            val collapsedMessage = newState.messages.first { it.messageId == messages.first().messageId }
            assertIs<Collapsed>(collapsedMessage)

            // When
            // Initial scroll completed and UI  notifies view model to expand message
            viewModel.submit(ExpandMessage(messageIdUiModelMapper.toUiModel(messageId)))

            // then
            val newExpandedState = awaitItem().messagesState as ConversationDetailsMessagesState.Data
            val expandedMessage = newExpandedState.messages.first { it.messageId == messages.first().messageId }
            assertIs<Expanded>(expandedMessage)
            assertEquals(
                messages.first().messageBodyUiModel.attachments,
                expandedMessage.messageBodyUiModel.attachments
            )
            assertEquals(MessageBodyExpandCollapseMode.NotApplicable, expandedMessage.expandCollapseMode)
            coVerifyOrder {
                observeAttachmentStatus.invoke(userId, messageId, MessageAttachmentSample.document.attachmentId)
                observeAttachmentStatus.invoke(
                    userId,
                    messageId,
                    MessageAttachmentSample.documentWithReallyLongFileName.attachmentId
                )
                observeAttachmentStatus.invoke(userId, messageId, MessageAttachmentSample.invoice.attachmentId)
                observeAttachmentStatus.invoke(userId, messageId, MessageAttachmentSample.image.attachmentId)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Should show expand button with initially collapsed mode when message contains quote`() = runTest {
        // given
        val messages = nonEmptyListOf(
            ConversationDetailMessageUiModelSample.invoiceExpandedWithAttachments(3)
        )
        val messageId = MessageId(messages.first().messageId.id)
        coEvery { getDecryptedMessageBody.invoke(userId, any()) } returns DecryptedMessageBody(
            messageId = messageId,
            value = EmailBodyTestSamples.BodyWithProtonMailQuote,
            mimeType = MimeType.Html,
            attachments = listOf(
                MessageAttachmentSample.document,
                MessageAttachmentSample.documentWithReallyLongFileName,
                MessageAttachmentSample.invoice,
                MessageAttachmentSample.image
            ),
            userAddress = UserAddressSample.PrimaryAddress
        ).right()
        coEvery { observeAttachmentStatus.invoke(userId, messageId, any()) } returns flowOf()

        val viewModel = buildConversationDetailViewModel()
        viewModel.state.test {
            // The initial states
            skipItems(4)

            // When
            // Initial scroll completed and UI  notifies view model to expand message
            viewModel.submit(ExpandMessage(messageIdUiModelMapper.toUiModel(messageId)))

            // Then
            val newExpandedStateBodyCollapsed = awaitItem().messagesState as ConversationDetailsMessagesState.Data
            val expandedMessageBodyCollapsed = newExpandedStateBodyCollapsed.messages.first {
                it.messageId == messages.first().messageId
            }
            assertIs<Expanded>(expandedMessageBodyCollapsed)
            assertEquals(MessageBodyExpandCollapseMode.Collapsed, expandedMessageBodyCollapsed.expandCollapseMode)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Should expand message body content when user clicks on body expand button`() = runTest {
        // given
        val messages = nonEmptyListOf(
            ConversationDetailMessageUiModelSample.invoiceExpandedWithAttachments(3)
        )
        val messageId = MessageId(messages.first().messageId.id)
        coEvery { getDecryptedMessageBody.invoke(userId, any()) } returns DecryptedMessageBody(
            messageId = messageId,
            value = EmailBodyTestSamples.BodyWithProtonMailQuote,
            mimeType = MimeType.Html,
            attachments = listOf(
                MessageAttachmentSample.document,
                MessageAttachmentSample.documentWithReallyLongFileName,
                MessageAttachmentSample.invoice,
                MessageAttachmentSample.image
            ),
            userAddress = UserAddressSample.PrimaryAddress
        ).right()
        coEvery { observeAttachmentStatus.invoke(userId, messageId, any()) } returns flowOf()

        val viewModel = buildConversationDetailViewModel()
        viewModel.state.test {
            // The initial states
            skipItems(4)
            viewModel.submit(ExpandMessage(messageIdUiModelMapper.toUiModel(messageId)))
            val stateBodyCollapsed = awaitItem().messagesState as ConversationDetailsMessagesState.Data
            val msgBodyCollapsed = stateBodyCollapsed.messages.first {
                it.messageId == messages.first().messageId
            }

            // When
            viewModel.submit(
                ConversationDetailViewAction.ExpandOrCollapseMessageBody(msgBodyCollapsed.messageId)
            )

            // then
            val newState = awaitItem().messagesState as ConversationDetailsMessagesState.Data
            val msgBodyExpanded = newState.messages.first {
                it.messageId == messages.first().messageId
            }
            assertIs<Expanded>(msgBodyExpanded)
            assertEquals(MessageBodyExpandCollapseMode.Expanded, msgBodyExpanded.expandCollapseMode)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Should collapse message body content when user clicks on body collapse button`() = runTest {
        // given
        val messages = nonEmptyListOf(
            ConversationDetailMessageUiModelSample.invoiceExpandedWithAttachments(3)
        )
        val messageId = MessageId(messages.first().messageId.id)
        val messageIdUiModel = messageIdUiModelMapper.toUiModel(messageId)
        coEvery { getDecryptedMessageBody.invoke(userId, any()) } returns DecryptedMessageBody(
            messageId = messageId,
            value = EmailBodyTestSamples.BodyWithProtonMailQuote,
            mimeType = MimeType.Html,
            attachments = listOf(
                MessageAttachmentSample.document,
                MessageAttachmentSample.documentWithReallyLongFileName,
                MessageAttachmentSample.invoice,
                MessageAttachmentSample.image
            ),
            userAddress = UserAddressSample.PrimaryAddress
        ).right()
        coEvery { observeAttachmentStatus.invoke(userId, messageId, any()) } returns flowOf()

        val viewModel = buildConversationDetailViewModel()
        viewModel.state.test {
            // The initial states
            skipItems(4)
            viewModel.submit(ExpandMessage(messageIdUiModelMapper.toUiModel(messageId)))
            awaitItem()
            viewModel.submit(ConversationDetailViewAction.ExpandOrCollapseMessageBody(messageIdUiModel))
            awaitItem()

            // When
            viewModel.submit(
                ConversationDetailViewAction.ExpandOrCollapseMessageBody(messageIdUiModel)
            )

            // then
            val newState = awaitItem().messagesState as ConversationDetailsMessagesState.Data
            val msgBodyExpanded = newState.messages.first {
                it.messageId == messages.first().messageId
            }
            assertIs<Expanded>(msgBodyExpanded)
            assertEquals(MessageBodyExpandCollapseMode.Collapsed, msgBodyExpanded.expandCollapseMode)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should emit mapped messages as they're emitted`() = runTest {
        // Given
        val initialMessage = MessageWithLabelsSample.AugWeatherForecast
        val updatedMessage = initialMessage.copy(
            message = initialMessage.message.copy(unread = true),
            labels = listOf(LabelSample.Label2021)
        )
        val conversationWithLabelsFlow = MutableStateFlow(nonEmptyListOf(initialMessage).right())
        val observeConversationMessagesMock = mockk<ObserveConversationMessagesWithLabels> {
            every { this@mockk(userId, initialMessage.message.conversationId) } returns conversationWithLabelsFlow
        }

        fun assertCorrectMessagesEmitted(actual: ConversationDetailsMessagesState.Data, expected: MessageWithLabels) {
            assertEquals(1, actual.messages.size)
            with(actual.messages.first() as Collapsed) {
                assertEquals(expected.message.messageId.id, messageId.id)
                assertEquals(expected.message.unread, isUnread)
                assertEquals(expected.labels.size, labels.size)
            }
        }

        // When
        buildConversationDetailViewModel(
            observeConversationMessages = observeConversationMessagesMock,
            observeFolderColor = observeFolderColorSettings
        ).state.test {
            // The initial states
            skipItems(3)

            // Then
            // The expanded message
            val actualFirstMessagesState = awaitItem().messagesState as ConversationDetailsMessagesState.Data
            assertEquals(1, actualFirstMessagesState.messages.size)
            assertCorrectMessagesEmitted(actualFirstMessagesState, expected = initialMessage)

            // When
            // Emit updated message
            conversationWithLabelsFlow.emit(nonEmptyListOf(updatedMessage).right())

            // Then
            val actualUpdatedMessagesState = awaitItem().messagesState as ConversationDetailsMessagesState.Data
            val actualLabels = (
                actualUpdatedMessagesState.messages.first()
                    as Collapsed
                ).labels
            assertCorrectMessagesEmitted(actualUpdatedMessagesState, expected = updatedMessage)
            assertEquals(LabelSample.Label2021.name, actualLabels.first().name)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should emit mapped header as folder color is emitted`() = runTest {
        // Given
        val message = MessageWithLabelsSample.AugWeatherForecastWithFolder

        val conversationWithLabelsFlow = MutableStateFlow(nonEmptyListOf(message).right())
        val observeConversationMessagesMock = mockk<ObserveConversationMessagesWithLabels> {
            every { this@mockk(userId, message.message.conversationId) } returns conversationWithLabelsFlow
        }
        val folderColorSettingsFlow = MutableStateFlow(defaultFolderColorSettings)
        val observeFolderColorSettingsMock = mockk<ObserveFolderColorSettings> {
            every { this@mockk(userId) } returns folderColorSettingsFlow
        }

        fun assertCorrectFolderColor(actual: ConversationDetailsMessagesState.Data, expected: FolderColorSettings) {
            assertEquals(1, actual.messages.size)
            with(actual.messages.first() as Collapsed) {
                when {
                    expected.useFolderColor -> assertNotNull(this.locationIcon.color)
                    else -> assertNull(this.locationIcon.color)
                }
            }
        }

        // When
        buildConversationDetailViewModel(
            observeConversationMessages = observeConversationMessagesMock,
            observeFolderColor = observeFolderColorSettingsMock
        ).state.test {
            // The initial states
            skipItems(3)

            // Then
            // The initial expanded message
            val actualFirstMessagesState = awaitItem().messagesState as ConversationDetailsMessagesState.Data
            assertCorrectFolderColor(actualFirstMessagesState, expected = defaultFolderColorSettings)

            // When
            // Emit updated message
            val updatedFolderColorSettings = FolderColorSettings(useFolderColor = false)
            folderColorSettingsFlow.emit(updatedFolderColorSettings)

            // Then
            val actualUpdatedMessagesState = awaitItem().messagesState as ConversationDetailsMessagesState.Data
            assertCorrectFolderColor(actualUpdatedMessagesState, expected = updatedFolderColorSettings)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should emit first non draft message as Collapsed`() = runTest {
        // given
        val expectedCollapsed = MessageWithLabelsSample.AugWeatherForecast
        val messages = nonEmptyListOf(
            expectedCollapsed,
            MessageWithLabelsSample.InvoiceWithLabel,
            MessageWithLabelsSample.EmptyDraft
        )
        coEvery { observeConversationMessagesWithLabels(userId, any()) } returns flowOf(messages.right())

        // When
        buildConversationDetailViewModel().state.test {
            skipItems(3)

            // then
            val collapsedState = awaitItem()
            val collapsedMessage = (collapsedState.messagesState as ConversationDetailsMessagesState.Data)
                .messages
                .first { it.messageId.id == expectedCollapsed.message.messageId.id }
            assertIs<Collapsed>(collapsedMessage)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should emit scroll to message id only on start but not when expanding a second message`() = runTest {
        // given
        val expectedScrolledTo = MessageWithLabelsSample.AugWeatherForecast
        val expectedExpandedNotScrolled = MessageWithLabelsSample.InvoiceWithLabel
        val messages = nonEmptyListOf(
            expectedScrolledTo,
            expectedExpandedNotScrolled,
            MessageWithLabelsSample.EmptyDraft
        )
        coEvery { observeConversationMessagesWithLabels(userId, any()) } returns flowOf(messages.right())

        // When
        val viewModel = buildConversationDetailViewModel()
        viewModel.state.test {
            skipItems(3)
            // then
            var conversationState: ConversationDetailState = awaitItem()
            val collapsedMessage = (conversationState.messagesState as ConversationDetailsMessagesState.Data)
                .messages
                .first { it.messageId.id == expectedScrolledTo.message.messageId.id }
            assertIs<Collapsed>(collapsedMessage)
            assertTrue { conversationState.scrollToMessage?.id == expectedScrolledTo.message.messageId.id }

            // when
            // scroll request completed, clear scroll msg id in state
            viewModel.submit(ConversationDetailViewAction.ScrollRequestCompleted)

            // then
            conversationState = awaitItem()
            assertTrue { conversationState.scrollToMessage == null }

            // when
            viewModel.submit(
                ExpandMessage(
                    messageIdUiModelMapper.toUiModel(
                        MessageWithLabelsSample.InvoiceWithLabel.message.messageId
                    )
                )
            )

            // then
            conversationState = awaitItem()
            val expandMessage = (conversationState.messagesState as ConversationDetailsMessagesState.Data)
                .messages
                .first { it.messageId.id == expectedExpandedNotScrolled.message.messageId.id }
            assertIs<Expanded>(expandMessage)
            // If there is only one expanded message that item  is scrolled to
            // (according to View Model requestScrollToMessageId)
            assertTrue { conversationState.scrollToMessage == expandMessage.messageId }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should emit expanding and expanded states when expanding a message`() = runTest {
        // given
        val defaultExpanded = MessageWithLabelsSample.AugWeatherForecast
        val expectedExpanded = MessageWithLabelsSample.InvoiceWithLabel
        val messages = nonEmptyListOf(
            defaultExpanded,
            expectedExpanded
        )
        coEvery { observeConversationMessagesWithLabels(userId, any()) } returns flowOf(messages.right())
        coEvery { getDecryptedMessageBody.invoke(any(), any()) } coAnswers {
            // Add a delay, so we're able to receive the `Expanding` state.
            // Without it, we'd only get the final `Expanded` state.
            delay(1)
            DecryptedMessageBody(
                messageId = defaultExpanded.message.messageId,
                value = "",
                mimeType = MimeType.Html,
                userAddress = UserAddressSample.PrimaryAddress
            ).right()
        }

        val viewModel = buildConversationDetailViewModel()

        // when
        viewModel.submit(ExpandMessage(messageIdUiModelMapper.toUiModel(expectedExpanded.message.messageId)))

        viewModel.state.test {
            skipItems(3)

            // then
            val expandingState = awaitItem()
            val expandingMessage = (expandingState.messagesState as ConversationDetailsMessagesState.Data)
                .messages
                .first { it.messageId.id == expectedExpanded.message.messageId.id }
            assertIs<Expanding>(expandingMessage)

            val expandedState = awaitItem()
            println(expandedState)
            val expandedMessage = (expandedState.messagesState as ConversationDetailsMessagesState.Data)
                .messages
                .first { it.messageId.id == expectedExpanded.message.messageId.id }
            assertIs<Expanded>(expandedMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should emit collapsed message when collapsing it`() = runTest {
        // given
        val defaultExpanded = MessageWithLabelsSample.AugWeatherForecast
        val messages = nonEmptyListOf(
            defaultExpanded,
            MessageWithLabelsSample.InvoiceWithLabel
        )
        coEvery { observeConversationMessagesWithLabels(userId, any()) } returns flowOf(messages.right())

        val viewModel = buildConversationDetailViewModel()

        viewModel.state.test {
            skipItems(3)

            // when
            viewModel.submit(
                ConversationDetailViewAction.CollapseMessage(
                    messageIdUiModelMapper.toUiModel(defaultExpanded.message.messageId)
                )
            )

            // then
            val collapsedState = awaitItem()
            val collapsedMessage = (collapsedState.messagesState as ConversationDetailsMessagesState.Data)
                .messages
                .first { it.messageId.id == defaultExpanded.message.messageId.id }
            assertIs<Collapsed>(collapsedMessage)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should emit scroll to message id when requested`() = runTest {
        // given
        val defaultExpanded = MessageWithLabelsSample.AugWeatherForecast
        val messages = nonEmptyListOf(
            defaultExpanded,
            MessageWithLabelsSample.InvoiceWithLabel
        )
        coEvery { observeConversationMessagesWithLabels(userId, any()) } returns flowOf(messages.right())

        val viewModel = buildConversationDetailViewModel()

        viewModel.state.test {
            skipItems(3)
            // when
            viewModel.submit(RequestScrollTo(messageIdUiModelMapper.toUiModel(defaultExpanded.message.messageId)))

            // then
            assertEquals(defaultExpanded.message.id, awaitItem().scrollToMessage?.id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should emit show all attachment when view action is triggered`() = runTest {
        // given
        val expectedAttachmentCount = 5
        val defaultExpanded = MessageWithLabelsSample.AugWeatherForecast
        val expectedExpanded = MessageWithLabelsSample.InvoiceWithLabel
        val messages = nonEmptyListOf(
            defaultExpanded,
            expectedExpanded
        )
        coEvery { observeConversationMessagesWithLabels(userId, any()) } returns flowOf(messages.right())
        coEvery { getDecryptedMessageBody.invoke(any(), expectedExpanded.message.messageId) } returns
            DecryptedMessageBody(
                messageId = expectedExpanded.message.messageId,
                value = "",
                mimeType = MimeType.Html,
                attachments = (0 until expectedAttachmentCount).map {
                    aMessageAttachment(id = it.toString())
                },
                userAddress = UserAddressSample.PrimaryAddress
            ).right()
        coEvery { observeAttachmentStatus.invoke(userId, any(), any()) } returns flowOf()

        val viewModel = buildConversationDetailViewModel()

        viewModel.state.test {
            skipItems(4)
            viewModel.submit(ExpandMessage(messageIdUiModelMapper.toUiModel(expectedExpanded.message.messageId)))
            advanceUntilIdle()

            // When
            viewModel.submit(
                ShowAllAttachmentsForMessage(
                    messageIdUiModelMapper.toUiModel(
                        expectedExpanded.message.messageId
                    )
                )
            )
            advanceUntilIdle()
            val newItem = awaitItem()

            // Then
            val messagesState = (newItem.messagesState as ConversationDetailsMessagesState.Data).messages
            val expandedInvoice =
                messagesState.first { it.messageId.id == expectedExpanded.message.messageId.id } as Expanded
            assertEquals(expectedAttachmentCount, expandedInvoice.messageBodyUiModel.attachments!!.attachments.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `verify get attachment is called when attachment is clicked`() = runTest {
        // given
        val expectedAttachmentCount = 5
        val defaultExpanded = MessageWithLabelsSample.AugWeatherForecast
        val expectedExpanded = MessageWithLabelsSample.InvoiceWithLabel
        val messages = nonEmptyListOf(
            defaultExpanded,
            expectedExpanded
        )
        val expandedMessageId = expectedExpanded.message.messageId
        coEvery { observeConversationMessagesWithLabels(userId, any()) } returns flowOf(messages.right())
        coEvery { getDecryptedMessageBody.invoke(any(), expandedMessageId) } returns
            DecryptedMessageBody(
                messageId = expandedMessageId,
                value = "",
                mimeType = MimeType.Html,
                attachments = (0 until expectedAttachmentCount).map {
                    aMessageAttachment(id = it.toString())
                },
                userAddress = UserAddressSample.PrimaryAddress
            ).right()
        coEvery { observeAttachmentStatus(userId, expandedMessageId, any()) } returns flowOf()
        coEvery {
            getDownloadingAttachmentsForMessages(
                userId,
                listOf(defaultExpanded.message.messageId, expandedMessageId)
            )
        } returns listOf()
        coEvery {
            getAttachmentIntentValues.invoke(any(), any(), any())
        } returns DataError.Local.NoDataCached.left()

        val viewModel = buildConversationDetailViewModel()

        viewModel.state.test {
            skipItems(4)
            viewModel.submit(ExpandMessage(messageIdUiModelMapper.toUiModel(expectedExpanded.message.messageId)))

            // When
            viewModel.submit(
                OnAttachmentClicked(
                    messageIdUiModelMapper.toUiModel(expectedExpanded.message.messageId),
                    AttachmentId(0.toString())
                )
            )
            awaitItem()

            // Then
            val expectedMessageId = expectedExpanded.message.messageId

            coVerify { getAttachmentIntentValues(userId, expectedMessageId, AttachmentId(0.toString())) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `verify get attachment is not called and error is shown when other attachment is currently downloaded`() =
        runTest {
            // given
            val expectedAttachmentCount = 5
            val defaultExpanded = MessageWithLabelsSample.AugWeatherForecast
            val expectedExpanded = MessageWithLabelsSample.InvoiceWithLabel
            val messages = nonEmptyListOf(
                defaultExpanded,
                expectedExpanded
            )
            val expandedMessageId = expectedExpanded.message.messageId
            coEvery { observeConversationMessagesWithLabels(userId, any()) } returns flowOf(messages.right())
            coEvery { getDecryptedMessageBody.invoke(any(), expandedMessageId) } returns
                DecryptedMessageBody(
                    messageId = expandedMessageId,
                    value = "",
                    mimeType = MimeType.Html,
                    attachments = (0 until expectedAttachmentCount).map {
                        aMessageAttachment(id = it.toString())
                    },
                    userAddress = UserAddressSample.PrimaryAddress
                ).right()
            coEvery { observeAttachmentStatus(userId, expandedMessageId, any()) } returns flowOf()
            coEvery {
                getDownloadingAttachmentsForMessages(
                    userId,
                    listOf(defaultExpanded.message.messageId, expandedMessageId)
                )
            } returns listOf(MessageAttachmentMetadataTestData.buildMessageAttachmentMetadata())

            val viewModel = buildConversationDetailViewModel()

            viewModel.state.test {
                skipItems(4)
                viewModel.submit(ExpandMessage(messageIdUiModelMapper.toUiModel(expectedExpanded.message.messageId)))
                skipItems(1)
                // When
                viewModel.submit(
                    OnAttachmentClicked(
                        messageIdUiModelMapper.toUiModel(expectedExpanded.message.messageId),
                        AttachmentId(0.toString())
                    )
                )
                val actualState = awaitItem()

                // Then
                assertEquals(Effect.of(TextUiModel(R.string.error_attachment_download_in_progress)), actualState.error)
                coVerify { getAttachmentIntentValues wasNot Called }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `verify error is shown when getting attachment failed`() = runTest {
        // given
        val expectedAttachmentCount = Random().nextInt(100)
        val defaultExpanded = MessageWithLabelsSample.AugWeatherForecast
        val expectedExpanded = MessageWithLabelsSample.InvoiceWithLabel
        val messages = nonEmptyListOf(
            defaultExpanded,
            expectedExpanded
        )
        val expandedMessageId = expectedExpanded.message.messageId
        mockAttachmentDownload(
            messages = messages,
            expandedMessageId = expandedMessageId,
            expectedAttachmentCount = expectedAttachmentCount,
            defaultExpanded = defaultExpanded,
            expectedError = DataError.Local.NoDataCached
        )

        val viewModel = buildConversationDetailViewModel()

        viewModel.state.test {
            skipItems(4)
            viewModel.submit(ExpandMessage(messageIdUiModelMapper.toUiModel(expectedExpanded.message.messageId)))
            skipItems(1)

            // When
            viewModel.submit(
                OnAttachmentClicked(
                    messageIdUiModelMapper.toUiModel(expectedExpanded.message.messageId),
                    AttachmentId(0.toString())
                )
            )
            val actualState = awaitItem()

            // Then
            val expectedMessageId = expectedExpanded.message.messageId

            coVerify { getAttachmentIntentValues(userId, expectedMessageId, AttachmentId(0.toString())) }
            assertEquals(Effect.of(TextUiModel(R.string.error_get_attachment_failed)), actualState.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `verify not enough space error is shown when getting attachment failed due to insufficient storage`() =
        runTest {
            // given
            val expectedAttachmentCount = Random().nextInt(100)
            val defaultExpanded = MessageWithLabelsSample.AugWeatherForecast
            val expectedExpanded = MessageWithLabelsSample.InvoiceWithLabel
            val messages = nonEmptyListOf(
                defaultExpanded,
                expectedExpanded
            )
            val expandedMessageId = expectedExpanded.message.messageId
            mockAttachmentDownload(
                messages = messages,
                expandedMessageId = expandedMessageId,
                expectedAttachmentCount = expectedAttachmentCount,
                defaultExpanded = defaultExpanded,
                expectedError = DataError.Local.OutOfMemory
            )

            val viewModel = buildConversationDetailViewModel()

            viewModel.state.test {
                skipItems(4)
                viewModel.submit(ExpandMessage(messageIdUiModelMapper.toUiModel(expectedExpanded.message.messageId)))
                skipItems(1)

                // When
                viewModel.submit(
                    OnAttachmentClicked(
                        messageIdUiModelMapper.toUiModel(expectedExpanded.message.messageId),
                        AttachmentId(0.toString())
                    )
                )
                val actualState = awaitItem()

                // Then
                val expectedMessageId = expectedExpanded.message.messageId

                coVerify { getAttachmentIntentValues(userId, expectedMessageId, AttachmentId(0.toString())) }
                assertEquals(Effect.of(TextUiModel(R.string.error_get_attachment_not_enough_memory)), actualState.error)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `verify delete dialog is shown when delete conversation is called`() = runTest {
        // Given
        val expectedTitle = TextUiModel(R.string.message_delete_dialog_title)
        val expectedMessage = TextUiModel(R.string.message_delete_dialog_message)
        val viewModel = buildConversationDetailViewModel()

        // When
        viewModel.submit(ConversationDetailViewAction.DeleteRequested)
        advanceUntilIdle()

        // Then
        assertEquals(
            expected = DeleteDialogState.Shown(expectedTitle, expectedMessage),
            actual = viewModel.state.value.deleteDialogState
        )
    }

    @Test
    fun `verify delete dialog is hidden when dismissed is called`() = runTest {
        // Given
        val viewModel = buildConversationDetailViewModel()

        // When
        viewModel.submit(ConversationDetailViewAction.DeleteRequested)
        advanceUntilIdle()
        viewModel.submit(ConversationDetailViewAction.DeleteDialogDismissed)
        advanceUntilIdle()

        // Then
        assertEquals(
            expected = DeleteDialogState.Hidden,
            actual = viewModel.state.value.deleteDialogState
        )
    }

    @Test
    fun `verify delete is executed when delete confirmed is called`() = runTest {
        // Given
        val expectedMessage = TextUiModel(R.string.conversation_deleted)
        coEvery {
            observeConversationUseCase(userId, conversationId, false)
        } returns flowOf(
            ConversationSample.WeatherForecast.copy(
                labels = listOf(ConversationLabelSample.build(labelId = LabelIdSample.Trash))
            ).right()
        )
        coJustRun { deleteConversations(userId, listOf(conversationId), LabelIdSample.Trash) }

        val viewModel = buildConversationDetailViewModel()

        // When
        viewModel.submit(ConversationDetailViewAction.DeleteConfirmed)
        advanceUntilIdle()

        // Then
        coVerify { deleteConversations(userId, listOf(conversationId), LabelIdSample.Trash) }
        assertEquals(expectedMessage, viewModel.state.value.exitScreenWithMessageEffect.consume())
    }

    @Test
    fun `verify error is shown when getting conversation fails`() = runTest {
        // Given
        val expectedMessage = TextUiModel(R.string.error_delete_conversation_failed)
        coEvery {
            observeConversationUseCase(userId, conversationId, false)
        } returns flowOf(DataError.Local.NoDataCached.left())

        val viewModel = buildConversationDetailViewModel()

        // When
        viewModel.submit(ConversationDetailViewAction.DeleteConfirmed)
        advanceUntilIdle()

        // Then
        coVerify { deleteConversations wasNot Called }
        assertEquals(expectedMessage, viewModel.state.value.error.consume())
    }

    @Test
    fun `verify error is shown when conversation is in wrong location `() = runTest {
        // Given
        val expectedMessage = TextUiModel(R.string.error_delete_conversation_failed_wrong_folder)

        val viewModel = buildConversationDetailViewModel()

        // When
        viewModel.submit(ConversationDetailViewAction.DeleteConfirmed)
        advanceUntilIdle()

        // Then
        coVerify { deleteConversations wasNot Called }
        assertEquals(expectedMessage, viewModel.state.value.error.consume())
    }

    @Test
    fun `should initially scroll to the message id assigned by the navigator in search mode`() = runTest {
        // given
        val searchedItem = MessageWithLabelsSample.AugWeatherForecast
        val messages = nonEmptyListOf(
            MessageWithLabelsSample.SepWeatherForecast,
            searchedItem,
            MessageWithLabelsSample.InvoiceWithLabel
        )
        coEvery { observeConversationMessagesWithLabels(userId, any()) } returns flowOf(messages.right())
        coEvery { savedStateHandle.get<String>(ConversationDetailScreen.ScrollToMessageIdKey) } returns
            searchedItem.message.messageId.id

        // When
        val viewModel = buildConversationDetailViewModel()
        viewModel.state.test {
            skipItems(3)

            // then
            val conversationState: ConversationDetailState = awaitItem()
            assertEquals(searchedItem.message.messageId.id, conversationState.scrollToMessage?.id)

            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun mockAttachmentDownload(
        messages: NonEmptyList<MessageWithLabels>,
        expandedMessageId: MessageId,
        expectedAttachmentCount: Int,
        defaultExpanded: MessageWithLabels,
        expectedError: DataError.Local
    ) {
        coEvery { observeConversationMessagesWithLabels(userId, any()) } returns flowOf(messages.right())
        coEvery { getDecryptedMessageBody.invoke(any(), expandedMessageId) } returns
            DecryptedMessageBody(
                messageId = expandedMessageId,
                value = "",
                MimeType.Html,
                attachments = (0 until expectedAttachmentCount).map {
                    aMessageAttachment(id = it.toString())
                },
                userAddress = UserAddressSample.PrimaryAddress
            ).right()
        coEvery { observeAttachmentStatus(userId, expandedMessageId, any()) } returns flowOf()
        coEvery {
            getDownloadingAttachmentsForMessages(
                userId,
                listOf(defaultExpanded.message.messageId, expandedMessageId)
            )
        } returns listOf()
        coEvery {
            getAttachmentIntentValues(userId, expandedMessageId, AttachmentId(0.toString()))
        } returns expectedError.left()
    }

    @Test
    fun `Should emit expanding and then collapse state if the message is not decrypted`() = runTest {
        // Given
        val defaultExpanded = MessageWithLabelsSample.AugWeatherForecast
        val messages = nonEmptyListOf(
            defaultExpanded,
            MessageWithLabelsSample.EmptyDraft
        )
        coEvery { observeConversationMessagesWithLabels(userId, any()) } returns flowOf(messages.right())
        coEvery { getDecryptedMessageBody.invoke(any(), any()) } coAnswers {
            // Add a delay, so we're able to receive the `Expanding` state.
            // Without it, we'd only get the final `Expanded` state.
            delay(1)
            GetDecryptedMessageBodyError.Decryption(defaultExpanded.message.messageId, "").left()
        }

        val viewModel = buildConversationDetailViewModel()
        viewModel.state.test {
            skipItems(3)

            // When
            val initialCollapsedState = awaitItem()

            // Then
            var message = (initialCollapsedState.messagesState as ConversationDetailsMessagesState.Data)
                .messages
                .first { it.messageId.id == defaultExpanded.message.messageId.id }
            assertIs<Collapsed>(message)

            // When
            viewModel.submit(ExpandMessage(messageIdUiModelMapper.toUiModel(defaultExpanded.message.messageId)))
            var expandingState = awaitItem()

            // Then
            message = (expandingState.messagesState as ConversationDetailsMessagesState.Data)
                .messages
                .first { it.messageId.id == defaultExpanded.message.messageId.id }
            assertIs<Expanding>(message)

            val collapsedState = awaitItem()
            message = (collapsedState.messagesState as ConversationDetailsMessagesState.Data)
                .messages
                .first { it.messageId.id == defaultExpanded.message.messageId.id }
            assertIs<Collapsed>(message)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `returns get embedded image result when getting was successful`() = runTest {
        // Given
        val messageId = MessageId("rawMessageId")
        val contentId = "contentId"
        val byteArray = "I'm a byte array".toByteArray()
        val expectedResult = GetEmbeddedImageResult(byteArray, "image/png")
        coEvery { getEmbeddedImageAvoidDuplicatedExecution(userId, messageId, contentId, any()) } returns expectedResult
        val viewModel = buildConversationDetailViewModel()

        // When
        val actual = viewModel.loadEmbeddedImage(messageId, contentId)

        // Then
        assertEquals(expectedResult, actual)
    }

    @Test
    fun `returns null when get embedded image returned an error`() = runTest {
        // Given
        val messageId = MessageId("rawMessageId")
        val contentId = "contentId"
        coEvery { getEmbeddedImageAvoidDuplicatedExecution(userId, messageId, contentId, any()) } returns null
        val viewModel = buildConversationDetailViewModel()

        // When
        val actual = viewModel.loadEmbeddedImage(messageId, contentId)

        // Then
        assertNull(actual)
    }

    @Test
    fun `verify bottom sheet with data is emitted when more actions bottom sheet is requested and loading succeeds`() =
        runTest {
            // Given
            val messageId = MessageId("messageId")
            coEvery {
                observeMessage(userId = userId, messageId = messageId)
            } returns flowOf(MessageSample.AugWeatherForecast.right())

            // When
            val viewModel = buildConversationDetailViewModel()
            viewModel.state.test {
                viewModel.submit(ConversationDetailViewAction.RequestMoreActionsBottomSheet(messageId))
                advanceUntilIdle()

                // Then
                assertIs<DetailMoreActionsBottomSheetState.Data>(lastEmittedItem().bottomSheetState?.contentState)
            }
        }

    @Test
    fun `verify no bottom sheet data is emitted when more actions bottom sheet is requested and loading fails`() =
        runTest {
            // Given
            val messageId = MessageId("messageId")
            coEvery {
                observeMessage(
                    userId = userId,
                    messageId = messageId
                )
            } returns flowOf(DataError.Local.NoDataCached.left())

            // When
            val viewModel = buildConversationDetailViewModel()
            viewModel.state.test {
                viewModel.submit(ConversationDetailViewAction.RequestMoreActionsBottomSheet(messageId))
                advanceUntilIdle()

                // Then
                assertNull(lastEmittedItem().bottomSheetState?.contentState)
            }
        }

    @Test
    fun `when user clicks report phishing and network state is connected then confirm dialog is shown`() = runTest {
        // Given
        val expected = ReportPhishingDialogState.Shown.ShowConfirmation(MessageIdSample.Invoice)
        coEvery { networkManager.networkStatus } returns NetworkStatus.Metered

        val viewModel = buildConversationDetailViewModel()
        viewModel.state.test {
            // When
            viewModel.submit(ConversationDetailViewAction.ReportPhishing(MessageIdSample.Invoice))
            advanceUntilIdle()

            // Then
            assertEquals(expected, lastEmittedItem().reportPhishingDialogState)
        }
    }

    @Test
    fun `when user clicks report phishing and network state is disconnected then offline hint is shown`() = runTest {
        // Given
        val expected = ReportPhishingDialogState.Shown.ShowOfflineHint
        coEvery { networkManager.networkStatus } returns NetworkStatus.Disconnected

        val viewModel = buildConversationDetailViewModel()
        viewModel.state.test {
            // When
            viewModel.submit(ConversationDetailViewAction.ReportPhishing(MessageIdSample.Invoice))
            advanceUntilIdle()

            // Then
            assertEquals(expected, lastEmittedItem().reportPhishingDialogState)
        }
    }

    @Test
    fun `when user confirms report phishing then report use case is called`() = runTest {
        // Given
        val expectedMessageId = MessageIdSample.HtmlInvoice
        coEvery { networkManager.networkStatus } returns NetworkStatus.Metered
        coEvery { reportPhishingMessage(userId, expectedMessageId) } returns Unit.right()

        val viewModel = buildConversationDetailViewModel()
        viewModel.state.test {
            // When
            viewModel.submit(ConversationDetailViewAction.ReportPhishingConfirmed(expectedMessageId))
            advanceUntilIdle()

            // Then
            coVerify { reportPhishingMessage(userId, expectedMessageId) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when a user click open proton calendar and calendar is installed, then open in proton calendar is called`() =
        runTest {
            // Given
            val expectedUri = mockk<Uri>()
            val message = MessageWithLabelsSample.CalendarWithoutLabels
            val messages = nonEmptyListOf(message)

            val messageId = message.message.messageId
            coEvery { observeConversationMessagesWithLabels(userId, any()) } returns flowOf(messages.right())
            coEvery { getDecryptedMessageBody.invoke(userId, messageId) } returns DecryptedMessageBody(
                messageId = messageId,
                value = EmailBodyTestSamples.BodyWithoutQuotes,
                mimeType = MimeType.Html,
                attachments = listOf(MessageAttachmentSample.calendar),
                userAddress = UserAddressSample.PrimaryAddress
            ).right()
            coEvery { observeAttachmentStatus.invoke(userId, messageId, any()) } returns flowOf()
            coEvery { isProtonCalendarInstalled() } returns true
            coEvery {
                getAttachmentIntentValues(userId, messageId, AttachmentId("calendar"))
            } returns OpenAttachmentIntentValues(
                mimeType = " text/calendar",
                uri = expectedUri
            ).right()

            val viewModel = buildConversationDetailViewModel()
            viewModel.state.test {
                // The initial states
                skipItems(4)
                viewModel.submit(ExpandMessage(messageIdUiModelMapper.toUiModel(messageId)))
                awaitItem() // Expanded message

                // When
                viewModel.submit(ConversationDetailViewAction.OpenInProtonCalendar(messageId))
                val actual = awaitItem()
                val calendarIntentValues = actual.openProtonCalendarIntent.consume() as? OpenIcsInProtonCalendar
                assertNotNull(calendarIntentValues)
                assertEquals(expectedUri, calendarIntentValues.uriToIcsAttachment)
                assertEquals(message.message.sender.address, calendarIntentValues.sender)
                assertEquals(UserAddressSample.PrimaryAddress.email, calendarIntentValues.recipient)
            }
        }

    @Test
    fun `when a user click open proton calendar and calendar is not installed, then open play store is called`() =
        runTest {
            // Given
            val message = MessageWithLabelsSample.CalendarWithoutLabels
            val messages = nonEmptyListOf(message)

            val messageId = message.message.messageId
            coEvery { observeConversationMessagesWithLabels(userId, any()) } returns flowOf(messages.right())
            coEvery { getDecryptedMessageBody.invoke(userId, messageId) } returns DecryptedMessageBody(
                messageId = messageId,
                value = EmailBodyTestSamples.BodyWithoutQuotes,
                mimeType = MimeType.Html,
                attachments = listOf(MessageAttachmentSample.calendar),
                userAddress = UserAddressSample.PrimaryAddress
            ).right()
            coEvery { observeAttachmentStatus.invoke(userId, messageId, any()) } returns flowOf()
            coEvery { isProtonCalendarInstalled() } returns false

            val viewModel = buildConversationDetailViewModel()
            viewModel.state.test {
                // The initial states
                skipItems(4)
                viewModel.submit(ExpandMessage(messageIdUiModelMapper.toUiModel(messageId)))
                awaitItem() // Expanded message

                // When
                viewModel.submit(ConversationDetailViewAction.OpenInProtonCalendar(messageId))
                val actual = awaitItem()
                val calendarIntentValues = actual.openProtonCalendarIntent.consume() as? OpenProtonCalendarOnPlayStore
                assertNotNull(calendarIntentValues)
            }
        }

    @Suppress("LongParameterList")
    private fun buildConversationDetailViewModel(
        observePrimaryUser: ObservePrimaryUserId = observePrimaryUserId,
        actionMapper: ActionUiModelMapper = actionUiModelMapper,
        messageMapper: ConversationDetailMessageUiModelMapper = conversationMessageMapper,
        metadataMapper: ConversationDetailMetadataUiModelMapper = conversationMetadataMapper,
        unread: MarkConversationAsUnread = markConversationAsUnread,
        moveConversation: MoveConversation = move,
        relabel: RelabelConversation = relabelConversation,
        delete: DeleteConversations = deleteConversations,
        report: ReportPhishingMessage = reportPhishingMessage,
        contacts: ObserveContacts = observeContacts,
        observeConversation: ObserveConversation = observeConversationUseCase,
        observeConversationMessages: ObserveConversationMessagesWithLabels = observeConversationMessagesWithLabels,
        observeDetailActions: ObserveConversationDetailActions = observeConversationDetailActions,
        observeDestinationMailLabels: ObserveExclusiveDestinationMailLabels = observeMailLabels,
        observeFolderColor: ObserveFolderColorSettings = observeFolderColorSettings,
        observeCustomMailLabels: ObserveCustomMailLabels = observeCustomMailLabelsUseCase,
        observeMessageAttachmentStatus: ObserveMessageAttachmentStatus = observeAttachmentStatus,
        getAttachmentStatus: GetDownloadingAttachmentsForMessages = getDownloadingAttachmentsForMessages,
        detailReducer: ConversationDetailReducer = reducer,
        savedState: SavedStateHandle = savedStateHandle,
        star: StarConversations = starConversations,
        unStar: UnStarConversations = unStarConversations,
        decryptedMessageBody: GetDecryptedMessageBody = getDecryptedMessageBody,
        markMessageAndConversationReadIfAllMessagesRead: MarkMessageAndConversationReadIfAllMessagesRead =
            markMessageAndConversationReadIfAllRead,
        getIntentValues: GetAttachmentIntentValues = getAttachmentIntentValues,
        ioDispatcher: CoroutineDispatcher = testDispatcher!!,
        networkMgmt: NetworkManager = networkManager,
        protonCalendarInstalled: IsProtonCalendarInstalled = isProtonCalendarInstalled
    ) = ConversationDetailViewModel(
        observePrimaryUserId = observePrimaryUser,
        messageIdUiModelMapper = messageIdUiModelMapper,
        actionUiModelMapper = actionMapper,
        conversationMessageMapper = messageMapper,
        conversationMetadataMapper = metadataMapper,
        markConversationAsUnread = unread,
        moveConversation = moveConversation,
        deleteConversations = delete,
        relabelConversation = relabel,
        reportPhishingMessage = report,
        observeContacts = contacts,
        observeConversation = observeConversation,
        observeConversationMessages = observeConversationMessages,
        observeDetailActions = observeDetailActions,
        observeDestinationMailLabels = observeDestinationMailLabels,
        observeFolderColor = observeFolderColor,
        observeCustomMailLabels = observeCustomMailLabels,
        observeMessage = observeMessage,
        observeMessageAttachmentStatus = observeMessageAttachmentStatus,
        getDownloadingAttachmentsForMessages = getAttachmentStatus,
        reducer = detailReducer,
        starConversations = star,
        unStarConversations = unStar,
        savedStateHandle = savedState,
        getDecryptedMessageBody = decryptedMessageBody,
        markMessageAndConversationReadIfAllMessagesRead = markMessageAndConversationReadIfAllMessagesRead,
        setMessageViewState = setMessageViewState,
        observeConversationViewState = observeConversationViewState,
        getAttachmentIntentValues = getIntentValues,
        getEmbeddedImageAvoidDuplicatedExecution = getEmbeddedImageAvoidDuplicatedExecution,
        ioDispatcher = ioDispatcher,
        observePrivacySettings = observePrivacySettings,
        updateLinkConfirmationSetting = updateLinkConfirmationSetting,
        resolveParticipantName = resolveParticipantName,
        isProtonCalendarInstalled = protonCalendarInstalled,
        networkManager = networkMgmt
    )

    private fun aMessageAttachment(id: String): MessageAttachment = MessageAttachment(
        attachmentId = AttachmentId(id),
        name = "name",
        size = 0,
        mimeType = MimeType.MultipartMixed.value,
        disposition = null,
        keyPackets = null,
        signature = null,
        encSignature = null,
        headers = emptyMap()
    )

    private suspend fun ReceiveTurbine<ConversationDetailState>.lastEmittedItem(): ConversationDetailState {
        val events = cancelAndConsumeRemainingEvents()
        return (events.last() as Event.Item).value
    }
}
