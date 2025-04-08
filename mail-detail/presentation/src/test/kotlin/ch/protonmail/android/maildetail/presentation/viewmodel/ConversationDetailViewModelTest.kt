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
import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.NetworkError
import ch.protonmail.android.mailcommon.domain.sample.ConversationIdSample
import ch.protonmail.android.mailcommon.domain.sample.DataErrorSample
import ch.protonmail.android.mailcommon.domain.sample.LabelSample
import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.R
import ch.protonmail.android.mailcommon.presentation.mapper.ActionUiModelMapper
import ch.protonmail.android.mailcommon.presentation.model.ActionResult
import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcontact.domain.model.GetContactError
import ch.protonmail.android.mailcontact.domain.usecase.FindContactByEmail
import ch.protonmail.android.mailcontact.domain.usecase.ObserveContacts
import ch.protonmail.android.mailconversation.domain.sample.ConversationSample
import ch.protonmail.android.mailconversation.domain.usecase.DeleteConversations
import ch.protonmail.android.mailconversation.domain.usecase.ObserveConversation
import ch.protonmail.android.mailconversation.domain.usecase.StarConversations
import ch.protonmail.android.mailconversation.domain.usecase.UnStarConversations
import ch.protonmail.android.maildetail.domain.usecase.DelayedMarkMessageAndConversationReadIfAllMessagesRead
import ch.protonmail.android.maildetail.domain.usecase.GetAttachmentIntentValues
import ch.protonmail.android.maildetail.domain.usecase.GetDetailBottomSheetActions
import ch.protonmail.android.maildetail.domain.usecase.GetDownloadingAttachmentsForMessages
import ch.protonmail.android.maildetail.domain.usecase.IsProtonCalendarInstalled
import ch.protonmail.android.maildetail.domain.usecase.MarkConversationAsUnread
import ch.protonmail.android.maildetail.domain.usecase.MarkMessageAsUnread
import ch.protonmail.android.maildetail.domain.usecase.MoveConversation
import ch.protonmail.android.maildetail.domain.usecase.MoveMessage
import ch.protonmail.android.maildetail.domain.usecase.MoveRemoteMessageAndLocalConversation
import ch.protonmail.android.maildetail.domain.usecase.ObserveConversationDetailActions
import ch.protonmail.android.maildetail.domain.usecase.ObserveConversationMessagesWithLabels
import ch.protonmail.android.maildetail.domain.usecase.ObserveConversationViewState
import ch.protonmail.android.maildetail.domain.usecase.ObserveMessageAttachmentStatus
import ch.protonmail.android.maildetail.domain.usecase.ObserveMessageWithLabels
import ch.protonmail.android.maildetail.domain.usecase.RelabelConversation
import ch.protonmail.android.maildetail.domain.usecase.ReportPhishingMessage
import ch.protonmail.android.maildetail.domain.usecase.SetMessageViewState
import ch.protonmail.android.maildetail.presentation.GetMessageIdToExpand
import ch.protonmail.android.maildetail.presentation.R.string
import ch.protonmail.android.maildetail.presentation.mapper.ConversationDetailMessageUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.ConversationDetailMetadataUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.MessageIdUiModelMapper
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMetadataState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailsMessagesState
import ch.protonmail.android.maildetail.presentation.model.MessageBodyLink
import ch.protonmail.android.maildetail.presentation.model.MessageIdUiModel
import ch.protonmail.android.maildetail.presentation.model.ParticipantUiModel
import ch.protonmail.android.maildetail.presentation.model.ReportPhishingDialogState
import ch.protonmail.android.maildetail.presentation.reducer.ConversationDetailReducer
import ch.protonmail.android.maildetail.presentation.sample.ConversationDetailMessageUiModelSample
import ch.protonmail.android.maildetail.presentation.sample.ConversationDetailMessageUiModelSample.InvoiceWithLabelExpanded
import ch.protonmail.android.maildetail.presentation.sample.ConversationDetailMessageUiModelSample.InvoiceWithLabelExpanding
import ch.protonmail.android.maildetail.presentation.sample.ConversationDetailMetadataUiModelSample
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailScreen
import ch.protonmail.android.maildetail.presentation.usecase.GetEmbeddedImageAvoidDuplicatedExecution
import ch.protonmail.android.maildetail.presentation.usecase.LoadDataForMessageLabelAsBottomSheet
import ch.protonmail.android.maildetail.presentation.usecase.OnMessageLabelAsConfirmed
import ch.protonmail.android.maildetail.presentation.usecase.PrintMessage
import ch.protonmail.android.maildetail.presentation.usecase.ShouldMessageBeHidden
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabels
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.usecase.ObserveCustomMailLabels
import ch.protonmail.android.maillabel.domain.usecase.ObserveExclusiveDestinationMailLabels
import ch.protonmail.android.maillabel.domain.usecase.ObserveMailLabels
import ch.protonmail.android.maillabel.presentation.model.MailLabelText
import ch.protonmail.android.maillabel.presentation.sample.LabelUiModelWithSelectedStateSample
import ch.protonmail.android.mailmessage.domain.model.DecryptedMessageBody
import ch.protonmail.android.mailmessage.domain.model.GetDecryptedMessageBodyError
import ch.protonmail.android.mailmessage.domain.model.LabelSelectionList
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MimeType
import ch.protonmail.android.mailmessage.domain.model.Participant
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageSample
import ch.protonmail.android.mailmessage.domain.sample.MessageWithLabelsSample
import ch.protonmail.android.mailmessage.domain.usecase.GetDecryptedMessageBody
import ch.protonmail.android.mailmessage.domain.usecase.ObserveMessage
import ch.protonmail.android.mailmessage.domain.usecase.ResolveParticipantName
import ch.protonmail.android.mailmessage.domain.usecase.ResolveParticipantNameResult
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyExpandCollapseMode
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.BottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.BottomSheetVisibilityEffect
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.ContactActionsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.DetailMoreActionsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.DetailMoreActionsBottomSheetState.MessageDataUiModel
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetEntryPoint
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MoveToBottomSheetEntryPoint
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MoveToBottomSheetState
import ch.protonmail.android.mailsettings.domain.model.AutoDeleteSetting
import ch.protonmail.android.mailsettings.domain.model.FolderColorSettings
import ch.protonmail.android.mailsettings.domain.model.PrivacySettings
import ch.protonmail.android.mailsettings.domain.usecase.ObserveAutoDeleteSetting
import ch.protonmail.android.mailsettings.domain.usecase.ObserveCustomizeToolbarSpotlight
import ch.protonmail.android.mailsettings.domain.usecase.ObserveFolderColorSettings
import ch.protonmail.android.mailsettings.domain.usecase.UpdateCustomizeToolbarSpotlight
import ch.protonmail.android.mailsettings.domain.usecase.privacy.ObservePrivacySettings
import ch.protonmail.android.mailsettings.domain.usecase.privacy.UpdateLinkConfirmationSetting
import ch.protonmail.android.testdata.action.ActionUiModelTestData
import ch.protonmail.android.testdata.contact.ContactSample
import ch.protonmail.android.testdata.conversation.ConversationTestData
import ch.protonmail.android.testdata.conversation.ConversationUiModelTestData
import ch.protonmail.android.testdata.maillabel.MailLabelTestData
import ch.protonmail.android.testdata.maillabel.MailLabelUiModelTestData
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.contact.domain.entity.Contact
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.network.domain.NetworkManager
import me.proton.core.network.domain.NetworkStatus
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Suppress("LargeClass")
class ConversationDetailViewModelTest {

    private val userId = UserIdSample.Primary
    private val conversationId = ConversationIdSample.WeatherForecast
    private val initialState = ConversationDetailState.Loading
    private val defaultFolderColorSettings = FolderColorSettings()
    private val defaultAutoDeleteSetting = AutoDeleteSetting.Disabled

