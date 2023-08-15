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

import java.util.UUID
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
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcontact.domain.model.GetContactError
import ch.protonmail.android.mailcontact.domain.usecase.ObserveContacts
import ch.protonmail.android.mailconversation.domain.sample.ConversationSample
import ch.protonmail.android.mailconversation.domain.usecase.ObserveConversation
import ch.protonmail.android.mailmessage.domain.model.DecryptedMessageBody
import ch.protonmail.android.mailmessage.domain.model.GetDecryptedMessageBodyError
import ch.protonmail.android.maildetail.domain.model.LabelSelectionList
import ch.protonmail.android.maildetail.domain.sample.MessageWithLabelsSample
import ch.protonmail.android.maildetail.domain.usecase.GetAttachmentIntentValues
import ch.protonmail.android.mailmessage.domain.usecase.GetDecryptedMessageBody
import ch.protonmail.android.maildetail.domain.usecase.GetDownloadingAttachmentsForMessages
import ch.protonmail.android.maildetail.domain.usecase.MarkConversationAsUnread
import ch.protonmail.android.maildetail.domain.usecase.MarkMessageAndConversationReadIfAllMessagesRead
import ch.protonmail.android.maildetail.domain.usecase.MoveConversation
import ch.protonmail.android.maildetail.domain.usecase.ObserveConversationDetailActions
import ch.protonmail.android.maildetail.domain.usecase.ObserveConversationMessagesWithLabels
import ch.protonmail.android.maildetail.domain.usecase.ObserveConversationViewState
import ch.protonmail.android.maildetail.domain.usecase.ObserveMessageAttachmentStatus
import ch.protonmail.android.maildetail.domain.usecase.ObserveMessageWithLabels
import ch.protonmail.android.maildetail.domain.usecase.RelabelConversation
import ch.protonmail.android.maildetail.domain.usecase.SetMessageViewState
import ch.protonmail.android.maildetail.domain.usecase.StarConversation
import ch.protonmail.android.maildetail.domain.usecase.UnStarConversation
import ch.protonmail.android.maildetail.presentation.R.string
import ch.protonmail.android.maildetail.presentation.mapper.ActionUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.ConversationDetailMessageUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.ConversationDetailMetadataUiModelMapper
import ch.protonmail.android.maildetail.presentation.model.BottomSheetState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMetadataState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailsMessagesState
import ch.protonmail.android.maildetail.presentation.model.LabelAsBottomSheetState
import ch.protonmail.android.maildetail.presentation.model.MoveToBottomSheetState
import ch.protonmail.android.maildetail.presentation.reducer.ConversationDetailReducer
import ch.protonmail.android.maildetail.presentation.sample.ConversationDetailMessageUiModelSample
import ch.protonmail.android.maildetail.presentation.sample.ConversationDetailMessageUiModelSample.InvoiceWithLabelExpanded
import ch.protonmail.android.maildetail.presentation.sample.ConversationDetailMessageUiModelSample.InvoiceWithLabelExpanding
import ch.protonmail.android.maildetail.presentation.sample.ConversationDetailMetadataUiModelSample
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailScreen
import ch.protonmail.android.maildetail.presentation.usecase.GetEmbeddedImageAvoidDuplicatedExecution
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabels
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.usecase.ObserveCustomMailLabels
import ch.protonmail.android.maillabel.domain.usecase.ObserveExclusiveDestinationMailLabels
import ch.protonmail.android.maillabel.presentation.sample.LabelUiModelWithSelectedStateSample
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MimeType
import ch.protonmail.android.mailsettings.domain.model.FolderColorSettings
import ch.protonmail.android.mailsettings.domain.usecase.ObserveFolderColorSettings
import ch.protonmail.android.testdata.action.ActionUiModelTestData
import ch.protonmail.android.testdata.conversation.ConversationTestData
import ch.protonmail.android.testdata.conversation.ConversationUiModelTestData
import ch.protonmail.android.testdata.maillabel.MailLabelTestData
import ch.protonmail.android.testdata.maillabel.MailLabelUiModelTestData
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.contact.domain.entity.Contact
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

    private val actionUiModelMapper = ActionUiModelMapper()
    private val conversationMetadataMapper: ConversationDetailMetadataUiModelMapper = mockk {
        every { toUiModel(ConversationSample.WeatherForecast) } returns
            ConversationDetailMetadataUiModelSample.WeatherForecast
    }
    private val conversationMessageMapper: ConversationDetailMessageUiModelMapper = mockk {
        coEvery {
            toUiModel(
                messageWithLabels = MessageWithLabelsSample.InvoiceWithLabel,
                contacts = any(),
                folderColorSettings = defaultFolderColorSettings
            )
        } returns
            ConversationDetailMessageUiModelSample.InvoiceWithLabel
        coEvery {
            toUiModel(
                messageWithLabels = MessageWithLabelsSample.InvoiceWithTwoLabels,
                contacts = any(),
                folderColorSettings = defaultFolderColorSettings
            )
        } returns
            ConversationDetailMessageUiModelSample.InvoiceWithTwoLabels
        coEvery {
            toUiModel(
                messageWithLabels = MessageWithLabelsSample.InvoiceWithoutLabels,
                contacts = any(),
                folderColorSettings = defaultFolderColorSettings
            )
        } returns
            ConversationDetailMessageUiModelSample.InvoiceWithoutLabels
        coEvery {
            toUiModel(
                messageWithLabels = MessageWithLabelsSample.AnotherInvoiceWithoutLabels,
                contacts = any(),
                folderColorSettings = defaultFolderColorSettings
            )
        } returns
            ConversationDetailMessageUiModelSample.AnotherInvoiceWithoutLabels
        coEvery {
            toUiModel(
                ofType(ConversationDetailMessageUiModel.Collapsed::class),
            )
        } returns
            InvoiceWithLabelExpanding
    }
    private val markConversationAsUnread: MarkConversationAsUnread = mockk()
    private val move: MoveConversation = mockk()
    private val relabelConversation: RelabelConversation = mockk()
    private val observeContacts: ObserveContacts = mockk {
        every { this@mockk(userId = UserIdSample.Primary) } returns flowOf(emptyList<Contact>().right())
    }
    private val observeConversation: ObserveConversation = mockk {
        every { this@mockk(UserIdSample.Primary, ConversationIdSample.WeatherForecast, any()) } returns
            flowOf(ConversationSample.WeatherForecast.right())
    }
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
            listOf(Action.Reply, Action.Archive, Action.MarkUnread).right()
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
    }
    private val starConversation: StarConversation = mockk {
        coEvery { this@mockk.invoke(any(), any()) } returns ConversationTestData.starredConversation.right()
    }
    private val unStarConversation: UnStarConversation = mockk {
        coEvery { this@mockk.invoke(any(), any()) } returns ConversationTestData.conversation.right()
    }
    private val getDecryptedMessageBody: GetDecryptedMessageBody = mockk {
        coEvery { this@mockk.invoke(any(), any()) } returns DecryptedMessageBody("", MimeType.Html).right()
    }
    private val observeMessageWithLabels = mockk<ObserveMessageWithLabels> {
        every { this@mockk.invoke(UserIdSample.Primary, any()) } returns mockk()
    }
    private val markMessageAndConversationReadIfAllRead: MarkMessageAndConversationReadIfAllMessagesRead =
        mockk {
            coEvery { this@mockk.invoke(any(), any(), any()) } returns Unit.right()
        }

    private val inMemoryConversationStateRepository = FakeInMemoryConversationStateRepository()
    private val setMessageViewState = SetMessageViewState(inMemoryConversationStateRepository)
    private val observeConversationViewState = ObserveConversationViewState(inMemoryConversationStateRepository)

    private val viewModel by lazy {
        ConversationDetailViewModel(
            observePrimaryUserId = observePrimaryUserId,
            actionUiModelMapper = actionUiModelMapper,
            conversationMessageMapper = conversationMessageMapper,
            conversationMetadataMapper = conversationMetadataMapper,
            markConversationAsUnread = markConversationAsUnread,
            moveConversation = move,
            relabelConversation = relabelConversation,
            observeContacts = observeContacts,
            observeConversation = observeConversation,
            observeConversationMessages = observeConversationMessagesWithLabels,
            observeDetailActions = observeConversationDetailActions,
            observeDestinationMailLabels = observeMailLabels,
            observeFolderColor = observeFolderColorSettings,
            observeCustomMailLabels = observeCustomMailLabels,
            observeMessageAttachmentStatus = observeAttachmentStatus,
            getDownloadingAttachmentsForMessages = getAttachmentDownloadStatus,
            reducer = reducer,
            savedStateHandle = savedStateHandle,
            starConversation = starConversation,
            unStarConversation = unStarConversation,
            getDecryptedMessageBody = getDecryptedMessageBody,
            markMessageAndConversationReadIfAllMessagesRead = markMessageAndConversationReadIfAllRead,
            setMessageViewState = setMessageViewState,
            observeConversationViewState = observeConversationViewState,
            getAttachmentIntentValues = getAttachmentIntentValues,
            getEmbeddedImageAvoidDuplicatedExecution = getEmbeddedImageAvoidDuplicatedExecution,
            ioDispatcher = Dispatchers.Unconfined
        )
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
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
                folderColorSettings = defaultFolderColorSettings
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
                folderColorSettings = defaultFolderColorSettings
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
                folderColorSettings = defaultFolderColorSettings
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
        )
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
                folderColorSettings = defaultFolderColorSettings
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
        )
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
                folderColorSettings = any()
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
                folderColorSettings = defaultFolderColorSettings
            )
        } returns messages.first()
        val actions = listOf(Action.Reply, Action.Archive)
        val actionUiModels = listOf(ActionUiModelTestData.reply, ActionUiModelTestData.archive)
        val expected = initialState.copy(bottomBarState = BottomBarState.Data(actionUiModels))
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
                folderColorSettings = defaultFolderColorSettings
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
                folderColorSettings = defaultFolderColorSettings
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
                folderColorSettings = defaultFolderColorSettings
            )
        } returns messages.first()
        val actionUiModels = listOf(
            ActionUiModelTestData.reply,
            ActionUiModelTestData.archive,
            ActionUiModelTestData.markUnread
        )
        givenReducerReturnsStarredUiModel()
        givenReducerReturnsBottomActions()
        givenReducerReturnsBottomSheetActions()

        // When
        viewModel.state.test {
            initialStateEmitted()

            // Then
            val bottomBarState = ConversationDetailState.Loading.copy(
                bottomBarState = BottomBarState.Data(actionUiModels)
            )
            assertEquals(bottomBarState, awaitItem())

            viewModel.submit(ConversationDetailViewAction.Star)
            val actual = assertIs<ConversationDetailMetadataState.Data>(awaitItem().conversationState)
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
                folderColorSettings = defaultFolderColorSettings
            )
        } returns messages.first()
        coEvery { starConversation.invoke(UserIdSample.Primary, any()) } returns DataError.Local.NoDataCached.left()
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
                folderColorSettings = defaultFolderColorSettings
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
                folderColorSettings = defaultFolderColorSettings
            )
        } returns messages.first()
        coEvery { unStarConversation.invoke(UserIdSample.Primary, any()) } returns DataError.Local.NoDataCached.left()
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
                folderColorSettings = defaultFolderColorSettings
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
                folderColorSettings = defaultFolderColorSettings
            )
        } returns messages.first()
        coEvery {
            move(
                UserIdSample.Primary,
                conversationId,
                SystemLabelId.Trash.labelId
            )
        } returns ConversationSample.WeatherForecast.right()
        every {
            reducer.newStateFrom(
                currentState = ConversationDetailState.Loading,
                operation = ConversationDetailViewAction.Trash
            )
        } returns ConversationDetailState.Loading.copy(
            exitScreenWithMessageEffect = Effect.of(TextUiModel(string.conversation_moved_to_trash))
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
                folderColorSettings = defaultFolderColorSettings
            )
        } returns messages.first()
        coEvery {
            move(
                userId = UserIdSample.Primary,
                conversationId = conversationId,
                labelId = SystemLabelId.Spam.labelId
            )
        } returns ConversationSample.WeatherForecast.right()
        val selectedLabel = MailLabelUiModelTestData.spamAndCustomFolder.first()
        val dataState = ConversationDetailState.Loading.copy(
            bottomSheetState = BottomSheetState(
                MoveToBottomSheetState.Data(
                    moveToDestinations = MailLabelUiModelTestData.spamAndCustomFolder,
                    selected = null
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
                    MailLabelUiModelTestData.spamAndCustomFolderWithSpamSelected.first()
                )
            )
        )

        coEvery {
            reducer.newStateFrom(any(), ConversationDetailViewAction.MoveToDestinationConfirmed("selectedLabel"))
        } returns ConversationDetailState.Loading.copy(
            exitScreenWithMessageEffect = Effect.of(
                TextUiModel(
                    string.conversation_moved_to_selected_destination,
                    "selectedLabel"
                )
            )
        )

        // When
        viewModel.state.test {
            advanceUntilIdle()
            viewModel.submit(ConversationDetailViewAction.MoveToDestinationSelected(selectedLabel.id))
            advanceUntilIdle()
            viewModel.submit(ConversationDetailViewAction.MoveToDestinationConfirmed("selectedLabel"))
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
                folderColorSettings = defaultFolderColorSettings
            )
        } returns messages.first()
        val event = LabelAsBottomSheetState.LabelAsBottomSheetEvent.ActionData(
            customLabelList = MailLabelUiModelTestData.customLabelList,
            selectedLabels = listOf(LabelSample.Document.labelId, LabelSample.Label2021.labelId),
            partiallySelectedLabels = listOf(LabelSample.Label2022.labelId)
        )

        val expectedResult = ConversationDetailState.Loading.copy(
            bottomSheetState = BottomSheetState(
                LabelAsBottomSheetState.Data(
                    LabelUiModelWithSelectedStateSample.customLabelListWithPartialSelection
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
            viewModel.submit(ConversationDetailViewAction.RequestLabelAsBottomSheet)
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
                folderColorSettings = defaultFolderColorSettings
            )
        } returns messages.first()
        val event = LabelAsBottomSheetState.LabelAsBottomSheetEvent.ActionData(
            customLabelList = MailLabelUiModelTestData.customLabelList,
            selectedLabels = listOf(),
            partiallySelectedLabels = listOf()
        )

        val dataState = ConversationDetailState.Loading.copy(
            bottomSheetState = BottomSheetState(
                LabelAsBottomSheetState.Data(LabelUiModelWithSelectedStateSample.customLabelListWithoutSelection)
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
                LabelAsBottomSheetState.Data(LabelUiModelWithSelectedStateSample.customLabelListWithDocumentSelected)
            )
        )

        // When
        viewModel.state.test {
            advanceUntilIdle()
            viewModel.submit(ConversationDetailViewAction.LabelAsToggleAction(LabelSample.Document.labelId))
            advanceUntilIdle()
            viewModel.submit(ConversationDetailViewAction.LabelAsConfirmed(false))
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
                    folderColorSettings = defaultFolderColorSettings
                )
            } returns messages.first()
            val event = LabelAsBottomSheetState.LabelAsBottomSheetEvent.ActionData(
                customLabelList = MailLabelUiModelTestData.customLabelList,
                selectedLabels = listOf(),
                partiallySelectedLabels = listOf()
            )

            val dataState = ConversationDetailState.Loading.copy(
                bottomSheetState = BottomSheetState(
                    LabelAsBottomSheetState.Data(LabelUiModelWithSelectedStateSample.customLabelListWithoutSelection)
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
                        LabelUiModelWithSelectedStateSample.customLabelListWithDocumentSelected
                    )
                )
            )

            coEvery {
                reducer.newStateFrom(any(), ConversationDetailViewAction.LabelAsConfirmed(true))
            } returns ConversationDetailState.Loading.copy(
                exitScreenWithMessageEffect = Effect.of(TextUiModel(string.conversation_moved_to_archive))
            )

            // When
            viewModel.state.test {
                advanceUntilIdle()
                viewModel.submit(ConversationDetailViewAction.LabelAsToggleAction(LabelSample.Document.labelId))
                advanceUntilIdle()
                viewModel.submit(ConversationDetailViewAction.LabelAsConfirmed(true))
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
    fun `verify relabel adds previously partially selected label`() = runTest {
        // Given
        val messages = nonEmptyListOf(ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded)
        coEvery {
            conversationMessageMapper.toUiModel(
                messageWithLabels = any(),
                contacts = any(),
                decryptedMessageBody = any(),
                folderColorSettings = defaultFolderColorSettings
            )
        } returns messages.first()
        val event = LabelAsBottomSheetState.LabelAsBottomSheetEvent.ActionData(
            customLabelList = MailLabelUiModelTestData.customLabelList,
            selectedLabels = listOf(LabelSample.Document.labelId, LabelSample.Label2021.labelId),
            partiallySelectedLabels = listOf(LabelSample.Label2022.labelId)
        )

        val dataState = ConversationDetailState.Loading.copy(
            bottomSheetState = BottomSheetState(
                LabelAsBottomSheetState.Data(
                    LabelUiModelWithSelectedStateSample.customLabelListWithVariousStates
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
                LabelAsBottomSheetState.Data(LabelUiModelWithSelectedStateSample.customLabelListAllSelected)
            )
        )

        // When
        viewModel.state.test {
            advanceUntilIdle()
            viewModel.submit(ConversationDetailViewAction.LabelAsToggleAction(LabelSample.Label2022.labelId))
            advanceUntilIdle()
            viewModel.submit(ConversationDetailViewAction.LabelAsConfirmed(false))
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
                folderColorSettings = defaultFolderColorSettings
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
                folderColorSettings = defaultFolderColorSettings
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
                folderColorSettings = defaultFolderColorSettings
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
            coVerify { markMessageAndConversationReadIfAllRead(userId, expectedExpanded.messageId, any()) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should emit click effect when a link click is submitted`() = runTest {
        // given
        val messages = nonEmptyListOf(ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded)
        coEvery {
            conversationMessageMapper.toUiModel(
                messageWithLabels = any(),
                contacts = any(),
                decryptedMessageBody = any(),
                folderColorSettings = defaultFolderColorSettings
            )
        } returns messages.first()
        val link = "https://www.proton.me/${UUID.randomUUID()}"
        setupLinkClickState(link)

        viewModel.state.test {
            initialStateEmitted()
            // when
            viewModel.submit(ConversationDetailViewAction.MessageBodyLinkClicked(link))

            // then
            assertEquals(link, awaitItem().openMessageBodyLinkEffect.consume())
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
                messageIds.first()
            )
        } returns GetDecryptedMessageBodyError.Decryption("").left()

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
                    messageIds.first()
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
                    messageIds.first()
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
        )
        coEvery {
            conversationMessageMapper.toUiModel(
                messageWithLabels = any(),
                contacts = any(),
                decryptedMessageBody = any(),
                folderColorSettings = defaultFolderColorSettings
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
    fun `Should observe bottom bar actions without refreshing the remote data`() = runTest {
        // Given
        val messages = nonEmptyListOf(ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded)
        coEvery {
            conversationMessageMapper.toUiModel(
                messageWithLabels = any(),
                contacts = any(),
                decryptedMessageBody = any(),
                folderColorSettings = defaultFolderColorSettings
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
                folderColorSettings = defaultFolderColorSettings
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

    @Suppress("LongMethod")
    private fun setupCollapsedToExpandMessagesState(
        withUnreadMessage: Boolean = false
    ): Pair<List<MessageId>, ConversationDetailMessageUiModel> {
        val (invoiceUiMessage, invoiceMessage) = if (withUnreadMessage) {
            Pair(ConversationDetailMessageUiModelSample.UnreadInvoice, MessageWithLabelsSample.UnreadInvoice)
        } else {
            Pair(ConversationDetailMessageUiModelSample.InvoiceWithLabel, MessageWithLabelsSample.InvoiceWithLabel)
        }
        val allCollapsed = nonEmptyListOf(
            invoiceUiMessage,
            ConversationDetailMessageUiModelSample.AugWeatherForecast
        )
        val firstExpanded = nonEmptyListOf(
            InvoiceWithLabelExpanded,
            ConversationDetailMessageUiModelSample.AugWeatherForecast
        )
        val firstExpanding = nonEmptyListOf(
            if (withUnreadMessage) {
                ConversationDetailMessageUiModelSample.InvoiceWithLabelExpandingUnread
            } else {
                InvoiceWithLabelExpanding
            },
            ConversationDetailMessageUiModelSample.AugWeatherForecast
        )
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
            scrollToMessage = MessageId(allCollapsed.first().messageId.id)
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
        coEvery { conversationMessageMapper.toUiModel(any(), any(), defaultFolderColorSettings) } returns
            ConversationDetailMessageUiModelSample.InvoiceWithLabel
        coEvery {
            conversationMessageMapper.toUiModel(
                messageWithLabels = any(),
                contacts = any(),
                decryptedMessageBody = any(),
                folderColorSettings = defaultFolderColorSettings
            )
        } returns
            InvoiceWithLabelExpanded
        every { conversationMessageMapper.toUiModel(any()) } returns
            InvoiceWithLabelExpanding
        return Pair(allCollapsed.map { it.messageId }, InvoiceWithLabelExpanded)
    }

    private fun setupLinkClickState(link: String) {
        every {
            reducer.newStateFrom(
                currentState = any(),
                operation = any()
            )
        } returns ConversationDetailState.Loading.copy(
            openMessageBodyLinkEffect = Effect.of(link)
        )
    }

    private fun givenReducerReturnsBottomActions() {
        val actionUiModels = listOf(
            ActionUiModelTestData.reply,
            ActionUiModelTestData.archive,
            ActionUiModelTestData.markUnread
        )
        every {
            reducer.newStateFrom(
                currentState = any(),
                operation = ofType<ConversationDetailEvent.ConversationBottomBarEvent>()
            )
        } returns ConversationDetailState.Loading.copy(
            bottomBarState = BottomBarState.Data(actionUiModels)
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
                    selected = null
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
