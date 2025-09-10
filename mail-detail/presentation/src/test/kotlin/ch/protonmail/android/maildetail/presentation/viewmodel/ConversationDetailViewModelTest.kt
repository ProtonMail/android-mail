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

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.Event
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import ch.protonmail.android.mailattachments.domain.usecase.GetAttachmentIntentValues
import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.NetworkError
import ch.protonmail.android.mailcommon.domain.sample.ConversationIdSample
import ch.protonmail.android.mailcommon.domain.sample.DataErrorSample
import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.R
import ch.protonmail.android.mailcommon.presentation.mapper.ActionUiModelMapper
import ch.protonmail.android.mailcommon.presentation.model.ActionResult
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.model.BottomSheetState
import ch.protonmail.android.mailcommon.presentation.model.BottomSheetVisibilityEffect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.sample.ParticipantAvatarSample
import ch.protonmail.android.mailcontact.domain.model.ContactMetadata
import ch.protonmail.android.mailcontact.domain.model.GetContactError
import ch.protonmail.android.mailcontact.domain.usecase.FindContactByEmail
import ch.protonmail.android.mailcontact.domain.usecase.ObserveContacts
import ch.protonmail.android.mailconversation.domain.entity.ConversationError
import ch.protonmail.android.mailconversation.domain.sample.ConversationSample
import ch.protonmail.android.mailconversation.domain.usecase.DeleteConversations
import ch.protonmail.android.mailconversation.domain.usecase.ObserveConversation
import ch.protonmail.android.mailconversation.domain.usecase.StarConversations
import ch.protonmail.android.mailconversation.domain.usecase.UnStarConversations
import ch.protonmail.android.maildetail.domain.usecase.AnswerRsvpEvent
import ch.protonmail.android.maildetail.domain.usecase.GetDownloadingAttachmentsForMessages
import ch.protonmail.android.maildetail.domain.usecase.GetRsvpEvent
import ch.protonmail.android.maildetail.domain.usecase.IsProtonCalendarInstalled
import ch.protonmail.android.maildetail.domain.usecase.MarkConversationAsRead
import ch.protonmail.android.maildetail.domain.usecase.MarkConversationAsUnread
import ch.protonmail.android.maildetail.domain.usecase.MarkMessageAsLegitimate
import ch.protonmail.android.maildetail.domain.usecase.MarkMessageAsRead
import ch.protonmail.android.maildetail.domain.usecase.MarkMessageAsUnread
import ch.protonmail.android.maildetail.domain.usecase.MessageViewStateCache
import ch.protonmail.android.maildetail.domain.usecase.MoveConversation
import ch.protonmail.android.maildetail.domain.usecase.MoveMessage
import ch.protonmail.android.maildetail.domain.usecase.ObserveConversationMessages
import ch.protonmail.android.maildetail.domain.usecase.ObserveConversationViewState
import ch.protonmail.android.maildetail.domain.usecase.ObserveDetailBottomBarActions
import ch.protonmail.android.maildetail.domain.usecase.ReportPhishingMessage
import ch.protonmail.android.maildetail.domain.usecase.UnblockSender
import ch.protonmail.android.maildetail.domain.usecase.UnsubscribeFromNewsletter
import ch.protonmail.android.maildetail.presentation.R.string
import ch.protonmail.android.maildetail.presentation.mapper.ConversationDetailMessageUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.ConversationDetailMetadataUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.MessageIdUiModelMapper
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMetadataState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.MoveToInbox
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailsMessagesState
import ch.protonmail.android.maildetail.presentation.model.MessageBodyLink
import ch.protonmail.android.maildetail.presentation.model.MessageIdUiModel
import ch.protonmail.android.maildetail.presentation.model.ParticipantUiModel
import ch.protonmail.android.maildetail.presentation.reducer.ConversationDetailReducer
import ch.protonmail.android.maildetail.presentation.sample.ConversationDetailMessageUiModelSample
import ch.protonmail.android.maildetail.presentation.sample.ConversationDetailMessageUiModelSample.InvoiceWithLabelExpanded
import ch.protonmail.android.maildetail.presentation.sample.ConversationDetailMessageUiModelSample.InvoiceWithLabelExpanding
import ch.protonmail.android.maildetail.presentation.sample.ConversationDetailMetadataUiModelSample
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailScreen
import ch.protonmail.android.maildetail.presentation.usecase.GetMessagesInSameExclusiveLocation
import ch.protonmail.android.maildetail.presentation.usecase.GetMoreActionsBottomSheetData
import ch.protonmail.android.maildetail.presentation.usecase.LoadImageAvoidDuplicatedExecution
import ch.protonmail.android.maildetail.presentation.usecase.ObservePrimaryUserAddress
import ch.protonmail.android.maildetail.presentation.usecase.print.PrintMessage
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.sample.LabelIdSample
import ch.protonmail.android.maillabel.presentation.bottomsheet.moveto.MoveToBottomSheetEntryPoint
import ch.protonmail.android.maillabel.presentation.model.MailLabelText
import ch.protonmail.android.mailmessage.domain.model.AttachmentListExpandCollapseMode
import ch.protonmail.android.mailmessage.domain.model.ConversationMessages
import ch.protonmail.android.mailmessage.domain.model.DecryptedMessageBody
import ch.protonmail.android.mailmessage.domain.model.GetMessageBodyError
import ch.protonmail.android.mailmessage.domain.model.MessageBodyTransformations
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageTheme
import ch.protonmail.android.mailmessage.domain.model.MessageThemeOptions
import ch.protonmail.android.mailmessage.domain.model.MimeType
import ch.protonmail.android.mailmessage.domain.model.Participant
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageSample
import ch.protonmail.android.mailmessage.domain.usecase.CancelScheduleSendMessage
import ch.protonmail.android.mailmessage.domain.usecase.DeleteMessages
import ch.protonmail.android.mailmessage.domain.usecase.GetMessageBodyWithClickableLinks
import ch.protonmail.android.mailmessage.domain.usecase.LoadAvatarImage
import ch.protonmail.android.mailmessage.domain.usecase.ObserveAvatarImageStates
import ch.protonmail.android.mailmessage.domain.usecase.StarMessages
import ch.protonmail.android.mailmessage.domain.usecase.UnStarMessages
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.ContactActionsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.SnoozeSheetState
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailsettings.domain.model.PrivacySettings
import ch.protonmail.android.mailsettings.domain.model.ToolbarActionsRefreshSignal
import ch.protonmail.android.mailsettings.domain.usecase.privacy.ObservePrivacySettings
import ch.protonmail.android.mailsettings.domain.usecase.privacy.UpdateLinkConfirmationSetting
import ch.protonmail.android.mailsnooze.domain.SnoozeRepository
import ch.protonmail.android.mailsnooze.domain.model.UnsnoozeError
import ch.protonmail.android.mailsnooze.presentation.model.SnoozeConversationId
import ch.protonmail.android.testdata.action.ActionUiModelTestData
import ch.protonmail.android.testdata.avatar.AvatarImageStatesTestData
import ch.protonmail.android.testdata.contact.ContactActionsGroupsSample
import ch.protonmail.android.testdata.contact.ContactSample
import ch.protonmail.android.testdata.conversation.ConversationTestData
import ch.protonmail.android.testdata.conversation.ConversationUiModelTestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Suppress("LargeClass")
class ConversationDetailViewModelTest {

    private val userId = UserIdSample.Primary
    private val primaryUserAddress = UserAddressSample.PrimaryAddress.email
    private val conversationId = ConversationIdSample.WeatherForecast
    private val initialState = ConversationDetailState.Loading

