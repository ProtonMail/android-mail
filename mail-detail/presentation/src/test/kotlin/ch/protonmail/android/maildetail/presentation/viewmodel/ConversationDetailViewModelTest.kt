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

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.Event
import app.cash.turbine.FlowTurbine
import app.cash.turbine.test
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.NetworkError
import ch.protonmail.android.mailcommon.domain.sample.ConversationIdSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcontact.domain.model.GetContactError
import ch.protonmail.android.mailcontact.domain.usecase.ObserveContacts
import ch.protonmail.android.mailconversation.domain.sample.ConversationSample
import ch.protonmail.android.mailconversation.domain.usecase.ObserveConversation
import ch.protonmail.android.maildetail.domain.sample.MessageWithLabelsSample
import ch.protonmail.android.maildetail.domain.usecase.MoveConversation
import ch.protonmail.android.maildetail.domain.usecase.ObserveConversationDetailActions
import ch.protonmail.android.maildetail.domain.usecase.ObserveConversationMessagesWithLabels
import ch.protonmail.android.maildetail.domain.usecase.StarConversation
import ch.protonmail.android.maildetail.domain.usecase.UnStarConversation
import ch.protonmail.android.maildetail.presentation.R.string
import ch.protonmail.android.maildetail.presentation.mapper.ActionUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.ConversationDetailMessageUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.ConversationDetailMetadataUiModelMapper
import ch.protonmail.android.maildetail.presentation.model.BottomSheetState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMetadataState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailsMessagesState
import ch.protonmail.android.maildetail.presentation.reducer.ConversationDetailReducer
import ch.protonmail.android.maildetail.presentation.sample.ConversationDetailMessageUiModelSample
import ch.protonmail.android.maildetail.presentation.sample.ConversationDetailMetadataUiModelSample
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailScreen
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabels
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.usecase.ObserveExclusiveDestinationMailLabels
import ch.protonmail.android.mailsettings.domain.model.FolderColorSettings
import ch.protonmail.android.mailsettings.domain.usecase.ObserveFolderColorSettings
import ch.protonmail.android.testdata.action.ActionUiModelTestData
import ch.protonmail.android.testdata.conversation.ConversationTestData
import ch.protonmail.android.testdata.conversation.ConversationUiModelTestData
import ch.protonmail.android.testdata.maillabel.MailLabelTestData
import ch.protonmail.android.testdata.maillabel.MailLabelUiModelTestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
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
import kotlin.test.assertTrue

class ConversationDetailViewModelTest {

    private val conversationId = ConversationIdSample.WeatherForecast