    private val actionUiModelMapper = ActionUiModelMapper()
    private val messageIdUiModelMapper = MessageIdUiModelMapper()
    private val conversationMetadataMapper: ConversationDetailMetadataUiModelMapper = mockk {
        every { toUiModel(ConversationSample.WeatherForecast) } returns
            ConversationDetailMetadataUiModelSample.WeatherForecast
    }
    private val conversationMessageMapper: ConversationDetailMessageUiModelMapper = mockk {
        coEvery {
            toUiModel(
                messageWithLabels = MessageWithLabelsSample.InvoiceWithLabel,
                contacts = any(),
                folderColorSettings = defaultFolderColorSettings,
                autoDeleteSetting = defaultAutoDeleteSetting
            )
        } returns
            ConversationDetailMessageUiModelSample.InvoiceWithLabel
        coEvery {
            toUiModel(
                messageWithLabels = MessageWithLabelsSample.InvoiceWithTwoLabels,
                contacts = any(),
                folderColorSettings = defaultFolderColorSettings,
                autoDeleteSetting = defaultAutoDeleteSetting
            )
        } returns
            ConversationDetailMessageUiModelSample.InvoiceWithTwoLabels
        coEvery {
            toUiModel(
                messageWithLabels = MessageWithLabelsSample.InvoiceWithoutLabels,
                contacts = any(),
                folderColorSettings = defaultFolderColorSettings,
                autoDeleteSetting = defaultAutoDeleteSetting
            )
        } returns
            ConversationDetailMessageUiModelSample.InvoiceWithoutLabels
        coEvery {
            toUiModel(
                messageWithLabels = MessageWithLabelsSample.AnotherInvoiceWithoutLabels,
                contacts = any(),
                folderColorSettings = defaultFolderColorSettings,
                autoDeleteSetting = defaultAutoDeleteSetting
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
    private val markConversationAsUnread: MarkConversationAsUnread = mockk()
    private val move: MoveConversation = mockk()
    private val relabelConversation: RelabelConversation = mockk()
    private val deleteConversations: DeleteConversations = mockk()
    private val observeContacts: ObserveContacts = mockk {
        every { this@mockk(userId = UserIdSample.Primary) } returns flowOf(emptyList<Contact>().right())
    }
    private val observeConversation: ObserveConversation = mockk {
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
        every {
            this@mockk(UserIdSample.Primary, ConversationIdSample.WeatherForecast, any())
        } returns flowOf(
            listOf(Action.Archive, Action.MarkUnread).right()
        )
    }
    private val observePrimaryUserId: ObservePrimaryUserId = mockk {
        every { this@mockk() } returns flowOf(UserIdSample.Primary)
    }
    private val observeDestinationMailLabels = mockk<ObserveExclusiveDestinationMailLabels> {
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
    private val observeAutoDeleteSetting = mockk<ObserveAutoDeleteSetting> {
        coEvery { this@mockk() } returns flowOf(defaultAutoDeleteSetting)
    }
    private val observeCustomMailLabels = mockk<ObserveCustomMailLabels> {
        every { this@mockk.invoke(UserIdSample.Primary) } returns flowOf(
            MailLabelTestData.listOfCustomLabels.right()
        )
    }
    private val observeAttachmentStatus = mockk<ObserveMessageAttachmentStatus>()
    private val getAttachmentIntentValues = mockk<GetAttachmentIntentValues>()
    private val getAttachmentDownloadStatus = mockk<GetDownloadingAttachmentsForMessages>()
    private val getEmbeddedImageAvoidDuplicatedExecution = mockk<GetEmbeddedImageAvoidDuplicatedExecution>()
    private val reducer: ConversationDetailReducer = mockk {
        every { newStateFrom(currentState = any(), operation = any()) } returns ConversationDetailState.Loading
    }
    private val savedStateHandle: SavedStateHandle = mockk {
        every { get<String>(ConversationDetailScreen.ConversationIdKey) } returns conversationId.id
        every { get<String>(ConversationDetailScreen.ScrollToMessageIdKey) } returns null
        every { get<String>(ConversationDetailScreen.FilterByLocationKey) } returns null
    }
    private val starConversations: StarConversations = mockk {
        coEvery { this@mockk.invoke(any(), any()) } returns listOf(ConversationTestData.starredConversation).right()
    }
    private val unStarConversations: UnStarConversations = mockk {
        coEvery {
            this@mockk.invoke(any(), any())
        } returns listOf(ConversationTestData.conversation).right()
    }
    private val getDecryptedMessageBody: GetDecryptedMessageBody = mockk {
        coEvery { this@mockk.invoke(any(), any()) } returns DecryptedMessageBody(
            MessageIdSample.build(), "", MimeType.Html, emptyList(), UserAddressSample.PrimaryAddress
        ).right()
    }
    private val observeMessageWithLabels = mockk<ObserveMessageWithLabels> {
        every { this@mockk.invoke(UserIdSample.Primary, any()) } returns mockk()
    }
    private val markMessageAndConversationReadIfAllRead: DelayedMarkMessageAndConversationReadIfAllMessagesRead =
        mockk {
            coEvery { this@mockk.invoke(any(), any(), any()) } returns Unit
        }

    private val findContactByEmail: FindContactByEmail = mockk<FindContactByEmail> {
        coEvery { this@mockk.invoke(any(), any()) } returns ContactSample.Stefano
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
    private val resolveParticipantsName = mockk<ResolveParticipantName> {
        coEvery { this@mockk(any(), any(), any()) } returns ResolveParticipantNameResult("Sender", isProton = false)
    }

    private val inMemoryConversationStateRepository = FakeInMemoryConversationStateRepository()
    private val setMessageViewState = SetMessageViewState(inMemoryConversationStateRepository)
    private val observeConversationViewState = ObserveConversationViewState(inMemoryConversationStateRepository)
    private val networkManager = mockk<NetworkManager>()
    private val reportPhishingMessage = mockk<ReportPhishingMessage>()
    private val isProtonCalendarInstalled = mockk<IsProtonCalendarInstalled>()
    private val printMessage = mockk<PrintMessage>()
    private val markMessageAsUnread = mockk<MarkMessageAsUnread>()
    private val getMessageIdToExpand = mockk<GetMessageIdToExpand> {
        coEvery { this@mockk.invoke(any(), any(), any()) } returns MessageIdSample.build()
    }
    private val loadDataForMessageLabelAsBottomSheet = mockk<LoadDataForMessageLabelAsBottomSheet>()
    private val onMessageLabelAsConfirmed = mockk<OnMessageLabelAsConfirmed>()
    private val moveMessage = mockk<MoveMessage>()
    private val shouldMessageBeHidden = mockk<ShouldMessageBeHidden> {
        every { this@mockk.invoke(any(), any(), any()) } returns false
    }

    private val moveRemoteMessageAndLocalConversation = mockk<MoveRemoteMessageAndLocalConversation>()
    private val observeMailLabels = mockk<ObserveMailLabels>()

    private val observeCustomizeToolbarSpotlight = mockk<ObserveCustomizeToolbarSpotlight> {
        every { this@mockk.invoke() } returns flowOf()
    }
    private val updateCustomizeToolbarSpotlight = mockk<UpdateCustomizeToolbarSpotlight>()

    private val getBottomSheetActions = mockk<GetDetailBottomSheetActions> {
        every { this@mockk.invoke(any(), any(), any()) } returns listOf(Action.Archive)
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
            markConversationAsUnread = markConversationAsUnread,
            moveConversation = move,
            deleteConversations = deleteConversations,
            relabelConversation = relabelConversation,
            observeContacts = observeContacts,
            observeConversation = observeConversation,
            observeConversationMessages = observeConversationMessagesWithLabels,
            observeDetailActions = observeConversationDetailActions,
            observeDestinationMailLabels = observeDestinationMailLabels,
            observeFolderColor = observeFolderColorSettings,
            observeAutoDeleteSetting = observeAutoDeleteSetting,
            observeCustomMailLabels = observeCustomMailLabels,
            observeMessage = observeMessage,
            observeMessageAttachmentStatus = observeAttachmentStatus,
            getDownloadingAttachmentsForMessages = getAttachmentDownloadStatus,
            reducer = reducer,
            starConversations = starConversations,
            unStarConversations = unStarConversations,
            savedStateHandle = savedStateHandle,
            getDecryptedMessageBody = getDecryptedMessageBody,
            markMessageAndConversationReadIfAllMessagesRead = markMessageAndConversationReadIfAllRead,
            setMessageViewState = setMessageViewState,
            observeConversationViewState = observeConversationViewState,
            getAttachmentIntentValues = getAttachmentIntentValues,
            getEmbeddedImageAvoidDuplicatedExecution = getEmbeddedImageAvoidDuplicatedExecution,
            ioDispatcher = Dispatchers.Unconfined,
            observePrivacySettings = observePrivacySettings,
            updateLinkConfirmationSetting = updateLinkConfirmationSetting,
            resolveParticipantName = resolveParticipantsName,
            networkManager = networkManager,
            reportPhishingMessage = reportPhishingMessage,
            isProtonCalendarInstalled = isProtonCalendarInstalled,
            printMessage = printMessage,
            markMessageAsUnread = markMessageAsUnread,
            findContactByEmail = findContactByEmail,
            getMessageIdToExpand = getMessageIdToExpand,
            loadDataForMessageLabelAsBottomSheet = loadDataForMessageLabelAsBottomSheet,
            onMessageLabelAsConfirmed = onMessageLabelAsConfirmed,
            shouldMessageBeHidden = shouldMessageBeHidden,
            moveRemoteMessageAndLocalConversation = moveRemoteMessageAndLocalConversation,
            observeMailLabels = observeMailLabels,
            observeCustomizeToolbarSpotlight = observeCustomizeToolbarSpotlight,
            updateCustomizeToolbarSpotlight = updateCustomizeToolbarSpotlight,
            getBottomSheetActions = getBottomSheetActions
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
        // Given
        val messages = nonEmptyListOf(ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded)
        coEvery {
            conversationMessageMapper.toUiModel(
                messageWithLabels = any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns messages.first()

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
        val messages = nonEmptyListOf(ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded)
        coEvery {
            conversationMessageMapper.toUiModel(
                messageWithLabels = any(),
                contacts = any(),
                decryptedMessageBody = any(),
                folderColorSettings = defaultFolderColorSettings,
                autoDeleteSetting = defaultAutoDeleteSetting,
                userAddress = UserAddressSample.PrimaryAddress,
                effect = null
            )
        } returns messages.first()
        val conversationUiModel = ConversationDetailMetadataUiModelSample.WeatherForecast
        val expectedState = initialState.copy(
            conversationState = ConversationDetailMetadataState.Data(conversationUiModel)
        )
        every {
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
        val messages = nonEmptyListOf(ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded)
        coEvery {
            conversationMessageMapper.toUiModel(
                messageWithLabels = any(),
                contacts = any(),
                decryptedMessageBody = any(),
                folderColorSettings = defaultFolderColorSettings,
                autoDeleteSetting = defaultAutoDeleteSetting,
                userAddress = UserAddressSample.PrimaryAddress,
                effect = null
            )
        } returns messages.first()
        val expectedState = initialState.copy(
            conversationState = ConversationDetailMetadataState.Error(
                message = TextUiModel(string.detail_error_loading_conversation)
            )
        )
        every {
            observeConversation(UserIdSample.Primary, ConversationIdSample.WeatherForecast, any())
        } returns
            flowOf(DataError.Local.NoDataCached.left())
        every {
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
        val messages = nonEmptyListOf(ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded)
        coEvery {
            conversationMessageMapper.toUiModel(
                messageWithLabels = any(),
                contacts = any(),
                decryptedMessageBody = any(),
                folderColorSettings = defaultFolderColorSettings,
                autoDeleteSetting = defaultAutoDeleteSetting,
                userAddress = UserAddressSample.PrimaryAddress,
                effect = null
            )
        } returns messages.first()
        val dataState = initialState.copy(
            conversationState = ConversationDetailMetadataState.Data(
                ConversationDetailMetadataUiModelSample.WeatherForecast
            )
        )

        every {
            observeConversation(UserIdSample.Primary, ConversationIdSample.WeatherForecast, any())
        } returns flow {
            emit(ConversationSample.WeatherForecast.right())
            emit(DataError.Remote.Http(NetworkError.NoNetwork).left())
        }
        every {
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
        verify { reducer.newStateFrom(currentState = dataState, operation = ConversationDetailEvent.NoNetworkError) }
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
        every {
            reducer.newStateFrom(
                currentState = initialState,
                operation = ofType<ConversationDetailEvent.MessagesData>()
            )
        } returns expectedState
        coEvery {
            conversationMessageMapper.toUiModel(
                messageWithLabels = any(),
                contacts = any(),
                decryptedMessageBody = any(),
                folderColorSettings = defaultFolderColorSettings,
                autoDeleteSetting = defaultAutoDeleteSetting,
                userAddress = UserAddressSample.PrimaryAddress,
                effect = null
            )
        } returns
            InvoiceWithLabelExpanded

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
        every { observeContacts(UserIdSample.Primary) } returns flowOf(GetContactError.left())
        every {
            reducer.newStateFrom(
                currentState = initialState,
                operation = ofType<ConversationDetailEvent.MessagesData>()
            )
        } returns expectedState
        coEvery {
            conversationMessageMapper.toUiModel(
                messageWithLabels = any(), // Model here has 3 labels, sample models only have one or two
                contacts = emptyList(),
                decryptedMessageBody = any(),
                folderColorSettings = any(),
                autoDeleteSetting = defaultAutoDeleteSetting,
                userAddress = UserAddressSample.PrimaryAddress,
                effect = null
            )
        } returns
            InvoiceWithLabelExpanded

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
        every {
            observeConversationMessagesWithLabels(UserIdSample.Primary, ConversationIdSample.WeatherForecast)
        } returns flowOf(DataError.Remote.Http(NetworkError.NoNetwork).left())
        every {
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
    fun `conversation messages state does not change when use case fails with local error`() = runTest {
        // given
        every {
            observeConversationMessagesWithLabels(UserIdSample.Primary, ConversationIdSample.WeatherForecast)
        } returns flowOf(DataError.Local.NoDataCached.left())
        every {
            reducer.newStateFrom(
                currentState = initialState,
                operation = ConversationDetailEvent.ErrorLoadingMessages
            )
        } returns mockk()

        // when
        viewModel.state.test {
            initialStateEmitted()

            // then
            assertEquals(emptyList(), cancelAndConsumeRemainingEvents())
        }
    }

    @Test
    fun `bottom bar state is data when use case returns actions`() = runTest {
        // Given
        val messages = nonEmptyListOf(ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded)
        coEvery {
            conversationMessageMapper.toUiModel(
                messageWithLabels = any(),
                contacts = any(),
                decryptedMessageBody = any(),
                folderColorSettings = defaultFolderColorSettings,
                autoDeleteSetting = defaultAutoDeleteSetting,
                userAddress = UserAddressSample.PrimaryAddress,
                effect = null
            )
        } returns messages.first()
        val actions = listOf(Action.Archive)
        val actionUiModels = listOf(ActionUiModelTestData.archive).toImmutableList()
        val expected = initialState.copy(bottomBarState = BottomBarState.Data.Shown(actionUiModels))
        every {
            observeConversationDetailActions(UserIdSample.Primary, ConversationIdSample.WeatherForecast, any())
        } returns flowOf(actions.right())
        every {
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
        coEvery {
            conversationMessageMapper.toUiModel(
                messageWithLabels = any(),
                contacts = any(),
                decryptedMessageBody = any(),
                folderColorSettings = defaultFolderColorSettings,
                autoDeleteSetting = defaultAutoDeleteSetting,
                userAddress = UserAddressSample.PrimaryAddress,
                effect = null
            )
        } returns messages.first()
        val expected = initialState.copy(bottomBarState = BottomBarState.Error.FailedLoadingActions)
        every {
            observeConversationDetailActions(UserIdSample.Primary, ConversationIdSample.WeatherForecast, any())
        } returns flowOf(DataError.Local.NoDataCached.left())
        every {
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
    fun `starred conversation metadata is emitted when star action is successful`() = runTest {
        // given
        val messages = nonEmptyListOf(ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded)
        coEvery {
            conversationMessageMapper.toUiModel(
                messageWithLabels = any(),
                contacts = any(),
                decryptedMessageBody = any(),
                folderColorSettings = defaultFolderColorSettings,
                autoDeleteSetting = defaultAutoDeleteSetting,
                userAddress = UserAddressSample.PrimaryAddress,
                effect = null
            )
        } returns messages.first()
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
        val messages = nonEmptyListOf(ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded)
        coEvery {
            conversationMessageMapper.toUiModel(
                messageWithLabels = any(),
                contacts = any(),
                decryptedMessageBody = any(),
                folderColorSettings = defaultFolderColorSettings,
                autoDeleteSetting = defaultAutoDeleteSetting,
                userAddress = UserAddressSample.PrimaryAddress,
                effect = null
            )
        } returns messages.first()
        val actionUiModels = listOf(
            ActionUiModelTestData.archive,
            ActionUiModelTestData.markUnread
        ).toImmutableList()
        givenReducerReturnsStarredUiModel()
        givenReducerReturnsBottomActions()
        givenReducerReturnsBottomSheetActions()

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
        val messages = nonEmptyListOf(ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded)
        coEvery {
            conversationMessageMapper.toUiModel(
                messageWithLabels = any(),
                contacts = any(),
                decryptedMessageBody = any(),
                folderColorSettings = defaultFolderColorSettings,
                autoDeleteSetting = defaultAutoDeleteSetting,
                userAddress = UserAddressSample.PrimaryAddress,
                effect = null
            )
        } returns messages.first()
        coEvery { starConversations.invoke(UserIdSample.Primary, any()) } returns DataError.Local.NoDataCached.left()
        every {
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
            verify(exactly = 1) { reducer.newStateFrom(any(), ConversationDetailEvent.ErrorAddStar) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `unStarred conversation metadata is emitted when unStar action is successful`() = runTest {
        // given
        val messages = nonEmptyListOf(ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded)
        coEvery {
            conversationMessageMapper.toUiModel(
                messageWithLabels = any(),
                contacts = any(),
                decryptedMessageBody = any(),
                folderColorSettings = defaultFolderColorSettings,
                autoDeleteSetting = defaultAutoDeleteSetting,
                userAddress = UserAddressSample.PrimaryAddress,
                effect = null
            )
        } returns messages.first()
        every {
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
        val messages = nonEmptyListOf(ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded)
        coEvery {
            conversationMessageMapper.toUiModel(
                messageWithLabels = any(),
                contacts = any(),
                decryptedMessageBody = any(),
                folderColorSettings = defaultFolderColorSettings,
                autoDeleteSetting = defaultAutoDeleteSetting,
                userAddress = UserAddressSample.PrimaryAddress,
                effect = null
            )
        } returns messages.first()
        coEvery { unStarConversations.invoke(UserIdSample.Primary, any()) } returns DataError.Local.NoDataCached.left()
        every {
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
        val messages = nonEmptyListOf(ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded)
        coEvery {
            conversationMessageMapper.toUiModel(
                messageWithLabels = any(),
                contacts = any(),
                decryptedMessageBody = any(),
                folderColorSettings = defaultFolderColorSettings,
                autoDeleteSetting = defaultAutoDeleteSetting,
                userAddress = UserAddressSample.PrimaryAddress,
                effect = null
            )
        } returns messages.first()
        coEvery {
            move(
                UserIdSample.Primary,
                conversationId,
                SystemLabelId.Trash.labelId
            )
        } returns DataError.Local.NoDataCached.left()
        every {
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
            viewModel.submit(ConversationDetailViewAction.Trash)

            // Then
            assertEquals(TextUiModel(string.error_move_to_trash_failed), awaitItem().error.consume())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `exit with message is emitted when success moving to trash`() = runTest {
        // Given
        val messages = nonEmptyListOf(ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded)
        coEvery {
            conversationMessageMapper.toUiModel(
                messageWithLabels = any(),
                contacts = any(),
                decryptedMessageBody = any(),
                folderColorSettings = defaultFolderColorSettings,
                autoDeleteSetting = defaultAutoDeleteSetting,
                userAddress = UserAddressSample.PrimaryAddress,
                effect = null
            )
        } returns messages.first()
        coEvery {
            move(
                UserIdSample.Primary,
                conversationId,
                SystemLabelId.Trash.labelId
            )
        } returns Unit.right()
        every {
            reducer.newStateFrom(
                currentState = ConversationDetailState.Loading,
                operation = ConversationDetailViewAction.Trash
            )
        } returns ConversationDetailState.Loading.copy(
            exitScreenWithMessageEffect = Effect.of(
                ActionResult.UndoableActionResult(TextUiModel(string.conversation_moved_to_trash))
            )
        )

        // When
        viewModel.state.test {
            initialStateEmitted()
            viewModel.submit(ConversationDetailViewAction.Trash)

            // Then
            assertNotNull(awaitItem().exitScreenWithMessageEffect.consume())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `verify move to is called and exit with message is emitted when destination get confirmed`() = runTest {
        // Given
        val messages = nonEmptyListOf(ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded)
        coEvery {
            conversationMessageMapper.toUiModel(
                messageWithLabels = any(),
                contacts = any(),
                decryptedMessageBody = any(),
                folderColorSettings = defaultFolderColorSettings,
                autoDeleteSetting = defaultAutoDeleteSetting,
                userAddress = UserAddressSample.PrimaryAddress,
                effect = null
            )
        } returns messages.first()
        coEvery {
            move(
                userId = UserIdSample.Primary,
                conversationId = conversationId,
                labelId = SystemLabelId.Spam.labelId
            )
        } returns Unit.right()
        val selectedLabel = MailLabelUiModelTestData.spamAndCustomFolder.first()
        val dataState = ConversationDetailState.Loading.copy(
            bottomSheetState = BottomSheetState(
                MoveToBottomSheetState.Data(
                    moveToDestinations = MailLabelUiModelTestData.spamAndCustomFolder,
                    selected = null,
                    entryPoint = MoveToBottomSheetEntryPoint.Conversation
                )
            )
        )

        coEvery {
            reducer.newStateFrom(
                ConversationDetailState.Loading,
                ConversationDetailViewAction.MoveToDestinationSelected(selectedLabel.id)
            )
        } returns dataState.copy(
            bottomSheetState = BottomSheetState(
                MoveToBottomSheetState.Data(
                    MailLabelUiModelTestData.spamAndCustomFolderWithSpamSelected,
                    MailLabelUiModelTestData.spamAndCustomFolderWithSpamSelected.first(),
                    entryPoint = MoveToBottomSheetEntryPoint.Conversation
                )
            )
        )

        coEvery {
            reducer.newStateFrom(
                any(),
                ConversationDetailViewAction.MoveToDestinationConfirmed(
                    MailLabelText("selectedLabel"),
                    MoveToBottomSheetEntryPoint.Conversation
                )
            )
        } returns ConversationDetailState.Loading.copy(
            exitScreenWithMessageEffect = Effect.of(
                ActionResult.UndoableActionResult(
                    TextUiModel(string.conversation_moved_to_selected_destination, "selectedLabel")
                )
            )
        )

        // When
        viewModel.state.test {
            advanceUntilIdle()
            viewModel.submit(ConversationDetailViewAction.MoveToDestinationSelected(selectedLabel.id))
            advanceUntilIdle()
            viewModel.submit(
                ConversationDetailViewAction.MoveToDestinationConfirmed(
                    MailLabelText("selectedLabel"),
                    MoveToBottomSheetEntryPoint.Conversation
                )
            )
            advanceUntilIdle()

            // Then
            assertNotNull(lastEmittedItem().exitScreenWithMessageEffect.consume())
        }
    }

    @Test
    fun `verify label as action data is build according to the labels of messages`() = runTest {
        // Given
        val messages = nonEmptyListOf(ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded)
        coEvery {
            conversationMessageMapper.toUiModel(
                messageWithLabels = any(),
                contacts = any(),
                decryptedMessageBody = any(),
                folderColorSettings = defaultFolderColorSettings,
                autoDeleteSetting = defaultAutoDeleteSetting,
                userAddress = UserAddressSample.PrimaryAddress,
                effect = null
            )
        } returns messages.first()
        val event = LabelAsBottomSheetState.LabelAsBottomSheetEvent.ActionData(
            customLabelList = MailLabelUiModelTestData.customLabelList,
            selectedLabels = listOf(LabelSample.Document.labelId, LabelSample.Label2021.labelId).toImmutableList(),
            partiallySelectedLabels = listOf(LabelSample.Label2022.labelId).toImmutableList(),
            entryPoint = LabelAsBottomSheetEntryPoint.Conversation
        )

        val expectedResult = ConversationDetailState.Loading.copy(
            bottomSheetState = BottomSheetState(
                LabelAsBottomSheetState.Data(
                    LabelUiModelWithSelectedStateSample.customLabelListWithPartialSelection.toImmutableList(),
                    LabelAsBottomSheetEntryPoint.Conversation
                )
            )
        )

        coEvery { observeConversationMessagesWithLabels(userId, conversationId) } returns flowOf(
            nonEmptyListOf(
                MessageWithLabelsSample.InvoiceWithTwoLabels,
                MessageWithLabelsSample.InvoiceWithLabel
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
            viewModel.submit(ConversationDetailViewAction.RequestConversationLabelAsBottomSheet)
            // Request bottom Sheet call, we can ignore that
            awaitItem()

            // Then
            val lastItem = awaitItem()
            assertEquals(expectedResult, lastItem)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `verify relabel is called and exit is not called when labels get confirmed`() = runTest {
        // Given
        val messages = nonEmptyListOf(ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded)
        coEvery {
            conversationMessageMapper.toUiModel(
                messageWithLabels = any(),
                contacts = any(),
                decryptedMessageBody = any(),
                folderColorSettings = defaultFolderColorSettings,
                autoDeleteSetting = defaultAutoDeleteSetting,
                userAddress = UserAddressSample.PrimaryAddress,
                effect = null
            )
        } returns messages.first()
        val event = LabelAsBottomSheetState.LabelAsBottomSheetEvent.ActionData(
            customLabelList = MailLabelUiModelTestData.customLabelList,
            selectedLabels = listOf<LabelId>().toImmutableList(),
            partiallySelectedLabels = listOf<LabelId>().toImmutableList(),
            entryPoint = LabelAsBottomSheetEntryPoint.Conversation
        )

        val dataState = ConversationDetailState.Loading.copy(
            bottomSheetState = BottomSheetState(
                LabelAsBottomSheetState.Data(
                    LabelUiModelWithSelectedStateSample.customLabelListWithoutSelection,
                    LabelAsBottomSheetEntryPoint.Conversation
                )
            )
        )

        coEvery {
            relabelConversation(
                userId,
                conversationId,
                currentSelections = LabelSelectionList(
                    selectedLabels = emptyList(),
                    partiallySelectionLabels = emptyList()
                ),
                updatedSelections = LabelSelectionList(
                    selectedLabels = listOf(LabelSample.Document.labelId),
                    partiallySelectionLabels = emptyList()
                )
            )
        } returns ConversationSample.WeatherForecast.right()

        coEvery { observeConversationMessagesWithLabels(userId, conversationId) } returns flowOf(
            nonEmptyListOf(
                MessageWithLabelsSample.InvoiceWithoutLabels,
                MessageWithLabelsSample.AnotherInvoiceWithoutLabels
            ).right()
        )
        coEvery {
            reducer.newStateFrom(
                any(),
                ConversationDetailEvent.ConversationBottomSheetEvent(event)
            )
        } returns dataState

        coEvery {
            reducer.newStateFrom(
                ConversationDetailState.Loading,
                ConversationDetailViewAction.LabelAsToggleAction(LabelSample.Document.labelId)
            )
        } returns dataState.copy(
            bottomSheetState = BottomSheetState(
                LabelAsBottomSheetState.Data(
                    LabelUiModelWithSelectedStateSample.customLabelListWithDocumentSelected,
                    LabelAsBottomSheetEntryPoint.Conversation
                )
            )
        )

        // When
        viewModel.state.test {
            advanceUntilIdle()
            viewModel.submit(ConversationDetailViewAction.LabelAsToggleAction(LabelSample.Document.labelId))
            advanceUntilIdle()
            viewModel.submit(
                ConversationDetailViewAction.LabelAsConfirmed(
                    false,
                    LabelAsBottomSheetEntryPoint.Conversation
                )
            )
            advanceUntilIdle()

            // Then
            coVerify {
                relabelConversation(
                    userId,
                    conversationId,
                    currentSelections = LabelSelectionList(
                        selectedLabels = emptyList(),
                        partiallySelectionLabels = emptyList()
                    ),
                    updatedSelections = LabelSelectionList(
                        selectedLabels = listOf(LabelSample.Document.labelId),
                        partiallySelectionLabels = emptyList()
                    )
                )
            }
            verify { move wasNot Called }
            assertNull(lastEmittedItem().exitScreenWithMessageEffect.consume())
        }
    }

    @Test
    fun `verify relabel and move is called and exit is set when labels get confirmed and should be archived`() =
        runTest {
            // Given
            val messages = nonEmptyListOf(ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded)
            coEvery {
                conversationMessageMapper.toUiModel(
                    messageWithLabels = any(),
                    contacts = any(),
                    decryptedMessageBody = any(),
                    folderColorSettings = defaultFolderColorSettings,
                    autoDeleteSetting = defaultAutoDeleteSetting,
                    userAddress = UserAddressSample.PrimaryAddress,
                    effect = null
                )
            } returns messages.first()
            val event = LabelAsBottomSheetState.LabelAsBottomSheetEvent.ActionData(
                customLabelList = MailLabelUiModelTestData.customLabelList,
                selectedLabels = listOf<LabelId>().toImmutableList(),
                partiallySelectedLabels = listOf<LabelId>().toImmutableList(),
                entryPoint = LabelAsBottomSheetEntryPoint.Conversation
            )

            val dataState = ConversationDetailState.Loading.copy(
                bottomSheetState = BottomSheetState(
                    LabelAsBottomSheetState.Data(
                        LabelUiModelWithSelectedStateSample.customLabelListWithoutSelection,
                        LabelAsBottomSheetEntryPoint.Conversation
                    )
                )
            )

            coEvery {
                relabelConversation(
                    userId = userId,
                    conversationId = conversationId,
                    currentSelections = LabelSelectionList(
                        selectedLabels = emptyList(),
                        partiallySelectionLabels = emptyList()
                    ),
                    updatedSelections = LabelSelectionList(
                        selectedLabels = listOf(LabelSample.Document.labelId),
                        partiallySelectionLabels = emptyList()
                    )
                )
            } returns ConversationSample.WeatherForecast.right()

            coEvery {
                move(
                    userId = userId,
                    conversationId = conversationId,
                    labelId = SystemLabelId.Archive.labelId
                )
            } returns Unit.right()

            coEvery { observeConversationMessagesWithLabels(userId, conversationId) } returns flowOf(
                nonEmptyListOf(
                    MessageWithLabelsSample.InvoiceWithoutLabels,
                    MessageWithLabelsSample.AnotherInvoiceWithoutLabels
                ).right()
            )
            coEvery {
                reducer.newStateFrom(
                    any(),
                    ConversationDetailEvent.ConversationBottomSheetEvent(event)
                )
            } returns dataState

            coEvery {
                reducer.newStateFrom(
                    ConversationDetailState.Loading,
                    ConversationDetailViewAction.LabelAsToggleAction(LabelSample.Document.labelId)
                )
            } returns dataState.copy(
                bottomSheetState = BottomSheetState(
                    LabelAsBottomSheetState.Data(
                        LabelUiModelWithSelectedStateSample.customLabelListWithDocumentSelected,
                        LabelAsBottomSheetEntryPoint.Conversation
                    )
                )
            )

            coEvery {
                reducer.newStateFrom(
                    any(),
                    ConversationDetailViewAction.LabelAsConfirmed(true, LabelAsBottomSheetEntryPoint.Conversation)
                )
            } returns ConversationDetailState.Loading.copy(
                exitScreenWithMessageEffect = Effect.of(
                    ActionResult.UndoableActionResult(TextUiModel(string.conversation_moved_to_archive))
                )
            )

            // When
            viewModel.state.test {
                advanceUntilIdle()
                viewModel.submit(ConversationDetailViewAction.LabelAsToggleAction(LabelSample.Document.labelId))
                advanceUntilIdle()
                viewModel.submit(
                    ConversationDetailViewAction.LabelAsConfirmed(
                        true,
                        LabelAsBottomSheetEntryPoint.Conversation
                    )
                )
                advanceUntilIdle()

                // Then
                coVerify {
                    relabelConversation(
                        userId,
                        conversationId,
                        currentSelections = LabelSelectionList(
                            selectedLabels = emptyList(),
                            partiallySelectionLabels = emptyList()
                        ),
                        updatedSelections = LabelSelectionList(
                            selectedLabels = listOf(LabelSample.Document.labelId),
                            partiallySelectionLabels = emptyList()
                        )
                    )
                }
                coVerify { move(userId, conversationId, SystemLabelId.Archive.labelId) }
                assertNotNull(lastEmittedItem().exitScreenWithMessageEffect.consume())
            }
        }

    @Test
    fun `verify print is called when printing last message`() = runTest {
        // given
        val conversationUiModel = ConversationDetailMetadataUiModelSample.WeatherForecast

        val messages = listOf(
            ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded,
            ConversationDetailMessageUiModelSample.EmptyDraft
        )

        val expectedConvState = initialState.copy(
            messagesState = ConversationDetailsMessagesState.Data(messages.toImmutableList()),
            conversationState = ConversationDetailMetadataState.Data(conversationUiModel)
        )
        every {
            reducer.newStateFrom(
                currentState = initialState,
                operation = ofType<ConversationDetailEvent.MessagesData>()
            )
        } returns expectedConvState

        val expectedTargetMessage = messages[0] as ConversationDetailMessageUiModel.Expanded

        every {
            printMessage(
                context = any(),
                subject = any(),
                messageHeaderUiModel = expectedTargetMessage.messageDetailHeaderUiModel,
                messageBodyUiModel = expectedTargetMessage.messageBodyUiModel,
                messageBodyExpandCollapseMode = any(),
                loadEmbeddedImage = any()
            )
        } just runs

        // when
        viewModel.state.test {
            initialStateEmitted()

            // then
            assertEquals(expectedConvState, awaitItem())

            viewModel.submit(ConversationDetailViewAction.PrintLastMessage(mockk()))

            advanceUntilIdle()

            verify {
                printMessage(
                    context = any(),
                    subject = any(),
                    messageHeaderUiModel = expectedTargetMessage.messageDetailHeaderUiModel,
                    messageBodyUiModel = expectedTargetMessage.messageBodyUiModel,
                    messageBodyExpandCollapseMode = any(),
                    loadEmbeddedImage = any()
                )
            }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `verify print is called when last message is collapsed`() = runTest {
        // given
        val conversationUiModel = ConversationDetailMetadataUiModelSample.WeatherForecast

        val messages = listOf(
            ConversationDetailMessageUiModelSample.AugWeatherForecast,
            ConversationDetailMessageUiModelSample.EmptyDraft
        )
        val messagesExpanded = listOf(
            ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded,
            ConversationDetailMessageUiModelSample.EmptyDraft
        )

        val expectedConvState = initialState.copy(
            messagesState = ConversationDetailsMessagesState.Data(messages.toImmutableList()),
            conversationState = ConversationDetailMetadataState.Data(conversationUiModel)
        )
        val expectedExpandedConvState = expectedConvState.copy(
            messagesState = ConversationDetailsMessagesState.Data(messagesExpanded.toImmutableList())
        )
        every {
            reducer.newStateFrom(
                currentState = initialState,
                operation = ofType<ConversationDetailEvent.MessagesData>()
            )
        } returns expectedConvState

        every {
            reducer.newStateFrom(
                currentState = expectedConvState,
                operation = ofType<ConversationDetailViewAction.DismissBottomSheet>()
            )
        } returns expectedConvState

        every {
            reducer.newStateFrom(
                currentState = expectedConvState,
                operation = ofType<ConversationDetailEvent.ConversationBottomBarEvent>()
            )
        } returns expectedConvState

        every {
            reducer.newStateFrom(
                currentState = expectedExpandedConvState,
                operation = ofType<ConversationDetailEvent.ConversationData>()
            )
        } returns expectedExpandedConvState.copy(
            messagesState = ConversationDetailsMessagesState.Data(
                listOf(
                    ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded.copy(
                        messageBodyUiModel = ConversationDetailMessageUiModelSample
                            .AugWeatherForecastExpanded
                            .messageBodyUiModel
                            .copy(
                                printEffect = Effect.of(Unit)
                            )
                    ),
                    ConversationDetailMessageUiModelSample.EmptyDraft
                ).toImmutableList()
            )
        )

        every {
            reducer.newStateFrom(
                currentState = expectedConvState,
                operation = ofType<ConversationDetailEvent.MessagesData>()
            )
        } returns expectedExpandedConvState

        // when
        viewModel.state.test {
            initialStateEmitted()

            // then
            assertEquals(expectedConvState, awaitItem())

            viewModel.submit(ConversationDetailViewAction.PrintLastMessage(mockk()))

            advanceUntilIdle()

            assertEquals(expectedExpandedConvState, awaitItem())
            val actualState = awaitItem() // With print effect

            val actualFirstMessage = (actualState.messagesState as ConversationDetailsMessagesState.Data).messages
                .first() as ConversationDetailMessageUiModel.Expanded

            assertEquals(Effect.of(Unit), actualFirstMessage.messageBodyUiModel.printEffect)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `verify reply effect is set when replying and last message is collapsed`() = runTest {
        // given
        val conversationUiModel = ConversationDetailMetadataUiModelSample.WeatherForecast

        val messages = listOf(
            ConversationDetailMessageUiModelSample.AugWeatherForecast,
            ConversationDetailMessageUiModelSample.EmptyDraft
        )
        val messagesExpanded = listOf(
            ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded,
            ConversationDetailMessageUiModelSample.EmptyDraft
        )

        val expectedConvState = initialState.copy(
            messagesState = ConversationDetailsMessagesState.Data(messages.toImmutableList()),
            conversationState = ConversationDetailMetadataState.Data(conversationUiModel)
        )
        val expectedExpandedConvState = expectedConvState.copy(
            messagesState = ConversationDetailsMessagesState.Data(messagesExpanded.toImmutableList())
        )
        every {
            reducer.newStateFrom(
                currentState = initialState,
                operation = ofType<ConversationDetailEvent.MessagesData>()
            )
        } returns expectedConvState

        every {
            reducer.newStateFrom(
                currentState = expectedConvState,
                operation = ofType<ConversationDetailViewAction.DismissBottomSheet>()
            )
        } returns expectedConvState

        every {
            reducer.newStateFrom(
                currentState = expectedConvState,
                operation = ofType<ConversationDetailEvent.ConversationBottomBarEvent>()
            )
        } returns expectedConvState

        every {
            reducer.newStateFrom(
                currentState = expectedExpandedConvState,
                operation = ofType<ConversationDetailEvent.ConversationData>()
            )
        } returns expectedExpandedConvState.copy(
            messagesState = ConversationDetailsMessagesState.Data(
                listOf(
                    ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded.copy(
                        messageBodyUiModel = ConversationDetailMessageUiModelSample
                            .AugWeatherForecastExpanded
                            .messageBodyUiModel
                            .copy(
                                replyEffect = Effect.of(Unit)
                            )
                    ),
                    ConversationDetailMessageUiModelSample.EmptyDraft
                ).toImmutableList()
            )
        )

        every {
            reducer.newStateFrom(
                currentState = expectedConvState,
                operation = ofType<ConversationDetailEvent.MessagesData>()
            )
        } returns expectedExpandedConvState

        // when
        viewModel.state.test {
            initialStateEmitted()

            // then
            assertEquals(expectedConvState, awaitItem())

            viewModel.submit(ConversationDetailViewAction.ReplyToLastMessage(replyToAll = false))

            advanceUntilIdle()

            assertEquals(expectedExpandedConvState, awaitItem())
            val actualState = awaitItem() // With print effect

            val actualFirstMessage = (actualState.messagesState as ConversationDetailsMessagesState.Data).messages
                .first() as ConversationDetailMessageUiModel.Expanded

            assertEquals(Effect.of(Unit), actualFirstMessage.messageBodyUiModel.replyEffect)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `verify reply effect is triggered when replying to last message`() = runTest {
        // given
        val conversationUiModel = ConversationDetailMetadataUiModelSample.WeatherForecast

        val messages = listOf(
            ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded,
            ConversationDetailMessageUiModelSample.EmptyDraft
        )

        val expectedConvState = initialState.copy(
            messagesState = ConversationDetailsMessagesState.Data(messages.toImmutableList()),
            conversationState = ConversationDetailMetadataState.Data(conversationUiModel)
        )
        every {
            reducer.newStateFrom(
                currentState = initialState,
                operation = ofType<ConversationDetailEvent.MessagesData>()
            )
        } returns expectedConvState

        val expectedTargetMessage = messages[0] as ConversationDetailMessageUiModel.Expanded

        every {
            reducer.newStateFrom(
                currentState = expectedConvState,
                operation = ConversationDetailEvent.ReplyToMessageRequested(expectedTargetMessage.messageId)
            )
        } returns expectedConvState.copy(
            openReply = Effect.of(expectedTargetMessage.messageId)
        )

        // when
        viewModel.state.test {
            initialStateEmitted()

            // then
            assertEquals(expectedConvState, awaitItem())

            viewModel.submit(ConversationDetailViewAction.ReplyToLastMessage(replyToAll = false))
            advanceUntilIdle()

            assertEquals(
                initialState.copy(
                    messagesState = ConversationDetailsMessagesState.Data(messages.toImmutableList()),
                    conversationState = ConversationDetailMetadataState.Data(conversationUiModel),
                    openReply = Effect.of(expectedTargetMessage.messageId)
                ),
                awaitItem()
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `verify forward effect is triggered when forwarding last message`() = runTest {
        // given
        val conversationUiModel = ConversationDetailMetadataUiModelSample.WeatherForecast

        val messages = listOf(
            ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded,
            ConversationDetailMessageUiModelSample.EmptyDraft
        )

        val expectedConvState = initialState.copy(
            messagesState = ConversationDetailsMessagesState.Data(messages.toImmutableList()),
            conversationState = ConversationDetailMetadataState.Data(conversationUiModel)
        )
        every {
            reducer.newStateFrom(
                currentState = initialState,
                operation = ofType<ConversationDetailEvent.MessagesData>()
            )
        } returns expectedConvState

        val expectedTargetMessage = messages[0] as ConversationDetailMessageUiModel.Expanded

        every {
            reducer.newStateFrom(
                currentState = expectedConvState,
                operation = ConversationDetailEvent.ForwardMessageRequested(expectedTargetMessage.messageId)
            )
        } returns expectedConvState.copy(
            openForward = Effect.of(expectedTargetMessage.messageId)
        )

        // when
        viewModel.state.test {
            initialStateEmitted()

            // then
            assertEquals(expectedConvState, awaitItem())

            viewModel.submit(ConversationDetailViewAction.ForwardLastMessage)
            advanceUntilIdle()

            assertEquals(
                initialState.copy(
                    messagesState = ConversationDetailsMessagesState.Data(messages.toImmutableList()),
                    conversationState = ConversationDetailMetadataState.Data(conversationUiModel),
                    openForward = Effect.of(expectedTargetMessage.messageId)
                ),
                awaitItem()
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `verify report phishing effect is triggered when reporting last message`() = runTest {
        // given
        val conversationUiModel = ConversationDetailMetadataUiModelSample.WeatherForecast

        val messages = listOf(
            ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded,
            ConversationDetailMessageUiModelSample.EmptyDraft
        )

        val expectedConvState = initialState.copy(
            messagesState = ConversationDetailsMessagesState.Data(messages.toImmutableList()),
            conversationState = ConversationDetailMetadataState.Data(conversationUiModel)
        )
        every {
            reducer.newStateFrom(
                currentState = initialState,
                operation = ofType<ConversationDetailEvent.MessagesData>()
            )
        } returns expectedConvState

        every {
            reducer.newStateFrom(
                currentState = expectedConvState,
                operation = ofType<ConversationDetailEvent.ConversationBottomBarEvent>()
            )
        } returns expectedConvState

        every {
            reducer.newStateFrom(
                currentState = expectedConvState,
                operation = ofType<ConversationDetailEvent.ConversationData>()
            )
        } returns expectedConvState

        val expectedTargetMessage = messages[0] as ConversationDetailMessageUiModel.Expanded

        every { networkManager.networkStatus } returns NetworkStatus.Unmetered
        every {
            reducer.newStateFrom(
                currentState = expectedConvState,
                operation = ConversationDetailEvent.ReportPhishingRequested(
                    messageId = MessageId(expectedTargetMessage.messageId.id),
                    isOffline = false
                )
            )
        } returns expectedConvState.copy(
            reportPhishingDialogState = ReportPhishingDialogState.Shown.ShowConfirmation(
                MessageId(expectedTargetMessage.messageId.id)
            )
        )

        // when
        viewModel.state.test {
            initialStateEmitted()

            // then
            assertEquals(expectedConvState, awaitItem())

            viewModel.submit(ConversationDetailViewAction.ReportPhishingLastMessage)
            advanceUntilIdle()

            assertEquals(
                initialState.copy(
                    messagesState = ConversationDetailsMessagesState.Data(messages.toImmutableList()),
                    conversationState = ConversationDetailMetadataState.Data(conversationUiModel),
                    reportPhishingDialogState = ReportPhishingDialogState.Shown.ShowConfirmation(
                        MessageId(expectedTargetMessage.messageId.id)
                    )
                ),
                awaitItem()
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `verify move is called and exit is set when archived`() = runTest {
        // Given
        val messages = nonEmptyListOf(ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded)
        coEvery {
            conversationMessageMapper.toUiModel(
                messageWithLabels = any(),
                contacts = any(),
                decryptedMessageBody = any(),
                folderColorSettings = defaultFolderColorSettings,
                autoDeleteSetting = defaultAutoDeleteSetting,
                userAddress = UserAddressSample.PrimaryAddress,
                effect = null
            )
        } returns messages.first()

        coEvery {
            move(
                userId = userId,
                conversationId = conversationId,
                labelId = SystemLabelId.Archive.labelId
            )
        } returns Unit.right()

        coEvery { observeConversationMessagesWithLabels(userId, conversationId) } returns flowOf(
            nonEmptyListOf(
                MessageWithLabelsSample.InvoiceWithoutLabels,
                MessageWithLabelsSample.AnotherInvoiceWithoutLabels
            ).right()
        )

        coEvery {
            reducer.newStateFrom(
                any(),
                ConversationDetailViewAction.LabelAsConfirmed(true, LabelAsBottomSheetEntryPoint.Conversation)
            )
        } returns ConversationDetailState.Loading.copy(
            exitScreenWithMessageEffect = Effect.of(
                ActionResult.UndoableActionResult(TextUiModel(string.conversation_moved_to_archive))
            )
        )

        // When
        viewModel.state.test {
            advanceUntilIdle()
            viewModel.submit(ConversationDetailViewAction.Archive)
            advanceUntilIdle()

            // Then
            coVerify { move(userId, conversationId, SystemLabelId.Archive.labelId) }
            assertNotNull(lastEmittedItem().exitScreenWithMessageEffect.consume())
        }
    }

    @Test
    fun `verify exit is set when marked as spam`() = runTest {
        // given
        val conversationUiModel = ConversationDetailMetadataUiModelSample.WeatherForecast

        val messages = listOf(
            ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded,
            ConversationDetailMessageUiModelSample.EmptyDraft
        )

        val expectedConvState = initialState.copy(
            messagesState = ConversationDetailsMessagesState.Data(messages.toImmutableList()),
            conversationState = ConversationDetailMetadataState.Data(conversationUiModel)
        )
        every {
            reducer.newStateFrom(
                currentState = initialState,
                operation = ofType<ConversationDetailEvent.MessagesData>()
            )
        } returns expectedConvState

        coEvery {
            move(
                userId = userId,
                conversationId = conversationId,
                labelId = SystemLabelId.Spam.labelId
            )
        } returns Unit.right()

        coEvery {
            reducer.newStateFrom(
                any(),
                ConversationDetailEvent.MovedToSpam
            )
        } returns expectedConvState.copy(
            exitScreenWithMessageEffect = Effect.of(
                ActionResult.UndoableActionResult(TextUiModel.Text("Test"))
            )
        )

        // when
        viewModel.state.test {
            initialStateEmitted()

            assertEquals(expectedConvState, awaitItem())

            viewModel.submit(ConversationDetailViewAction.MoveToSpam)

            advanceUntilIdle()

            // then
            coVerify {
                move(
                    userId = userId,
                    conversationId = conversationId,
                    labelId = SystemLabelId.Spam.labelId
                )
            }
            assertNotNull(lastEmittedItem().exitScreenWithMessageEffect.consume())
        }
    }

    @Test
    fun `verify more actions bottomsheet opened when more clicked`() = runTest {
        // given
        val conversationUiModel = ConversationDetailMetadataUiModelSample.WeatherForecast

        val messages = listOf(
            ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded,
            ConversationDetailMessageUiModelSample.EmptyDraft
        )

        val expectedConvState = initialState.copy(
            messagesState = ConversationDetailsMessagesState.Data(messages.toImmutableList()),
            conversationState = ConversationDetailMetadataState.Data(conversationUiModel)
        )
        every {
            reducer.newStateFrom(
                currentState = initialState,
                operation = ofType<ConversationDetailEvent.MessagesData>()
            )
        } returns expectedConvState

        every {
            reducer.newStateFrom(
                currentState = expectedConvState,
                operation = ofType<ConversationDetailViewAction.RequestMoreActionsBottomSheet>()
            )
        } returns expectedConvState

        every {
            reducer.newStateFrom(
                currentState = expectedConvState,
                operation = ofType<ConversationDetailEvent.ConversationBottomBarEvent>()
            )
        } returns expectedConvState

        val messageDataMock = mockk<MessageDataUiModel>()

        every {
            reducer.newStateFrom(
                currentState = expectedConvState,
                match { operation ->
                    operation is ConversationDetailEvent.ConversationBottomSheetEvent &&
                        operation.bottomSheetOperation is
                        DetailMoreActionsBottomSheetState.MessageDetailMoreActionsBottomSheetEvent.DataLoaded &&
                        (
                            operation.bottomSheetOperation as
                                DetailMoreActionsBottomSheetState.MessageDetailMoreActionsBottomSheetEvent.DataLoaded
                            )
                            .affectingConversation &&
                        (
                            operation.bottomSheetOperation as
                                DetailMoreActionsBottomSheetState.MessageDetailMoreActionsBottomSheetEvent.DataLoaded
                            )
                            .messageId == "aug_weather_forecast"
                }
            )
        } returns expectedConvState.copy(
            bottomSheetState = BottomSheetState(
                contentState = DetailMoreActionsBottomSheetState.Data(
                    isAffectingConversation = true,
                    messageDataUiModel = messageDataMock,
                    replyActionsUiModel = persistentListOf()
                ),
                bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Show)
            )
        )

        val expectedTargetMessage = messages[0] as ConversationDetailMessageUiModel.Expanded
        coEvery { observeMessage(userId, MessageId(expectedTargetMessage.messageId.id)) } returns
            flowOf(MessageSample.AugWeatherForecast.right())

        // when
        viewModel.state.test {
            initialStateEmitted()

            // then
            assertEquals(expectedConvState, awaitItem())

            viewModel.submit(ConversationDetailViewAction.RequestConversationMoreActionsBottomSheet)

            advanceUntilIdle()

            val expectedEndState = expectedConvState.copy(
                bottomSheetState = BottomSheetState(
                    contentState = DetailMoreActionsBottomSheetState.Data(
                        isAffectingConversation = true,
                        messageDataUiModel = messageDataMock,
                        replyActionsUiModel = persistentListOf()
                    ),
                    bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Show)
                )
            )
            assertEquals(expectedEndState, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `verify relabel adds previously partially selected label`() = runTest {
        // Given
        val messages = nonEmptyListOf(ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded)
        coEvery {
            conversationMessageMapper.toUiModel(
                messageWithLabels = any(),
                contacts = any(),
                decryptedMessageBody = any(),
                folderColorSettings = defaultFolderColorSettings,
                autoDeleteSetting = defaultAutoDeleteSetting,
                userAddress = UserAddressSample.PrimaryAddress,
                effect = null
            )
        } returns messages.first()
        val event = LabelAsBottomSheetState.LabelAsBottomSheetEvent.ActionData(
            customLabelList = MailLabelUiModelTestData.customLabelList,
            selectedLabels = listOf(LabelSample.Document.labelId, LabelSample.Label2021.labelId).toImmutableList(),
            partiallySelectedLabels = listOf(LabelSample.Label2022.labelId).toImmutableList(),
            entryPoint = LabelAsBottomSheetEntryPoint.Conversation
        )

        val dataState = ConversationDetailState.Loading.copy(
            bottomSheetState = BottomSheetState(
                LabelAsBottomSheetState.Data(
                    LabelUiModelWithSelectedStateSample.customLabelListWithVariousStates,
                    LabelAsBottomSheetEntryPoint.Conversation
                )
            )
        )

        coEvery {
            relabelConversation(
                userId,
                conversationId,
                currentSelections = LabelSelectionList(
                    selectedLabels = listOf(LabelSample.Document.labelId, LabelSample.Label2021.labelId),
                    partiallySelectionLabels = listOf(LabelSample.Label2022.labelId)
                ),
                updatedSelections = LabelSelectionList(
                    selectedLabels = listOf(
                        LabelSample.Document.labelId,
                        LabelSample.Label2021.labelId,
                        LabelSample.Label2022.labelId
                    ),
                    partiallySelectionLabels = emptyList()
                )
            )
        } returns ConversationSample.WeatherForecast.right()

        coEvery { observeConversationMessagesWithLabels(userId, conversationId) } returns flowOf(
            nonEmptyListOf(
                MessageWithLabelsSample.InvoiceWithLabel,
                MessageWithLabelsSample.InvoiceWithTwoLabels
            ).right()
        )
        coEvery {
            reducer.newStateFrom(
                any(),
                ConversationDetailEvent.ConversationBottomSheetEvent(event)
            )
        } returns dataState

        coEvery {
            reducer.newStateFrom(
                ConversationDetailState.Loading,
                ConversationDetailViewAction.LabelAsToggleAction(LabelSample.Label2022.labelId)
            )
        } returns dataState.copy(
            bottomSheetState = BottomSheetState(
                LabelAsBottomSheetState.Data(
                    LabelUiModelWithSelectedStateSample.customLabelListAllSelected,
                    LabelAsBottomSheetEntryPoint.Conversation
                )
            )
        )

        // When
        viewModel.state.test {
            advanceUntilIdle()
            viewModel.submit(ConversationDetailViewAction.LabelAsToggleAction(LabelSample.Label2022.labelId))
            advanceUntilIdle()
            viewModel.submit(
                ConversationDetailViewAction.LabelAsConfirmed(
                    false,
                    LabelAsBottomSheetEntryPoint.Conversation
                )
            )
            advanceUntilIdle()

            // Then
            coVerify {
                relabelConversation(
                    userId,
                    conversationId,
                    currentSelections = LabelSelectionList(
                        selectedLabels = listOf(LabelSample.Document.labelId, LabelSample.Label2021.labelId),
                        partiallySelectionLabels = listOf(LabelSample.Label2022.labelId)
                    ),
                    updatedSelections = LabelSelectionList(
                        selectedLabels = listOf(
                            LabelSample.Document.labelId,
                            LabelSample.Label2021.labelId,
                            LabelSample.Label2022.labelId
                        ),
                        partiallySelectionLabels = emptyList()
                    )
                )
            }
            verify { move wasNot Called }
            assertNull(lastEmittedItem().exitScreenWithMessageEffect.consume())
        }
    }

    @Test
    fun `mark as unread is called correctly when action is submitted`() = runTest {
        // given
        val messages = nonEmptyListOf(ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded)
        coEvery {
            conversationMessageMapper.toUiModel(
                messageWithLabels = any(),
                contacts = any(),
                decryptedMessageBody = any(),
                folderColorSettings = defaultFolderColorSettings,
                autoDeleteSetting = defaultAutoDeleteSetting,
                userAddress = UserAddressSample.PrimaryAddress,
                effect = null
            )
        } returns messages.first()
        coEvery { markConversationAsUnread(userId, conversationId) } returns ConversationSample.WeatherForecast.right()

        // when
        viewModel.submit(ConversationDetailViewAction.MarkUnread)
        advanceUntilIdle()

        // then
        coVerify { markConversationAsUnread(userId, conversationId) }
    }

    @Test
    fun `exit state is emitted when marked as unread successfully`() = runTest {
        // given
        val messages = nonEmptyListOf(ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded)
        coEvery {
            conversationMessageMapper.toUiModel(
                messageWithLabels = any(),
                contacts = any(),
                decryptedMessageBody = any(),
                folderColorSettings = defaultFolderColorSettings,
                autoDeleteSetting = defaultAutoDeleteSetting,
                userAddress = UserAddressSample.PrimaryAddress,
                effect = null
            )
        } returns messages.first()
        coEvery { markConversationAsUnread(userId, conversationId) } returns ConversationSample.WeatherForecast.right()
        every {
            reducer.newStateFrom(
                currentState = ConversationDetailState.Loading,
                operation = ConversationDetailViewAction.MarkUnread
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
    fun `error message is emitted when mark as unread fails`() = runTest {
        // given
        val messages = nonEmptyListOf(ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded)
        coEvery {
            conversationMessageMapper.toUiModel(
                messageWithLabels = any(),
                contacts = any(),
                decryptedMessageBody = any(),
                folderColorSettings = defaultFolderColorSettings,
                autoDeleteSetting = defaultAutoDeleteSetting,
                userAddress = UserAddressSample.PrimaryAddress,
                effect = null
            )
        } returns messages.first()
        coEvery { markConversationAsUnread(userId, conversationId) } returns DataError.Local.NoDataCached.left()
        every {
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

        viewModel.state.test {
            conversationMessagesEmitted()

            // when
            viewModel.submit(ConversationDetailViewAction.ExpandMessage(messageIds.first()))
            advanceUntilIdle()

            // then
            coVerify {
                markMessageAndConversationReadIfAllRead(
                    userId, MessageId(expectedExpanded.messageId.id),
                    any()
                )
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should emit click effect when a link click is submitted`() = runTest {
        // given
        val messageId = MessageIdUiModel(MessageIdSample.AugWeatherForecast.id)
        val messages = nonEmptyListOf(ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded)
        coEvery {
            conversationMessageMapper.toUiModel(
                messageWithLabels = any(),
                contacts = any(),
                decryptedMessageBody = any(),
                folderColorSettings = defaultFolderColorSettings,
                autoDeleteSetting = defaultAutoDeleteSetting,
                userAddress = UserAddressSample.PrimaryAddress,
                effect = null
            )
        } returns messages.first()

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
        } returns GetDecryptedMessageBodyError.Decryption(MessageId(messageIds.first().id), "").left()

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
            } returns GetDecryptedMessageBodyError.Data(DataErrorSample.Unreachable).left()

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
            } returns GetDecryptedMessageBodyError.Data(DataErrorSample.Offline).left()

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
                messageWithLabels = any(),
                contacts = any(),
                decryptedMessageBody = any(),
                folderColorSettings = defaultFolderColorSettings,
                autoDeleteSetting = defaultAutoDeleteSetting,
                userAddress = UserAddressSample.PrimaryAddress,
                effect = null
            )
        } returns messages.first()
        every {
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
    fun `expand collapse mode of the automatically expanded message will be collapsed when body contains quote`() =
        runTest {
            // given
            val expectedUiModel = InvoiceWithLabelExpanded.copy(
                expandCollapseMode = MessageBodyExpandCollapseMode.Collapsed
            )
            val messages = nonEmptyListOf(
                InvoiceWithLabelExpanded
            ).toImmutableList()
            coEvery {
                conversationMessageMapper.toUiModel(
                    messageWithLabels = any(),
                    contacts = any(),
                    decryptedMessageBody = any(),
                    folderColorSettings = defaultFolderColorSettings,
                    autoDeleteSetting = defaultAutoDeleteSetting,
                    userAddress = UserAddressSample.PrimaryAddress,
                    effect = null
                )
            } returns expectedUiModel
            every {
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
                assertEquals(MessageBodyExpandCollapseMode.Collapsed, expandedMessage.expandCollapseMode)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `when user clicks on expand collapse button in the expanded message then mode will toggle`() = runTest {
        // given
        val expectedUiModel = InvoiceWithLabelExpanded.copy(
            expandCollapseMode = MessageBodyExpandCollapseMode.Collapsed
        )
        val messagesBodyCollapsed = nonEmptyListOf(
            InvoiceWithLabelExpanded.copy(
                expandCollapseMode = MessageBodyExpandCollapseMode.Collapsed
            )
        ).toImmutableList()
        val messagesBodyExpanded = nonEmptyListOf(
            InvoiceWithLabelExpanded.copy(
                expandCollapseMode = MessageBodyExpandCollapseMode.Expanded
            )
        ).toImmutableList()
        coEvery {
            conversationMessageMapper.toUiModel(
                messageWithLabels = any(),
                contacts = any(),
                decryptedMessageBody = any(),
                folderColorSettings = defaultFolderColorSettings,
                autoDeleteSetting = defaultAutoDeleteSetting,
                userAddress = UserAddressSample.PrimaryAddress,
                effect = null
            )
        } returns expectedUiModel
        every {
            reducer.newStateFrom(
                currentState = any(),
                operation = any()
            )
        } returns ConversationDetailState.Loading.copy(
            messagesState = ConversationDetailsMessagesState.Data(messagesBodyCollapsed)
        )
        every {
            reducer.newStateFrom(
                currentState = any(),
                operation = ofType<ConversationDetailViewAction.ExpandOrCollapseMessageBody>()
            )
        } returns ConversationDetailState.Loading.copy(
            messagesState = ConversationDetailsMessagesState.Data(messagesBodyExpanded)
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
            assertEquals(MessageBodyExpandCollapseMode.Collapsed, expandedMessage.expandCollapseMode)

            // when
            viewModel.submit(
                ConversationDetailViewAction.ExpandOrCollapseMessageBody(InvoiceWithLabelExpanded.messageId)
            )
            advanceUntilIdle()

            // Then
            val newStateForExpandBody = awaitItem().messagesState as ConversationDetailsMessagesState.Data
            val messageWithCollapsedBody =
                newStateForExpandBody
                    .messages
                    .first { it.messageId == InvoiceWithLabelExpanded.messageId }
            assertIs<ConversationDetailMessageUiModel.Expanded>(messageWithCollapsedBody)
            assertEquals(MessageBodyExpandCollapseMode.Expanded, messageWithCollapsedBody.expandCollapseMode)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Should observe bottom bar actions without refreshing the remote data`() = runTest {
        // Given
        val messages = nonEmptyListOf(ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded)
        coEvery {
            conversationMessageMapper.toUiModel(
                messageWithLabels = any(),
                contacts = any(),
                decryptedMessageBody = any(),
                folderColorSettings = defaultFolderColorSettings,
                autoDeleteSetting = defaultAutoDeleteSetting,
                userAddress = UserAddressSample.PrimaryAddress,
                effect = null
            )
        } returns messages.first()

        // When
        viewModel.state.test {
            advanceUntilIdle()

            // Then
            verify { observeConversationDetailActions(any(), any(), refreshConversations = false) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Should observe conversation metadata refreshing the remote data`() = runTest {
        // Given
        val messages = nonEmptyListOf(ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded)
        coEvery {
            conversationMessageMapper.toUiModel(
                messageWithLabels = any(),
                contacts = any(),
                decryptedMessageBody = any(),
                folderColorSettings = defaultFolderColorSettings,
                autoDeleteSetting = defaultAutoDeleteSetting,
                userAddress = UserAddressSample.PrimaryAddress,
                effect = null
            )
        } returns messages.first()

        // When
        viewModel.state.test {
            advanceUntilIdle()

            // Then
            verify { observeConversation(any(), any(), refreshData = true) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `verify contact actions bottom sheet data is build correctly`() = runTest {
        // Given
        val messages = nonEmptyListOf(ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded)
        coEvery {
            conversationMessageMapper.toUiModel(
                messageWithLabels = any(),
                contacts = any(),
                decryptedMessageBody = any(),
                folderColorSettings = defaultFolderColorSettings,
                autoDeleteSetting = defaultAutoDeleteSetting,
                userAddress = UserAddressSample.PrimaryAddress,
                effect = null
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
        val avatar = AvatarUiModel.ParticipantInitial("TU")

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
                    contactId = ContactSample.Stefano.id
                )
            )
        )

        coEvery { observeConversationMessagesWithLabels(userId, conversationId) } returns flowOf(
            nonEmptyListOf(
                MessageWithLabelsSample.InvoiceWithTwoLabels,
                MessageWithLabelsSample.InvoiceWithLabel
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

    @Suppress("LongMethod")
    private fun setupCollapsedToExpandMessagesState(
        withUnreadMessage: Boolean = false
    ): Pair<List<MessageIdUiModel>, ConversationDetailMessageUiModel> {
        val (invoiceUiMessage, invoiceMessage) = if (withUnreadMessage) {
            Pair(ConversationDetailMessageUiModelSample.UnreadInvoice, MessageWithLabelsSample.UnreadInvoice)
        } else {
            Pair(ConversationDetailMessageUiModelSample.InvoiceWithLabel, MessageWithLabelsSample.InvoiceWithLabel)
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
        every {
            observeConversationMessagesWithLabels(
                UserIdSample.Primary,
                ConversationIdSample.WeatherForecast
            )
        } returns flowOf(nonEmptyListOf(invoiceMessage, MessageWithLabelsSample.AugWeatherForecast).right())

        // This is no bueno, the order of the mocks here is important
        every {
            reducer.newStateFrom(
                currentState = any(),
                operation = any()
            )
        } returns ConversationDetailState.Loading.copy(
            messagesState = ConversationDetailsMessagesState.Data(allCollapsed)
        )
        every {
            reducer.newStateFrom(
                currentState = any(),
                operation = ofType<ConversationDetailEvent.ExpandDecryptedMessage>()
            )
        } returns ConversationDetailState.Loading.copy(
            messagesState = ConversationDetailsMessagesState.Data(firstExpanded)
        )
        every {
            reducer.newStateFrom(
                currentState = any(),
                operation = ofType<ConversationDetailEvent.ErrorExpandingDecryptMessageError>()
            )
        } returns ConversationDetailState.Loading.copy(
            messagesState = ConversationDetailsMessagesState.Data(allCollapsed),
            error = Effect.of(TextUiModel(string.decryption_error))
        )
        every {
            reducer.newStateFrom(
                currentState = any(),
                operation = ofType<ConversationDetailEvent.ErrorExpandingRetrieveMessageError>()
            )
        } returns ConversationDetailState.Loading.copy(
            messagesState = ConversationDetailsMessagesState.Data(allCollapsed),
            error = Effect.of(TextUiModel(string.detail_error_retrieving_message_body))
        )
        every {
            reducer.newStateFrom(
                currentState = any(),
                operation = ofType<ConversationDetailEvent.ErrorExpandingRetrievingMessageOffline>()
            )
        } returns ConversationDetailState.Loading.copy(
            messagesState = ConversationDetailsMessagesState.Data(allCollapsed),
            error = Effect.of(TextUiModel(string.error_offline_loading_message))
        )
        every {
            reducer.newStateFrom(
                currentState = any(),
                operation = ofType<ConversationDetailViewAction.RequestScrollTo>()
            )
        } returns ConversationDetailState.Loading.copy(
            scrollToMessage = MessageIdUiModel(allCollapsed.first().messageId.id)
        )
        every {
            reducer.newStateFrom(
                currentState = any(),
                operation = ofType<ConversationDetailEvent.ExpandingMessage>()
            )
        } returns ConversationDetailState.Loading.copy(
            messagesState = ConversationDetailsMessagesState.Data(firstExpanding)
        )

        coEvery { observeMessageWithLabels(userId, any()) } returns flowOf(invoiceMessage.right())
        coEvery {
            conversationMessageMapper.toUiModel(
                any(),
                any(),
                defaultFolderColorSettings,
                defaultAutoDeleteSetting
            )
        } returns
            ConversationDetailMessageUiModelSample.InvoiceWithLabel
        coEvery {
            conversationMessageMapper.toUiModel(
                messageWithLabels = any(),
                contacts = any(),
                decryptedMessageBody = any(),
                folderColorSettings = defaultFolderColorSettings,
                autoDeleteSetting = defaultAutoDeleteSetting,
                userAddress = UserAddressSample.PrimaryAddress,
                effect = null
            )
        } returns
            InvoiceWithLabelExpanded
        every { conversationMessageMapper.toUiModel(any<ConversationDetailMessageUiModel.Collapsed>()) } returns
            InvoiceWithLabelExpanding
        return Pair(allCollapsed.map { it.messageId }, InvoiceWithLabelExpanded)
    }

    private fun setupLinkClickState(messageId: MessageIdUiModel, link: Uri) {
        every {
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
        every {
            reducer.newStateFrom(
                currentState = any(),
                operation = ofType<ConversationDetailEvent.ConversationBottomBarEvent>()
            )
        } returns ConversationDetailState.Loading.copy(
            bottomBarState = BottomBarState.Data.Shown(actionUiModels)
        )
    }

    private fun givenReducerReturnsStarredUiModel() {
        every {
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

    private fun givenReducerReturnsBottomSheetActions() {
        every {
            reducer.newStateFrom(
                currentState = any(),
                operation = ofType<ConversationDetailEvent.ConversationBottomSheetEvent>()
            )
        } returns ConversationDetailState.Loading.copy(
            bottomSheetState = BottomSheetState(
                MoveToBottomSheetState.Data(
                    moveToDestinations = MailLabelUiModelTestData.spamAndCustomFolder,
                    selected = null,
                    entryPoint = MoveToBottomSheetEntryPoint.Conversation
                )
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