    private val actionUiModelMapper = ActionUiModelMapper()
    private val messageIdUiModelMapper = MessageIdUiModelMapper()
    private val conversationMetadataMapper: ConversationDetailMetadataUiModelMapper = mockk {
        every { toUiModel(ConversationSample.WeatherForecast) } returns
            ConversationDetailMetadataUiModelSample.WeatherForecast
    }
    private val conversationMessageMapper: ConversationDetailMessageUiModelMapper = mockk {
        coEvery {
            toUiModel(
                message = MessageSample.Invoice,
                primaryUserAddress = primaryUserAddress,
                avatarImageState = any()
            )
        } returns
            ConversationDetailMessageUiModelSample.InvoiceWithLabel
        coEvery {
            toUiModel(
                message = MessageSample.Invoice,
                primaryUserAddress = primaryUserAddress,
                avatarImageState = any()
            )
        } returns
            ConversationDetailMessageUiModelSample.InvoiceWithTwoLabels
        coEvery {
            toUiModel(
                message = MessageSample.Invoice,
                primaryUserAddress = primaryUserAddress,
                avatarImageState = any()
            )
        } returns
            ConversationDetailMessageUiModelSample.InvoiceWithoutLabels
        coEvery {
            toUiModel(
                message = MessageSample.Invoice,
                primaryUserAddress = primaryUserAddress,
                avatarImageState = any()
            )
        } returns
            ConversationDetailMessageUiModelSample.AnotherInvoiceWithoutLabels
        coEvery {
            toUiModel(
                ofType(ConversationDetailMessageUiModel.Collapsed::class)
            )
        } returns
            InvoiceWithLabelExpanding
    }
    private val markConversationAsRead: MarkConversationAsRead = mockk()
    private val markConversationAsUnread: MarkConversationAsUnread = mockk()
    private val move: MoveConversation = mockk()
    private val deleteConversations: DeleteConversations = mockk()
    private val observeContacts: ObserveContacts = mockk {
        coEvery {
            this@mockk(userId = UserIdSample.Primary)
        } returns flowOf(emptyList<ContactMetadata.Contact>().right())
    }
    private val observeConversation: ObserveConversation = mockk {
        coEvery { this@mockk(UserIdSample.Primary, ConversationIdSample.WeatherForecast, any()) } returns
            flowOf(ConversationSample.WeatherForecast.right())
    }
    private val observeConversationMessages: ObserveConversationMessages = mockk {
        coEvery { this@mockk(UserIdSample.Primary, ConversationIdSample.WeatherForecast, any()) } returns flowOf(
            ConversationMessages(
                nonEmptyListOf(
                    MessageSample.Invoice,
                    MessageSample.Invoice
                ),
                MessageSample.Invoice.messageId
            ).right()
        )
    }
    private val observeDetailBottomBarActions = mockk<ObserveDetailBottomBarActions> {
        coEvery {
            this@mockk(UserIdSample.Primary, any(), ConversationIdSample.WeatherForecast)
        } returns flowOf(
            listOf(Action.Archive, Action.MarkUnread).right()
        )
    }
    private val observePrimaryUserId: ObservePrimaryUserId = mockk {
        every { this@mockk() } returns flowOf(UserIdSample.Primary)
    }
    private val getAttachmentIntentValues = mockk<GetAttachmentIntentValues>()
    private val getAttachmentDownloadStatus = mockk<GetDownloadingAttachmentsForMessages>()
    private val loadImageAvoidDuplicatedExecution = mockk<LoadImageAvoidDuplicatedExecution>()
    private val reducer: ConversationDetailReducer = mockk {
        coEvery { newStateFrom(currentState = any(), operation = any()) } returns ConversationDetailState.Loading
    }
    private val savedStateHandle: SavedStateHandle = mockk {
        every { get<String>(ConversationDetailScreen.ConversationIdKey) } returns conversationId.id
        every { get<String>(ConversationDetailScreen.ScrollToMessageIdKey) } returns null
        every { get<String>(ConversationDetailScreen.OpenedFromLocationKey) } returns "allmail"
        every { get<String>(ConversationDetailScreen.IsSingleMessageMode) } returns "false"
    }
    private val starMessages = mockk<StarMessages>()
    private val unStarMessages = mockk<UnStarMessages>()
    private val starConversations: StarConversations = mockk {
        coEvery { this@mockk.invoke(any(), any()) } returns listOf(ConversationTestData.starredConversation).right()
    }
    private val unStarConversations: UnStarConversations = mockk {
        coEvery {
            this@mockk.invoke(any(), any())
        } returns listOf(ConversationTestData.conversation).right()
    }
    private val getDecryptedMessageBody: GetMessageBodyWithClickableLinks = mockk {
        coEvery { this@mockk.invoke(any(), any(), any()) } returns DecryptedMessageBody(
            MessageIdSample.build(),
            "",
            isUnread = false,
            MimeType.Html,
            hasQuotedText = false,
            hasCalendarInvite = false,
            banners = emptyList()
        ).right()
    }
    private val markMessageAsRead: MarkMessageAsRead =
        mockk {
            coEvery { this@mockk.invoke(any(), any()) } returns Unit.right()
        }

    private val findContactByEmail: FindContactByEmail = mockk<FindContactByEmail> {
        coEvery { this@mockk.invoke(any(), any()) } returns ContactSample.Stefano
    }

    private val observePrimaryUserAddress = mockk<ObservePrimaryUserAddress> {
        every { this@mockk() } returns flowOf(primaryUserAddress)
    }

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

    private val inMemoryConversationStateRepository = FakeInMemoryConversationStateRepository()
    private val messageViewStateCache = MessageViewStateCache(inMemoryConversationStateRepository)
    private val observeConversationViewState = ObserveConversationViewState(inMemoryConversationStateRepository)
    private val reportPhishingMessage = mockk<ReportPhishingMessage>()
    private val isProtonCalendarInstalled = mockk<IsProtonCalendarInstalled>()
    private val markMessageAsUnread = mockk<MarkMessageAsUnread>()
    private val getMoreActionsBottomSheetData = mockk<GetMoreActionsBottomSheetData>()
    private val moveMessage = mockk<MoveMessage>()
    private val deleteMessages = mockk<DeleteMessages>()
    private val loadAvatarImage = mockk<LoadAvatarImage> {
        every { this@mockk.invoke(any(), any()) } returns Unit
    }
    private val observeAvatarImageStates = mockk<ObserveAvatarImageStates> {
        every { this@mockk() } returns flowOf(AvatarImageStatesTestData.SampleData1)
    }
    private val getMessagesInSameExclusiveLocation = mockk<GetMessagesInSameExclusiveLocation>()
    private val markMessageAsLegitimate = mockk<MarkMessageAsLegitimate>()
    private val unblockSender = mockk<UnblockSender>()
    private val cancelScheduleSendMessage = mockk<CancelScheduleSendMessage>()

    private val printMessage = mockk<PrintMessage>()

    private val getRsvpEvent = mockk<GetRsvpEvent>()
    private val answerRsvpEvent = mockk<AnswerRsvpEvent>()

    private val snoozeRepository = mockk<SnoozeRepository> {
        coEvery { this@mockk.unSnoozeConversation(any(), any(), any()) } returns Unit.right()
    }

    private val unsubscribeFromNewsletter = mockk<UnsubscribeFromNewsletter>()

    private val refreshToolbarSharedFlow = MutableSharedFlow<Unit>()
    private val toolbarRefreshSignal = mockk<ToolbarActionsRefreshSignal> {
        every { this@mockk.refreshEvents } returns refreshToolbarSharedFlow
    }

    private val testDispatcher: TestDispatcher by lazy {
        StandardTestDispatcher().apply { Dispatchers.setMain(this) }
    }