    private val actionUiModelMapper = ActionUiModelMapper()
    private val conversationMetadataMapper: ConversationDetailMetadataUiModelMapper = mockk {
        every { toUiModel(ConversationSample.WeatherForecast) } returns
            ConversationDetailMetadataUiModelSample.WeatherForecast
    }
    private val conversationMessageMapper: ConversationDetailMessageUiModelMapper = mockk {
        every { toUiModel(messageWithLabels = MessageWithLabelsSample.AugWeatherForecast, contacts = any()) } returns
            ConversationDetailMessageUiModelSample.AugWeatherForecast
        every { toUiModel(messageWithLabels = MessageWithLabelsSample.SepWeatherForecast, contacts = any()) } returns
            ConversationDetailMessageUiModelSample.SepWeatherForecast
    }
    private val move: MoveConversation = mockk()
    private val observeContacts: ObserveContacts = mockk {
        every { this@mockk(userId = UserIdSample.Primary) } returns flowOf(emptyList<Contact>().right())
    }
    private val observeConversation: ObserveConversation = mockk {
        every { this@mockk(UserIdSample.Primary, ConversationIdSample.WeatherForecast) } returns
            flowOf(ConversationSample.WeatherForecast.right())
    }
    private val observeConversationMessagesWithLabels: ObserveConversationMessagesWithLabels = mockk {
        every { this@mockk(UserIdSample.Primary, ConversationIdSample.WeatherForecast) } returns flowOf(
            nonEmptyListOf(
                MessageWithLabelsSample.AugWeatherForecast,
                MessageWithLabelsSample.SepWeatherForecast
            ).right()
        )
    }
    private val observeConversationDetailActions = mockk<ObserveConversationDetailActions> {
        every { this@mockk(UserIdSample.Primary, ConversationIdSample.WeatherForecast) } returns flowOf(
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
            every { this@mockk.invoke(UserIdSample.Primary) } returns flowOf(FolderColorSettings())
        }
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

    private val viewModel by lazy {
        ConversationDetailViewModel(
            observePrimaryUserId = observePrimaryUserId,
            actionUiModelMapper = actionUiModelMapper,
            conversationMessageMapper = conversationMessageMapper,
            conversationMetadataMapper = conversationMetadataMapper,
            moveConversation = move,
            observeContacts = observeContacts,
            observeConversation = observeConversation,
            observeConversationMessages = observeConversationMessagesWithLabels,
            observeDetailActions = observeConversationDetailActions,
            observeDestinationMailLabels = observeMailLabels,
            observeFolderColor = observeFolderColorSettings,
            reducer = reducer,
            savedStateHandle = savedStateHandle,
            starConversation = starConversation,
            unStarConversation = unStarConversation
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
    fun `does handle conversation data`() = runTest {
        // given
        val initialState = ConversationDetailState.Loading
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
    fun `does handle conversation error`() = runTest {
        // given
        val initialState = ConversationDetailState.Loading
        val expectedState = initialState.copy(
            conversationState = ConversationDetailMetadataState.Error(
                message = TextUiModel(string.detail_error_loading_conversation)
            )
        )
        every { observeConversation(UserIdSample.Primary, ConversationIdSample.WeatherForecast) } returns
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
    fun `does handle conversation messages data`() = runTest {
        // given
        val initialState = ConversationDetailState.Loading
        val messagesUiModels = listOf(
            ConversationDetailMessageUiModelSample.AugWeatherForecast,
            ConversationDetailMessageUiModelSample.SepWeatherForecast
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

        // when
        viewModel.state.test {
            initialStateEmitted()

            // then
            assertEquals(expectedState, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `uses default when contacts error`() = runTest {
        // given
        val initialState = ConversationDetailState.Loading
        val messagesUiModels = listOf(
            ConversationDetailMessageUiModelSample.AugWeatherForecast,
            ConversationDetailMessageUiModelSample.SepWeatherForecast
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

        // when
        viewModel.state.test {
            initialStateEmitted()

            // then
            assertEquals(expectedState, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `does handle messages remote errors`() = runTest {
        // given
        val initialState = ConversationDetailState.Loading
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
    fun `does ignore messages local errors`() = runTest {
        // given
        val initialState = ConversationDetailState.Loading
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
    fun `bottomBar state is data when use case returns actions`() = runTest {
        // Given
        val initialState = ConversationDetailState.Loading
        val actions = listOf(Action.Reply, Action.Archive)
        val actionUiModels = listOf(ActionUiModelTestData.reply, ActionUiModelTestData.archive)
        val expected = initialState.copy(bottomBarState = BottomBarState.Data(actionUiModels))
        every {
            observeConversationDetailActions(UserIdSample.Primary, ConversationIdSample.WeatherForecast)
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
    fun `bottomBar state is failed loading actions when use case returns error`() = runTest {
        // Given
        val initialState = ConversationDetailState.Loading
        val expected = initialState.copy(bottomBarState = BottomBarState.Error.FailedLoadingActions)
        every {
            observeConversationDetailActions(UserIdSample.Primary, ConversationIdSample.WeatherForecast)
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

            val bottomSheetState = ConversationDetailState.Loading.copy(
                bottomSheetState = BottomSheetState.Data(MailLabelUiModelTestData.spamAndCustomFolder)
            )
            assertEquals(bottomSheetState, awaitItem())
            viewModel.submit(ConversationDetailViewAction.Star)
            val actual = assertIs<ConversationDetailMetadataState.Data>(awaitItem().conversationState)
            assertTrue(actual.conversationUiModel.isStarred)
        }
    }

    @Test
    fun `error starring conversation is emitted when star action fails`() = runTest {
        // Given
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
        }
    }

    @Test
    fun `unStarred conversation metadata is emitted when unStar action is successful`() = runTest {
        // given
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
        }
    }

    @Test
    fun `error moving to trash is emitted when action fails`() = runTest {
        // Given
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
        coEvery {
            move(
                userId = UserIdSample.Primary,
                conversationId = conversationId,
                labelId = SystemLabelId.Spam.labelId
            )
        } returns ConversationSample.WeatherForecast.right()
        val selectedLabel = MailLabelUiModelTestData.spamAndCustomFolder.first()
        val initialState = ConversationDetailState.Loading.copy(
            bottomSheetState = BottomSheetState.Data(MailLabelUiModelTestData.spamAndCustomFolder)
        )

        coEvery {
            reducer.newStateFrom(
                ConversationDetailState.Loading,
                ConversationDetailViewAction.MoveToDestinationSelected(selectedLabel.id)
            )
        } returns initialState.copy(
            bottomSheetState = BottomSheetState.Data(MailLabelUiModelTestData.spamAndCustomFolderWithSpamSelected)
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
            // Then
            advanceUntilIdle()
            viewModel.submit(ConversationDetailViewAction.MoveToDestinationSelected(selectedLabel.id))
            advanceUntilIdle()
            viewModel.submit(ConversationDetailViewAction.MoveToDestinationConfirmed("selectedLabel"))
            advanceUntilIdle()
            assertNotNull(lastEmittedItem().exitScreenWithMessageEffect.consume())
        }
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
            bottomSheetState = BottomSheetState.Data(MailLabelUiModelTestData.spamAndCustomFolder)
        )
    }

    private suspend fun FlowTurbine<ConversationDetailState>.initialStateEmitted() {
        assertEquals(ConversationDetailState.Loading, awaitItem())
    }

    private fun FlowTurbine<ConversationDetailState>.lastEmittedItem(): ConversationDetailState {
        val events = cancelAndConsumeRemainingEvents()
        return (events.last() as Event.Item).value
    }
}