    private val viewModel by lazy {
        ConversationDetailViewModel(
            observePrimaryUserId = observePrimaryUserId,
            messageIdUiModelMapper = messageIdUiModelMapper,
            actionUiModelMapper = actionUiModelMapper,
            conversationMessageMapper = conversationMessageMapper,
            conversationMetadataMapper = conversationMetadataMapper,
            markConversationAsRead = markConversationAsRead,
            markConversationAsUnread = markConversationAsUnread,
            moveConversation = move,
            deleteConversations = deleteConversations,
            observeConversation = observeConversation,
            observeConversationMessages = observeConversationMessages,
            observeDetailActions = observeDetailBottomBarActions,
            getDownloadingAttachmentsForMessages = getAttachmentDownloadStatus,
            reducer = reducer,
            starConversations = starConversations,
            unStarConversations = unStarConversations,
            starMessages = starMessages,
            unStarMessages = unStarMessages,
            savedStateHandle = savedStateHandle,
            getMessageBodyWithClickableLinks = getDecryptedMessageBody,
            markMessageAsRead = markMessageAsRead,
            messageViewStateCache = messageViewStateCache,
            observeConversationViewState = observeConversationViewState,
            getAttachmentIntentValues = getAttachmentIntentValues,
            loadImageAvoidDuplicatedExecution = loadImageAvoidDuplicatedExecution,
            ioDispatcher = Dispatchers.Unconfined,
            observePrivacySettings = observePrivacySettings,
            updateLinkConfirmationSetting = updateLinkConfirmationSetting,
            reportPhishingMessage = reportPhishingMessage,
            isProtonCalendarInstalled = isProtonCalendarInstalled,
            markMessageAsUnread = markMessageAsUnread,
            findContactByEmail = findContactByEmail,
            getMoreActionsBottomSheetData = getMoreActionsBottomSheetData,
            moveMessage = moveMessage,
            deleteMessages = deleteMessages,
            observePrimaryUserAddress = observePrimaryUserAddress,
            loadAvatarImage = loadAvatarImage,
            observeAvatarImageStates = observeAvatarImageStates,
            getMessagesInSameExclusiveLocation = getMessagesInSameExclusiveLocation,
            markMessageAsLegitimate = markMessageAsLegitimate,
            unblockSender = unblockSender,
            cancelScheduleSendMessage = cancelScheduleSendMessage,
            printMessage = printMessage,
            getRsvpEvent = getRsvpEvent,
            answerRsvpEvent = answerRsvpEvent,
            snoozeRepository = snoozeRepository,
            unsubscribeFromNewsletter = unsubscribeFromNewsletter,
            toolbarRefreshSignal = toolbarRefreshSignal
        )
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading`() = runTest {
        // When
        viewModel.state.test {
            // Then
            assertEquals(ConversationDetailState.Loading, awaitItem())
        }
    }

    @Test
    fun `throws exception when conversation id parameter was not provided as input`() = runTest {
        // Given
        every { savedStateHandle.get<String>(ConversationDetailScreen.ConversationIdKey) } returns null

        // When
        val thrown = assertFailsWith<IllegalStateException> { viewModel.state }

        // Then
        assertEquals("No Conversation id given", thrown.message)
    }

    @Test
    fun `conversation state is data when use case succeeds`() = runTest {
        // given
        val conversationUiModel = ConversationDetailMetadataUiModelSample.WeatherForecast
        val expectedState = initialState.copy(
            conversationState = ConversationDetailMetadataState.Data(conversationUiModel)
        )
        coEvery {
            reducer.newStateFrom(
                currentState = initialState,
                operation = ofType<ConversationDetailEvent.ConversationData>()
            )
        } returns expectedState

        // when
        viewModel.state.test {
            initialStateEmitted()

            // then
            assertEquals(expectedState, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `conversation state is error loading when use case fails`() = runTest {
        // given
        val expectedState = initialState.copy(
            conversationState = ConversationDetailMetadataState.Error(
                message = TextUiModel(string.detail_error_loading_conversation)
            )
        )
        val labelId = LabelIdSample.AllMail
        every { savedStateHandle.get<String>(ConversationDetailScreen.OpenedFromLocationKey) } returns labelId.id
        coEvery {
            observeConversation(UserIdSample.Primary, ConversationIdSample.WeatherForecast, labelId)
        } returns flowOf(ConversationError.UnknownMessage.left())
        coEvery {
            reducer.newStateFrom(
                currentState = initialState,
                operation = ConversationDetailEvent.ErrorLoadingConversation
            )
        } returns expectedState

        // when
        viewModel.state.test {
            initialStateEmitted()

            // then
            assertEquals(expectedState, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `reducer is called with no network error when observe conversation fails with no network error`() = runTest {
        // given
        val dataState = initialState.copy(
            conversationState = ConversationDetailMetadataState.Data(
                ConversationDetailMetadataUiModelSample.WeatherForecast
            )
        )
        val labelId = LabelIdSample.AllMail
        every { savedStateHandle.get<String>(ConversationDetailScreen.OpenedFromLocationKey) } returns labelId.id

        coEvery {
            observeConversation(UserIdSample.Primary, ConversationIdSample.WeatherForecast, labelId)
        } returns flow {
            emit(ConversationSample.WeatherForecast.right())
            emit(ConversationError.Other(DataError.Remote.Http(NetworkError.NoNetwork)).left())
        }
        coEvery {
            reducer.newStateFrom(
                currentState = initialState,
                operation = ofType<ConversationDetailEvent.ConversationData>()
            )
        } returns dataState

        viewModel.state.test {
            initialStateEmitted()
            // then
            assertEquals(dataState, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { reducer.newStateFrom(currentState = dataState, operation = ConversationDetailEvent.NoNetworkError) }
    }

    @Test
    fun `conversation messages state is data when use case succeeds`() = runTest {
        // given
        val messagesUiModels = listOf(
            ConversationDetailMessageUiModelSample.InvoiceWithLabel,
            ConversationDetailMessageUiModelSample.InvoiceWithTwoLabels
        ).toImmutableList()
        val expectedState = initialState.copy(
            messagesState = ConversationDetailsMessagesState.Data(messagesUiModels)
        )
        coEvery {
            reducer.newStateFrom(
                currentState = initialState,
                operation = ofType<ConversationDetailEvent.MessagesData>()
            )
        } returns expectedState
        coEvery {
            conversationMessageMapper.toUiModel(
                message = any(),
                avatarImageState = any(),
                primaryUserAddress = primaryUserAddress,
                decryptedMessageBody = any(),
                attachmentListExpandCollapseMode = null,
                rsvpEventState = null
            )
        } returns InvoiceWithLabelExpanded

        // when
        viewModel.state.test {
            initialStateEmitted()

            // then
            assertEquals(expectedState, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `fallback on empty contacts list when contacts use case fails`() = runTest {
        // given
        val messagesUiModels = listOf(
            ConversationDetailMessageUiModelSample.InvoiceWithLabel,
            ConversationDetailMessageUiModelSample.InvoiceWithTwoLabels
        ).toImmutableList()
        val expectedState = initialState.copy(
            messagesState = ConversationDetailsMessagesState.Data(messagesUiModels)
        )
        coEvery { observeContacts(UserIdSample.Primary) } returns flowOf(GetContactError.left())
        coEvery {
            reducer.newStateFrom(
                currentState = initialState,
                operation = ofType<ConversationDetailEvent.MessagesData>()
            )
        } returns expectedState

        // when
        viewModel.state.test {
            initialStateEmitted()

            // then
            assertEquals(expectedState, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `conversation messages state is error loading when use case fails with remote error`() = runTest {
        // given
        val expectedState = initialState.copy(
            messagesState = ConversationDetailsMessagesState.Error(
                message = TextUiModel(string.detail_error_loading_messages)
            )
        )
        val labelId = LabelIdSample.AllMail
        every { savedStateHandle.get<String>(ConversationDetailScreen.OpenedFromLocationKey) } returns labelId.id
        coEvery {
            observeConversationMessages(UserIdSample.Primary, ConversationIdSample.WeatherForecast, labelId)
        } returns flowOf(ConversationError.Other(DataError.Remote.Http(NetworkError.ServerError)).left())
        coEvery {
            reducer.newStateFrom(
                currentState = initialState,
                operation = ConversationDetailEvent.ErrorLoadingMessages
            )
        } returns expectedState

        // when
        viewModel.state.test {
            initialStateEmitted()

            // then
            assertEquals(expectedState, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `conversation messages state is offline when use case fails with no network error`() = runTest {
        // given
        val expectedState = initialState.copy(messagesState = ConversationDetailsMessagesState.Offline)
        val labelId = LabelIdSample.AllMail
        every { savedStateHandle.get<String>(ConversationDetailScreen.OpenedFromLocationKey) } returns labelId.id
        coEvery {
            observeConversationMessages(UserIdSample.Primary, ConversationIdSample.WeatherForecast, labelId)
        } returns flowOf(ConversationError.Other(DataError.Remote.Http(NetworkError.NoNetwork)).left())
        coEvery {
            reducer.newStateFrom(
                currentState = initialState,
                operation = ConversationDetailEvent.NoNetworkError
            )
        } returns expectedState

        // when
        viewModel.state.test {
            initialStateEmitted()

            // then
            assertEquals(expectedState, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `bottom bar state is data when use case returns actions`() = runTest {
        // Given
        val messages = nonEmptyListOf(ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded)
        coEvery {
            conversationMessageMapper.toUiModel(
                message = any(),
                avatarImageState = any(),
                primaryUserAddress = primaryUserAddress,
                decryptedMessageBody = any(),
                attachmentListExpandCollapseMode = null,
                rsvpEventState = null
            )
        } returns messages.first()
        val actions = listOf(Action.Archive)
        val actionUiModels = listOf(ActionUiModelTestData.archive).toImmutableList()
        val labelId = LabelIdSample.Archive
        val expected = initialState.copy(bottomBarState = BottomBarState.Data.Shown(actionUiModels))
        every { savedStateHandle.get<String>(ConversationDetailScreen.OpenedFromLocationKey) } returns labelId.id
        coEvery {
            observeDetailBottomBarActions(UserIdSample.Primary, labelId, ConversationIdSample.WeatherForecast)
        } returns flowOf(actions.right())
        coEvery {
            reducer.newStateFrom(
                currentState = initialState,
                operation = ofType<ConversationDetailEvent.ConversationBottomBarEvent>()
            )
        } returns expected

        // when
        viewModel.state.test {
            initialStateEmitted()

            // Then
            assertEquals(expected.bottomBarState, awaitItem().bottomBarState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `bottom bar state is failed loading actions when use case returns error`() = runTest {
        // Given
        val messages = nonEmptyListOf(ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded)
        val labelId = LabelIdSample.Trash
        val expected = initialState.copy(bottomBarState = BottomBarState.Error.FailedLoadingActions)
        coEvery {
            conversationMessageMapper.toUiModel(
                message = any(),
                avatarImageState = any(),
                primaryUserAddress = primaryUserAddress,
                decryptedMessageBody = any(),
                attachmentListExpandCollapseMode = null,
                rsvpEventState = null
            )
        } returns messages.first()
        every { savedStateHandle.get<String>(ConversationDetailScreen.OpenedFromLocationKey) } returns labelId.id
        coEvery {
            observeDetailBottomBarActions(UserIdSample.Primary, labelId, ConversationIdSample.WeatherForecast)
        } returns flowOf(DataError.Local.NoDataCached.left())
        coEvery {
            reducer.newStateFrom(
                currentState = initialState,
                operation = ofType<ConversationDetailEvent.ConversationBottomBarEvent>()
            )
        } returns expected

        // When
        viewModel.state.test {
            initialStateEmitted()

            // Then
            assertEquals(expected.bottomBarState, awaitItem().bottomBarState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when toolbar refresh signal is emitted, bottom bar actions are updated and emitted`() = runTest {
        // Given
        val actions = listOf(Action.Archive, Action.MarkUnread)
        val actionUiModels = listOf(ActionUiModelTestData.archive, ActionUiModelTestData.markUnread).toImmutableList()
        val labelId = LabelIdSample.AllMail
        val expectedState = initialState.copy(bottomBarState = BottomBarState.Data.Shown(actionUiModels))

        every { savedStateHandle.get<String>(ConversationDetailScreen.OpenedFromLocationKey) } returns labelId.id
        coEvery {
            observeDetailBottomBarActions(UserIdSample.Primary, labelId, ConversationIdSample.WeatherForecast)
        } returns flowOf(actions.right())
        coEvery {
            reducer.newStateFrom(
                currentState = any(),
                operation = ofType<ConversationDetailEvent.ConversationBottomBarEvent>()
            )
        } returns expectedState

        // When + Then
        viewModel.state.test {
            initialStateEmitted()
            assertEquals(expectedState.bottomBarState, awaitItem().bottomBarState)

            // Emit toolbar refresh signal to trigger a new observeDetailBottomBarActions call
            refreshToolbarSharedFlow.emit(Unit)
            advanceUntilIdle()

            coVerify(exactly = 2) {
                observeDetailBottomBarActions(UserIdSample.Primary, labelId, ConversationIdSample.WeatherForecast)
            }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `starred conversation metadata is emitted when star action is successful`() = runTest {
        // given
        givenReducerReturnsStarredUiModel()

        viewModel.state.test {
            advanceUntilIdle()

            // When
            viewModel.submit(ConversationDetailViewAction.Star)
            advanceUntilIdle()

            // Then
            val actual = assertIs<ConversationDetailMetadataState.Data>(lastEmittedItem().conversationState)
            assertTrue(actual.conversationUiModel.isStarred)
        }
    }

    @Test
    fun `verify order of emitted states when starring a conversation`() = runTest {
        // Given
        val labelId = LabelIdSample.Spam
        every { savedStateHandle.get<String>(ConversationDetailScreen.OpenedFromLocationKey) } returns labelId.id
        val actionUiModels = listOf(
            ActionUiModelTestData.archive,
            ActionUiModelTestData.markUnread
        ).toImmutableList()
        givenReducerReturnsStarredUiModel()
        givenReducerReturnsBottomActions()

        // When
        viewModel.state.test {
            initialStateEmitted()

            // Then
            val bottomBarState = ConversationDetailState.Loading.copy(
                bottomBarState = BottomBarState.Data.Shown(actionUiModels)
            )
            assertEquals(bottomBarState, awaitItem())

            viewModel.submit(ConversationDetailViewAction.Star)
            advanceUntilIdle()

            val actual = assertIs<ConversationDetailMetadataState.Data>(lastEmittedItem().conversationState)
            assertTrue(actual.conversationUiModel.isStarred)
        }
    }

    @Test
    fun `error starring conversation is emitted when star action fails`() = runTest {
        // Given
        coEvery { starConversations.invoke(UserIdSample.Primary, any()) } returns DataError.Local.NoDataCached.left()
        coEvery {
            reducer.newStateFrom(
                currentState = ConversationDetailState.Loading,
                operation = ConversationDetailEvent.ErrorAddStar
            )
        } returns ConversationDetailState.Loading.copy(
            error = Effect.of(
                TextUiModel(string.error_star_operation_failed)
            )
        )
        viewModel.state.test {
            initialStateEmitted()

            // When
            viewModel.submit(ConversationDetailViewAction.Star)

            // Then
            assertEquals(TextUiModel(string.error_star_operation_failed), awaitItem().error.consume())
            coVerify(exactly = 1) { reducer.newStateFrom(any(), ConversationDetailEvent.ErrorAddStar) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `unStarred conversation metadata is emitted when unStar action is successful`() = runTest {
        // given
        coEvery {
            reducer.newStateFrom(
                currentState = ConversationDetailState.Loading,
                operation = ConversationDetailViewAction.UnStar
            )
        } returns ConversationDetailState.Loading.copy(
            conversationState = ConversationDetailMetadataState.Data(
                ConversationUiModelTestData.conversationUiModel
            )
        )

        // When
        viewModel.state.test {
            advanceUntilIdle()
            viewModel.submit(ConversationDetailViewAction.UnStar)
            advanceUntilIdle()

            // Then
            val actual = assertIs<ConversationDetailMetadataState.Data>(lastEmittedItem().conversationState)
            assertFalse(actual.conversationUiModel.isStarred)
        }
    }

    @Test
    fun `error unStarring conversation is emitted when unStar action fails`() = runTest {
        // Given
        coEvery { unStarConversations.invoke(UserIdSample.Primary, any()) } returns DataError.Local.NoDataCached.left()
        coEvery {
            reducer.newStateFrom(
                currentState = ConversationDetailState.Loading,
                operation = ConversationDetailEvent.ErrorRemoveStar
            )
        } returns ConversationDetailState.Loading.copy(
            error = Effect.of(
                TextUiModel(string.error_unstar_operation_failed)
            )
        )

        // When
        viewModel.state.test {
            initialStateEmitted()
            viewModel.submit(ConversationDetailViewAction.UnStar)

            // Then
            assertEquals(TextUiModel(string.error_unstar_operation_failed), awaitItem().error.consume())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `error moving to trash is emitted when action fails`() = runTest {
        // Given
        coEvery {
            move(
                UserIdSample.Primary,
                conversationId,
                SystemLabelId.Trash
            )
        } returns DataError.Local.NoDataCached.left()
        coEvery {
            reducer.newStateFrom(
                currentState = ConversationDetailState.Loading,
                operation = ConversationDetailEvent.ErrorMovingToTrash
            )
        } returns ConversationDetailState.Loading.copy(
            error = Effect.of(TextUiModel(string.error_move_to_trash_failed))
        )

        // When
        viewModel.state.test {
            initialStateEmitted()
            viewModel.submit(ConversationDetailViewAction.MoveToTrash)

            // Then
            assertEquals(TextUiModel(string.error_move_to_trash_failed), awaitItem().error.consume())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `exit with message is emitted when success moving to trash`() = runTest {
        // Given
        coEvery {
            move(
                UserIdSample.Primary,
                conversationId,
                SystemLabelId.Trash
            )
        } returns Unit.right()
        coEvery {
            reducer.newStateFrom(
                currentState = ConversationDetailState.Loading,
                operation = ConversationDetailEvent.ExitScreenWithMessage(ConversationDetailViewAction.MoveToTrash)
            )
        } returns ConversationDetailState.Loading.copy(
            exitScreenActionResult = Effect.of(
                ActionResult.UndoableActionResult(TextUiModel(string.conversation_moved_to_trash))
            )
        )

        // When
        viewModel.state.test {
            initialStateEmitted()
            viewModel.submit(ConversationDetailViewAction.MoveToTrash)

            // Then
            assertNotNull(awaitItem().exitScreenActionResult.consume())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `verify exit with message is emitted when destination get confirmed (conversation)`() = runTest {
        // Given
        val moveToEntryPoint = MoveToBottomSheetEntryPoint.Conversation
        val labelText = MailLabelText("selectedLabel")

        coEvery {
            reducer.newStateFrom(
                currentState = any(),
                operation = ConversationDetailEvent.ExitScreenWithMessage(
                    ConversationDetailViewAction.MoveToCompleted(
                        labelText, moveToEntryPoint
                    )
                )
            )
        } returns ConversationDetailState.Loading.copy(
            exitScreenActionResult = Effect.of(
                ActionResult.UndoableActionResult(
                    TextUiModel(string.conversation_moved_to_selected_destination, "selectedLabel")
                )
            )
        )

        // When
        viewModel.state.test {
            initialStateEmitted()

            viewModel.submit(ConversationDetailViewAction.MoveToCompleted(labelText, moveToEntryPoint))
            advanceUntilIdle()

            // Then
            assertNotNull(awaitItem().exitScreenActionResult.consume())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `mark as unread is called correctly when action is submitted`() = runTest {
        // given
        val labelId = LabelIdSample.AllMail
        every { savedStateHandle.get<String>(ConversationDetailScreen.OpenedFromLocationKey) } returns labelId.id
        coEvery { markConversationAsUnread(userId, labelId, conversationId) } returns Unit.right()

        // when
        viewModel.submit(ConversationDetailViewAction.MarkUnread)
        advanceUntilIdle()

        // then
        coVerify { markConversationAsUnread(userId, labelId, conversationId) }
    }

    @Test
    fun `exit state is emitted when marked as unread successfully`() = runTest {
        // given
        val labelId = LabelIdSample.AllMail
        every { savedStateHandle.get<String>(ConversationDetailScreen.OpenedFromLocationKey) } returns labelId.id
        coEvery { markConversationAsUnread(userId, labelId, conversationId) } returns Unit.right()
        coEvery {
            reducer.newStateFrom(
                currentState = ConversationDetailState.Loading,
                operation = ConversationDetailEvent.ExitScreen
            )
        } returns ConversationDetailState.Loading.copy(
            exitScreenEffect = Effect.of(Unit)
        )

        // when
        viewModel.state.test {
            initialStateEmitted()
            viewModel.submit(ConversationDetailViewAction.MarkUnread)

            // then
            assertNotNull(awaitItem().exitScreenEffect.consume())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `exit state is emitted when moving the conversation item back to inbox`() = runTest {
        // given
        val labelId = LabelIdSample.Archive
        every { savedStateHandle.get<String>(ConversationDetailScreen.OpenedFromLocationKey) } returns labelId.id
        coEvery { move(userId, conversationId, SystemLabelId.Inbox) } returns Unit.right()
        coEvery {
            reducer.newStateFrom(
                currentState = ConversationDetailState.Loading,
                operation = ConversationDetailEvent.ExitScreenWithMessage(MoveToInbox)
            )
        } returns ConversationDetailState.Loading.copy(
            exitScreenEffect = Effect.of(Unit)
        )

        // when
        viewModel.state.test {
            initialStateEmitted()
            viewModel.submit(MoveToInbox)

            // then
            assertNotNull(awaitItem().exitScreenEffect.consume())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `error message is emitted when mark as unread fails`() = runTest {
        // given
        val labelId = LabelIdSample.AllMail
        every { savedStateHandle.get<String>(ConversationDetailScreen.OpenedFromLocationKey) } returns labelId.id
        coEvery {
            markConversationAsUnread(userId, labelId, conversationId)
        } returns DataError.Local.NoDataCached.left()
        coEvery {
            reducer.newStateFrom(
                currentState = ConversationDetailState.Loading,
                operation = ConversationDetailEvent.ErrorMarkingAsUnread
            )
        } returns ConversationDetailState.Loading.copy(
            error = Effect.of(TextUiModel(string.error_mark_as_unread_failed))
        )

        // when
        viewModel.state.test {
            initialStateEmitted()
            viewModel.submit(ConversationDetailViewAction.MarkUnread)

            // then
            assertEquals(TextUiModel(string.error_mark_as_unread_failed), awaitItem().error.consume())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should mark message and conversation as read when expanding it successfully`() = runTest {
        // given
        val (messageIds, expectedExpanded) = setupCollapsedToExpandMessagesState(withUnreadMessage = true)

        coEvery { getDecryptedMessageBody(userId, MessageId(messageIds.first().id)) } returns DecryptedMessageBody(
            MessageIdSample.build(),
            "",
            isUnread = true,
            MimeType.Html,
            hasQuotedText = false,
            hasCalendarInvite = false,
            banners = emptyList()
        ).right()

        viewModel.state.test {
            conversationMessagesEmitted()

            // when
            viewModel.submit(ConversationDetailViewAction.ExpandMessage(messageIds.first()))
            advanceUntilIdle()

            // then
            coVerify {
                markMessageAsRead(
                    userId, MessageId(expectedExpanded.messageId.id)
                )
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should emit click effect when a link click is submitted`() = runTest {
        // given
        val messageId = MessageIdUiModel(MessageIdSample.AugWeatherForecast.id)

        // Mock the Uri class
        val mockUri = mockk<Uri>(relaxed = true)
        setupLinkClickState(messageId, mockUri)

        viewModel.state.test {
            initialStateEmitted()
            // when
            viewModel.submit(ConversationDetailViewAction.MessageBodyLinkClicked(messageId, mockUri))

            // then
            assertEquals(MessageBodyLink(messageId, mockUri), awaitItem().openMessageBodyLinkEffect.consume())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should emit decryption error when the message can not be decrypted and messages stay collapsed`() = runTest {
        // given
        val (messageIds, _) = setupCollapsedToExpandMessagesState()
        coEvery {
            getDecryptedMessageBody(
                userId,
                MessageId(messageIds.first().id)
            )
        } returns GetMessageBodyError.Decryption(MessageId(messageIds.first().id), "").left()

        viewModel.state.test {
            conversationMessagesEmitted()

            // when
            viewModel.submit(ConversationDetailViewAction.ExpandMessage(messageIds.first()))
            advanceUntilIdle()

            // then
            val errorState = awaitItem()
            assertEquals(TextUiModel(string.decryption_error), errorState.error.consume())

            val collapsedMessage = (errorState.messagesState as ConversationDetailsMessagesState.Data)
                .messages
                .first { it.messageId == messageIds.first() }
            assertIs<ConversationDetailMessageUiModel.Collapsed>(collapsedMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should emit retrieve message error when the message can not be loaded and the message stays collapsed`() =
        runTest {
            // given
            val (messageIds, _) = setupCollapsedToExpandMessagesState()
            coEvery {
                getDecryptedMessageBody(
                    userId,
                    MessageId(messageIds.first().id)
                )
            } returns GetMessageBodyError.Data(DataErrorSample.Unreachable).left()

            viewModel.state.test {
                conversationMessagesEmitted()

                // when
                viewModel.submit(ConversationDetailViewAction.ExpandMessage(messageIds.first()))
                advanceUntilIdle()

                // then
                val errorState = awaitItem()
                assertEquals(errorState.error.consume(), TextUiModel(string.detail_error_retrieving_message_body))

                val collapsedMessage = (errorState.messagesState as ConversationDetailsMessagesState.Data)
                    .messages
                    .first { it.messageId == messageIds.first() }
                assertIs<ConversationDetailMessageUiModel.Collapsed>(collapsedMessage)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `should emit offline error when offline and the message can not be loaded and messages stay collapsed`() =
        runTest {
            // given
            val (messageIds, _) = setupCollapsedToExpandMessagesState()
            coEvery {
                getDecryptedMessageBody(
                    userId,
                    MessageId(messageIds.first().id)
                )
            } returns GetMessageBodyError.Data(DataErrorSample.Offline).left()

            viewModel.state.test {
                conversationMessagesEmitted()

                // when
                viewModel.submit(ConversationDetailViewAction.ExpandMessage(messageIds.first()))
                advanceUntilIdle()

                // then
                val errorState = awaitItem()
                assertEquals(errorState.error.consume(), TextUiModel(string.error_offline_loading_message))

                val collapsedMessage = (errorState.messagesState as ConversationDetailsMessagesState.Data)
                    .messages
                    .first { it.messageId == messageIds.first() }
                assertIs<ConversationDetailMessageUiModel.Collapsed>(collapsedMessage)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Should expand automatically the message in the conversation`() = runTest {
        // given
        val messages = nonEmptyListOf(
            InvoiceWithLabelExpanded
        ).toImmutableList()
        coEvery {
            conversationMessageMapper.toUiModel(
                message = any(),
                avatarImageState = any(),
                primaryUserAddress = primaryUserAddress,
                decryptedMessageBody = any(),
                attachmentListExpandCollapseMode = null,
                rsvpEventState = null
            )
        } returns messages.first()
        coEvery {
            reducer.newStateFrom(
                currentState = any(),
                operation = any()
            )
        } returns ConversationDetailState.Loading.copy(
            messagesState = ConversationDetailsMessagesState.Data(messages)
        )

        viewModel.state.test {
            initialStateEmitted()

            // when
            advanceUntilIdle()
            val newState = awaitItem().messagesState as ConversationDetailsMessagesState.Data

            // then
            val expandedMessage =
                newState
                    .messages
                    .first { it.messageId == InvoiceWithLabelExpanded.messageId }
            assertIs<ConversationDetailMessageUiModel.Expanded>(expandedMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should request the message body again when expand quoted body is invoked`() = runTest {
        // Given
        val messages = nonEmptyListOf(InvoiceWithLabelExpanded).toImmutableList()
        val messageId = InvoiceWithLabelExpanded.messageId

        coEvery {
            conversationMessageMapper.toUiModel(
                message = any(),
                avatarImageState = any(),
                primaryUserAddress = primaryUserAddress,
                decryptedMessageBody = any(),
                attachmentListExpandCollapseMode = null,
                rsvpEventState = null
            )
        } returns messages.first()

        coEvery {
            reducer.newStateFrom(
                currentState = any(),
                operation = any()
            )
        } returns ConversationDetailState.Loading.copy(
            messagesState = ConversationDetailsMessagesState.Data(messages)
        )

        viewModel.state.test {
            initialStateEmitted()

            advanceUntilIdle()
            val newState = awaitItem().messagesState as ConversationDetailsMessagesState.Data

            val expandedMessage = newState.messages.first { it.messageId == messageId }
            assertIs<ConversationDetailMessageUiModel.Expanded>(expandedMessage)

            viewModel.submit(ConversationDetailViewAction.ExpandOrCollapseMessageBody(messageId))
            advanceUntilIdle()

            cancelAndIgnoreRemainingEvents()

            val transformations = MessageBodyTransformations.MessageDetailsDefaults.copy(showQuotedText = true)

            coVerify(exactly = 1) {
                getDecryptedMessageBody(userId, MessageId(messageId.id), transformations)
            }
            confirmVerified(getDecryptedMessageBody)
        }
    }

    @Test
    fun `should request the message body again when load remote content is invoked`() = runTest {
        // Given
        val messages = nonEmptyListOf(InvoiceWithLabelExpanded).toImmutableList()
        val messageId = InvoiceWithLabelExpanded.messageId

        coEvery {
            conversationMessageMapper.toUiModel(
                message = any(),
                avatarImageState = any(),
                primaryUserAddress = primaryUserAddress,
                decryptedMessageBody = any(),
                attachmentListExpandCollapseMode = null,
                rsvpEventState = null
            )
        } returns messages.first()

        coEvery {
            reducer.newStateFrom(
                currentState = any(),
                operation = any()
            )
        } returns ConversationDetailState.Loading.copy(
            messagesState = ConversationDetailsMessagesState.Data(messages)
        )

        viewModel.state.test {
            initialStateEmitted()

            advanceUntilIdle()
            val newState = awaitItem().messagesState as ConversationDetailsMessagesState.Data

            val expandedMessage = newState.messages.first { it.messageId == messageId }
            assertIs<ConversationDetailMessageUiModel.Expanded>(expandedMessage)

            viewModel.submit(ConversationDetailViewAction.LoadRemoteContent(messageId))
            advanceUntilIdle()

            cancelAndIgnoreRemainingEvents()

            val transformations = MessageBodyTransformations.MessageDetailsDefaults.copy(hideRemoteContent = false)

            coVerify(exactly = 1) {
                getDecryptedMessageBody(userId, MessageId(messageId.id), transformations)
            }
            confirmVerified(getDecryptedMessageBody)
        }
    }

    @Test
    fun `should request the message body again when load embedded images is invoked`() = runTest {
        // Given
        val messages = nonEmptyListOf(InvoiceWithLabelExpanded).toImmutableList()
        val messageId = InvoiceWithLabelExpanded.messageId

        coEvery {
            conversationMessageMapper.toUiModel(
                message = any(),
                avatarImageState = any(),
                primaryUserAddress = primaryUserAddress,
                decryptedMessageBody = any(),
                attachmentListExpandCollapseMode = null,
                rsvpEventState = null
            )
        } returns messages.first()

        coEvery {
            reducer.newStateFrom(
                currentState = any(),
                operation = any()
            )
        } returns ConversationDetailState.Loading.copy(
            messagesState = ConversationDetailsMessagesState.Data(messages)
        )

        viewModel.state.test {
            initialStateEmitted()

            advanceUntilIdle()
            val newState = awaitItem().messagesState as ConversationDetailsMessagesState.Data

            val expandedMessage = newState.messages.first { it.messageId == messageId }
            assertIs<ConversationDetailMessageUiModel.Expanded>(expandedMessage)

            viewModel.submit(ConversationDetailViewAction.ShowEmbeddedImages(messageId))
            advanceUntilIdle()

            cancelAndIgnoreRemainingEvents()

            val transformations = MessageBodyTransformations.MessageDetailsDefaults.copy(hideEmbeddedImages = false)

            coVerify(exactly = 1) {
                getDecryptedMessageBody(userId, MessageId(messageId.id), transformations)
            }
            confirmVerified(getDecryptedMessageBody)
        }
    }

    @Test
    fun `should request the message body again when load remote + embedded images is invoked`() = runTest {
        // Given
        val messages = nonEmptyListOf(InvoiceWithLabelExpanded).toImmutableList()
        val messageId = InvoiceWithLabelExpanded.messageId

        coEvery {
            conversationMessageMapper.toUiModel(
                message = any(),
                avatarImageState = any(),
                primaryUserAddress = primaryUserAddress,
                decryptedMessageBody = any(),
                attachmentListExpandCollapseMode = null,
                rsvpEventState = null
            )
        } returns messages.first()

        coEvery {
            reducer.newStateFrom(
                currentState = any(),
                operation = any()
            )
        } returns ConversationDetailState.Loading.copy(
            messagesState = ConversationDetailsMessagesState.Data(messages)
        )

        viewModel.state.test {
            initialStateEmitted()

            advanceUntilIdle()
            val newState = awaitItem().messagesState as ConversationDetailsMessagesState.Data

            val expandedMessage = newState.messages.first { it.messageId == messageId }
            assertIs<ConversationDetailMessageUiModel.Expanded>(expandedMessage)

            viewModel.submit(ConversationDetailViewAction.LoadRemoteAndEmbeddedContent(messageId))
            advanceUntilIdle()

            cancelAndIgnoreRemainingEvents()

            val transformations = MessageBodyTransformations.MessageDetailsDefaults.copy(
                hideEmbeddedImages = false,
                hideRemoteContent = false
            )

            coVerify(exactly = 1) {
                getDecryptedMessageBody(userId, MessageId(messageId.id), transformations)
            }
            confirmVerified(getDecryptedMessageBody)
        }
    }

    @Test
    fun `Should observe conversation metadata refreshing the remote data`() = runTest {
        // Given
        val messages = nonEmptyListOf(ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded)
        coEvery {
            conversationMessageMapper.toUiModel(
                message = any(),
                avatarImageState = any(),
                primaryUserAddress = primaryUserAddress,
                decryptedMessageBody = any(),
                attachmentListExpandCollapseMode = null,
                rsvpEventState = null
            )
        } returns messages.first()

        // When
        viewModel.state.test {
            advanceUntilIdle()

            // Then
            coVerify { observeConversation(any(), any(), any()) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `verify contact actions bottom sheet data is build correctly`() = runTest {
        // Given
        val messages = nonEmptyListOf(ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded)
        coEvery {
            conversationMessageMapper.toUiModel(
                message = any(),
                avatarImageState = any(),
                primaryUserAddress = primaryUserAddress,
                decryptedMessageBody = any(),
                attachmentListExpandCollapseMode = null,
                rsvpEventState = null
            )
        } returns messages.first()
        val participant = Participant(
            "test@proton.me",
            "Test User"
        )
        val participantUiModel =
            ParticipantUiModel(
                participant.name,
                participant.address,
                R.drawable.ic_proton_lock,
                shouldShowOfficialBadge = false
            )
        val avatar = ParticipantAvatarSample.ebay

        val event = ContactActionsBottomSheetState.ContactActionsBottomSheetEvent.ActionData(
            participant = participant,
            avatarUiModel = avatar,
            contactId = ContactSample.Stefano.id
        )

        val expectedResult = ConversationDetailState.Loading.copy(
            bottomSheetState = BottomSheetState(
                ContactActionsBottomSheetState.Data(
                    participant = participant,
                    avatarUiModel = avatar,
                    actions = ContactActionsGroupsSample.defaultForContact(participant)
                )
            )
        )
        val labelId = LabelIdSample.AllMail
        every { savedStateHandle.get<String>(ConversationDetailScreen.OpenedFromLocationKey) } returns labelId.id

        coEvery { observeConversationMessages(userId, conversationId, labelId) } returns flowOf(
            ConversationMessages(
                nonEmptyListOf(
                    MessageSample.Invoice,
                    MessageSample.Invoice
                ),
                MessageSample.Invoice.messageId
            ).right()
        )

        coEvery {
            reducer.newStateFrom(
                any(),
                ConversationDetailEvent.ConversationBottomSheetEvent(event)
            )
        } returns expectedResult

        // When
        viewModel.state.test {
            viewModel.submit(
                ConversationDetailViewAction.RequestContactActionsBottomSheet(
                    participant = participantUiModel,
                    avatarUiModel = avatar
                )
            )
            // Request bottom Sheet call, we can ignore that
            awaitItem()

            // Then
            val lastItem = awaitItem()
            assertEquals(expectedResult, lastItem)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `exit state is emitted when marked as read successfully`() = runTest {
        // given
        val labelId = LabelIdSample.AllMail
        every { savedStateHandle.get<String>(ConversationDetailScreen.OpenedFromLocationKey) } returns labelId.id
        coEvery { markConversationAsRead(userId, labelId, conversationId) } returns Unit.right()
        coEvery {
            reducer.newStateFrom(
                currentState = ConversationDetailState.Loading,
                operation = ConversationDetailEvent.ExitScreen
            )
        } returns ConversationDetailState.Loading.copy(
            exitScreenEffect = Effect.of(Unit)
        )

        // when
        viewModel.state.test {
            initialStateEmitted()
            viewModel.submit(ConversationDetailViewAction.MarkRead)

            // then
            assertNotNull(awaitItem().exitScreenEffect.consume())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `error message is emitted when mark as read fails`() = runTest {
        // given
        val labelId = LabelIdSample.AllMail
        every { savedStateHandle.get<String>(ConversationDetailScreen.OpenedFromLocationKey) } returns labelId.id
        coEvery { markConversationAsRead(userId, labelId, conversationId) } returns DataError.Local.NoDataCached.left()
        coEvery {
            reducer.newStateFrom(
                currentState = ConversationDetailState.Loading,
                operation = ConversationDetailEvent.ErrorMarkingAsRead
            )
        } returns ConversationDetailState.Loading.copy(
            error = Effect.of(TextUiModel(string.error_mark_as_read_failed))
        )

        // when
        viewModel.state.test {
            initialStateEmitted()
            viewModel.submit(ConversationDetailViewAction.MarkRead)

            // then
            assertEquals(TextUiModel(string.error_mark_as_read_failed), awaitItem().error.consume())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `exit state is emitted when move to spam successfully`() = runTest {
        // given
        coEvery { move(userId, conversationId, SystemLabelId.Spam) } returns Unit.right()
        coEvery {
            reducer.newStateFrom(
                currentState = ConversationDetailState.Loading,
                operation = ConversationDetailEvent.ExitScreenWithMessage(
                    ConversationDetailViewAction.MoveToSpam
                )
            )
        } returns ConversationDetailState.Loading.copy(
            exitScreenEffect = Effect.of(Unit)
        )

        // when
        viewModel.state.test {
            initialStateEmitted()
            viewModel.submit(ConversationDetailViewAction.MoveToSpam)

            // then
            assertNotNull(awaitItem().exitScreenEffect.consume())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `error message is emitted when move to spam fails`() = runTest {
        // given
        coEvery { move(userId, conversationId, SystemLabelId.Spam) } returns DataError.Local.NoDataCached.left()
        coEvery {
            reducer.newStateFrom(
                currentState = ConversationDetailState.Loading,
                operation = ConversationDetailEvent.ErrorMovingConversation
            )
        } returns ConversationDetailState.Loading.copy(
            error = Effect.of(TextUiModel(string.error_move_to_spam_failed))
        )

        // when
        viewModel.state.test {
            initialStateEmitted()
            viewModel.submit(ConversationDetailViewAction.MoveToSpam)

            // then
            assertEquals(TextUiModel(string.error_move_to_spam_failed), awaitItem().error.consume())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `exit state is emitted when move to archive successfully`() = runTest {
        // given
        coEvery { move(userId, conversationId, SystemLabelId.Archive) } returns Unit.right()
        coEvery {
            reducer.newStateFrom(
                currentState = ConversationDetailState.Loading,
                operation = ConversationDetailEvent.ExitScreenWithMessage(
                    ConversationDetailViewAction.MoveToArchive
                )
            )
        } returns ConversationDetailState.Loading.copy(
            exitScreenEffect = Effect.of(Unit)
        )

        // when
        viewModel.state.test {
            initialStateEmitted()
            viewModel.submit(ConversationDetailViewAction.MoveToArchive)

            // then
            assertNotNull(awaitItem().exitScreenEffect.consume())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `error message is emitted when move to archive fails`() = runTest {
        // given
        coEvery {
            move(userId, conversationId, SystemLabelId.Archive)
        } returns DataError.Local.NoDataCached.left()
        coEvery {
            reducer.newStateFrom(
                currentState = ConversationDetailState.Loading,
                operation = ConversationDetailEvent.ErrorMovingConversation
            )
        } returns ConversationDetailState.Loading.copy(
            error = Effect.of(TextUiModel(string.error_move_to_archive_failed))
        )

        // when
        viewModel.state.test {
            initialStateEmitted()
            viewModel.submit(ConversationDetailViewAction.MoveToArchive)

            // then
            assertEquals(TextUiModel(string.error_move_to_archive_failed), awaitItem().error.consume())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should request message body with theme override when SwitchViewMode is invoked`() = runTest {
        // Given
        val messageId = MessageIdSample.Invoice
        val currentTheme = MessageTheme.Light
        val overrideTheme = MessageTheme.Dark

        val messages = nonEmptyListOf(InvoiceWithLabelExpanded).toImmutableList()
        coEvery {
            conversationMessageMapper.toUiModel(
                message = any(),
                avatarImageState = any(),
                primaryUserAddress = primaryUserAddress,
                decryptedMessageBody = any(),
                attachmentListExpandCollapseMode = null,
                rsvpEventState = null
            )
        } returns messages.first()
        coEvery {
            reducer.newStateFrom(
                currentState = any(),
                operation = any()
            )
        } returns ConversationDetailState.Loading.copy(
            messagesState = ConversationDetailsMessagesState.Data(messages)
        )

        // When
        viewModel.state.test {
            initialStateEmitted()
            advanceUntilIdle()

            viewModel.submit(
                ConversationDetailViewAction.SwitchViewMode(
                    messageId = messageId,
                    currentTheme = currentTheme,
                    overrideTheme = overrideTheme
                )
            )
            advanceUntilIdle()

            // Then
            val expected = MessageBodyTransformations.MessageDetailsDefaults.copy(
                messageThemeOptions = MessageThemeOptions(
                    currentTheme = currentTheme,
                    themeOverride = overrideTheme
                )
            )

            coVerify(exactly = 1) {
                getDecryptedMessageBody(userId, MessageId(messageId.id), expected)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should update expand collapse mode when expand or collapse attachment list is invoked`() = runTest {
        // Given
        val messageId = InvoiceWithLabelExpanded.messageId
        val messages = nonEmptyListOf(InvoiceWithLabelExpanded).toImmutableList()
        val expectedExpandCollapseMode = AttachmentListExpandCollapseMode.Collapsed

        coEvery {
            conversationMessageMapper.toUiModel(
                message = MessageSample.Invoice,
                avatarImageState = any(),
                primaryUserAddress = primaryUserAddress,
                decryptedMessageBody = any(),
                attachmentListExpandCollapseMode = null,
                rsvpEventState = null
            )
        } returns messages.first()

        coEvery {
            reducer.newStateFrom(
                currentState = any(),
                operation = any()
            )
        } returns ConversationDetailState.Loading.copy(
            messagesState = ConversationDetailsMessagesState.Data(messages)
        )

        // When
        viewModel.state.test {
            initialStateEmitted()
            advanceUntilIdle()

            viewModel.submit(ConversationDetailViewAction.ExpandOrCollapseAttachmentList(messageId))
            advanceUntilIdle()

            cancelAndIgnoreRemainingEvents()
        }

        // Then
        coVerify {
            messageViewStateCache.updateAttachmentsExpandCollapseMode(
                MessageId(messageId.id),
                expectedExpandCollapseMode
            )
        }
    }

    @Test
    fun `exit state is emitted when snooze successful`() = runTest {
        // given
        coEvery {
            reducer.newStateFrom(
                currentState = any(),
                operation = ConversationDetailEvent.ExitScreenWithMessage(
                    ConversationDetailViewAction.SnoozeCompleted(
                        "message"
                    )
                )
            )
        } returns ConversationDetailState.Loading.copy(
            exitScreenActionResult = Effect.of(
                ActionResult.DefinitiveActionResult(
                    TextUiModel(string.conversation_moved_to_selected_destination, "message")
                )
            )
        )

        // when
        viewModel.state.test {
            initialStateEmitted()
            viewModel.submit(ConversationDetailViewAction.SnoozeCompleted("message"))

            // then
            assertNotNull(awaitItem().exitScreenActionResult.consume())
            cancelAndIgnoreRemainingEvents()
        }
    }


    @Test
    fun `verify request snooze bottom sheet correctly sets bottom sheet state`() = runTest {
        // Given
        val labelId = LabelIdSample.AllMail
        every { savedStateHandle.get<String>(ConversationDetailScreen.OpenedFromLocationKey) } returns labelId.id

        val expectedResult = ConversationDetailState.Loading.copy(
            bottomSheetState = BottomSheetState(
                contentState = SnoozeSheetState.Requested(
                    userId,
                    labelId,
                    listOf(SnoozeConversationId(conversationId.id))
                ),
                bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Show)
            )
        )

        viewModel.submit(ConversationDetailViewAction.RequestSnoozeBottomSheet)
        advanceUntilIdle()

        coEvery {
            reducer.newStateFrom(
                any(),
                ConversationDetailEvent.ConversationBottomSheetEvent(
                    SnoozeSheetState.SnoozeOptionsBottomSheetEvent.Ready(
                        userId,
                        labelId,
                        listOf(SnoozeConversationId(conversationId.id))
                    )
                )
            )
        } returns expectedResult

        // When
        viewModel.state.test {
            viewModel.submit(
                ConversationDetailViewAction.RequestSnoozeBottomSheet
            )
            awaitItem()

            // Then
            val lastItem = awaitItem()
            assertEquals(expectedResult, lastItem)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Suppress("LongMethod")
    private fun setupCollapsedToExpandMessagesState(
        withUnreadMessage: Boolean = false
    ): Pair<List<MessageIdUiModel>, ConversationDetailMessageUiModel> {
        val (invoiceUiMessage, invoiceMessage) = if (withUnreadMessage) {
            Pair(ConversationDetailMessageUiModelSample.UnreadInvoice, MessageSample.UnreadInvoice)
        } else {
            Pair(ConversationDetailMessageUiModelSample.InvoiceWithLabel, MessageSample.Invoice)
        }
        val allCollapsed = nonEmptyListOf(
            invoiceUiMessage,
            ConversationDetailMessageUiModelSample.AugWeatherForecast
        ).toImmutableList()
        val firstExpanded = nonEmptyListOf(
            InvoiceWithLabelExpanded,
            ConversationDetailMessageUiModelSample.AugWeatherForecast
        ).toImmutableList()
        val firstExpanding = nonEmptyListOf(
            if (withUnreadMessage) {
                ConversationDetailMessageUiModelSample.InvoiceWithLabelExpandingUnread
            } else {
                InvoiceWithLabelExpanding
            },
            ConversationDetailMessageUiModelSample.AugWeatherForecast
        ).toImmutableList()
        val labelId = LabelIdSample.AllMail
        every { savedStateHandle.get<String>(ConversationDetailScreen.OpenedFromLocationKey) } returns labelId.id
        coEvery {
            observeConversationMessages(
                UserIdSample.Primary,
                ConversationIdSample.WeatherForecast,
                labelId
            )
        } returns flowOf(
            ConversationMessages(
                nonEmptyListOf(invoiceMessage, MessageSample.AugWeatherForecast),
                invoiceMessage.messageId
            ).right()
        )

        // This is no bueno, the order of the mocks here is important
        coEvery {
            reducer.newStateFrom(
                currentState = any(),
                operation = any()
            )
        } returns ConversationDetailState.Loading.copy(
            messagesState = ConversationDetailsMessagesState.Data(allCollapsed)
        )
        coEvery {
            reducer.newStateFrom(
                currentState = any(),
                operation = ofType<ConversationDetailEvent.ExpandDecryptedMessage>()
            )
        } returns ConversationDetailState.Loading.copy(
            messagesState = ConversationDetailsMessagesState.Data(firstExpanded)
        )
        coEvery {
            reducer.newStateFrom(
                currentState = any(),
                operation = ofType<ConversationDetailEvent.ErrorExpandingDecryptMessageError>()
            )
        } returns ConversationDetailState.Loading.copy(
            messagesState = ConversationDetailsMessagesState.Data(allCollapsed),
            error = Effect.of(TextUiModel(string.decryption_error))
        )
        coEvery {
            reducer.newStateFrom(
                currentState = any(),
                operation = ofType<ConversationDetailEvent.ErrorExpandingRetrieveMessageError>()
            )
        } returns ConversationDetailState.Loading.copy(
            messagesState = ConversationDetailsMessagesState.Data(allCollapsed),
            error = Effect.of(TextUiModel(string.detail_error_retrieving_message_body))
        )
        coEvery {
            reducer.newStateFrom(
                currentState = any(),
                operation = ofType<ConversationDetailEvent.ErrorExpandingRetrievingMessageOffline>()
            )
        } returns ConversationDetailState.Loading.copy(
            messagesState = ConversationDetailsMessagesState.Data(allCollapsed),
            error = Effect.of(TextUiModel(string.error_offline_loading_message))
        )
        coEvery {
            reducer.newStateFrom(
                currentState = any(),
                operation = ofType<ConversationDetailViewAction.RequestScrollTo>()
            )
        } returns ConversationDetailState.Loading.copy(
            scrollToMessage = MessageIdUiModel(allCollapsed.first().messageId.id)
        )
        coEvery {
            reducer.newStateFrom(
                currentState = any(),
                operation = ofType<ConversationDetailEvent.ExpandingMessage>()
            )
        } returns ConversationDetailState.Loading.copy(
            messagesState = ConversationDetailsMessagesState.Data(firstExpanding)
        )

        coEvery { conversationMessageMapper.toUiModel(any(), any(), primaryUserAddress) } returns
            ConversationDetailMessageUiModelSample.InvoiceWithLabel
        coEvery {
            conversationMessageMapper.toUiModel(
                message = any(),
                avatarImageState = any(),
                primaryUserAddress = primaryUserAddress,
                decryptedMessageBody = any(),
                attachmentListExpandCollapseMode = null,
                rsvpEventState = null
            )
        } returns
            InvoiceWithLabelExpanded
        every { conversationMessageMapper.toUiModel(any<ConversationDetailMessageUiModel.Collapsed>()) } returns
            InvoiceWithLabelExpanding
        return Pair(allCollapsed.map { it.messageId }, InvoiceWithLabelExpanded)
    }

    @Test
    fun `given unsnoozed successfully then message body is refreshed`() = runTest {
        // given
        val labelId = LabelIdSample.AllMail
        every { savedStateHandle.get<String>(ConversationDetailScreen.OpenedFromLocationKey) } returns labelId.id
        coEvery {
            reducer.newStateFrom(
                currentState = any(),
                operation = ConversationDetailEvent.ExitScreenWithMessage(
                    ConversationDetailEvent.UnsnoozeCompleted
                )
            )
        } returns ConversationDetailState.Loading.copy(
            exitScreenActionResult = Effect.of(
                ActionResult.DefinitiveActionResult(
                    TextUiModel("unsnooze")
                )
            )
        )

        // when
        viewModel.state.test {
            initialStateEmitted()
            viewModel.submit(ConversationDetailViewAction.OnUnsnoozeConversationRequested)
            advanceUntilIdle()
            // then
            coVerify { snoozeRepository.unSnoozeConversation(userId, labelId, listOf(conversationId)) }

            advanceUntilIdle()

            assertNotNull(awaitItem().exitScreenActionResult.consume())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `error message is emitted when unsnooze fails`() = runTest {
        // given
        coEvery {
            snoozeRepository.unSnoozeConversation(any(), any(), any())
        } returns UnsnoozeError.Other().left()
        coEvery {
            reducer.newStateFrom(
                currentState = ConversationDetailState.Loading,
                operation = ConversationDetailEvent.ErrorUnsnoozing
            )
        } returns ConversationDetailState.Loading.copy(
            error = Effect.of(TextUiModel(string.snooze_sheet_error_unable_to_unsnooze))
        )

        // when
        viewModel.state.test {
            initialStateEmitted()
            viewModel.submit(ConversationDetailViewAction.OnUnsnoozeConversationRequested)

            // then
            assertEquals(TextUiModel(string.snooze_sheet_error_unable_to_unsnooze), awaitItem().error.consume())
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun setupLinkClickState(messageId: MessageIdUiModel, link: Uri) {
        coEvery {
            reducer.newStateFrom(
                currentState = any(),
                operation = any()
            )
        } returns ConversationDetailState.Loading.copy(
            openMessageBodyLinkEffect = Effect.of(MessageBodyLink(messageId, link))
        )
    }

    private fun givenReducerReturnsBottomActions() {
        val actionUiModels = listOf(
            ActionUiModelTestData.archive,
            ActionUiModelTestData.markUnread
        ).toImmutableList()
        coEvery {
            reducer.newStateFrom(
                currentState = any(),
                operation = ofType<ConversationDetailEvent.ConversationBottomBarEvent>()
            )
        } returns ConversationDetailState.Loading.copy(
            bottomBarState = BottomBarState.Data.Shown(actionUiModels)
        )
    }

    private fun givenReducerReturnsStarredUiModel() {
        coEvery {
            reducer.newStateFrom(
                currentState = any(),
                operation = ConversationDetailViewAction.Star
            )
        } returns ConversationDetailState.Loading.copy(
            conversationState = ConversationDetailMetadataState.Data(
                ConversationUiModelTestData.conversationUiModelStarred
            )
        )
    }

    private suspend fun ReceiveTurbine<ConversationDetailState>.conversationMessagesEmitted() {
        initialStateEmitted()
        assertIs<ConversationDetailsMessagesState.Data>(awaitItem().messagesState)
    }

    private suspend fun ReceiveTurbine<ConversationDetailState>.initialStateEmitted() {
        assertEquals(ConversationDetailState.Loading, awaitItem())
    }

    private suspend fun ReceiveTurbine<ConversationDetailState>.lastEmittedItem(): ConversationDetailState {
        val events = cancelAndConsumeRemainingEvents()
        return (events.last() as Event.Item).value
    }
}
