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
import app.cash.turbine.FlowTurbine
import app.cash.turbine.test
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.ConversationIdSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.model.BottomBarEvent
import ch.protonmail.android.mailcommon.presentation.sample.ActionUiModelSample
import ch.protonmail.android.mailconversation.domain.sample.ConversationSample
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.reducer.BottomBarReducer
import ch.protonmail.android.mailconversation.domain.usecase.ObserveConversation
import ch.protonmail.android.maildetail.domain.usecase.ObserveConversationDetailActions
import ch.protonmail.android.maildetail.presentation.mapper.ActionUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.ConversationDetailMetadataUiModelMapper
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMetadataState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailsMessagesState
import ch.protonmail.android.maildetail.presentation.model.MessageDetailState
import ch.protonmail.android.maildetail.presentation.reducer.ConversationDetailReducer
import ch.protonmail.android.maildetail.presentation.sample.ConversationDetailMetadataUiModelSample
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailScreen
import ch.protonmail.android.testdata.action.ActionUiModelTestData
import ch.protonmail.android.testdata.conversation.ConversationTestData
import ch.protonmail.android.testdata.conversation.ConversationUiModelTestData
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.test.kotlin.assertIs
import org.junit.Assert.assertThrows
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ConversationDetailViewModelTest {

    private val actionUiModelMapper = ActionUiModelMapper()
    private val conversationUiModelMapper: ConversationDetailMetadataUiModelMapper = mockk {
        every { toUiModel(ConversationSample.WeatherForecast) } returns
            ConversationDetailMetadataUiModelSample.WeatherForecast
    }
    private val observeConversation: ObserveConversation = mockk {
        every { this@mockk(UserIdSample.Primary, ConversationIdSample.WeatherForecast) } returns
            flowOf(ConversationSample.WeatherForecast.right())
    }
    private val observeConversationDetailActions = mockk<ObserveConversationDetailActions> {
        every { this@mockk(UserIdSample.Primary, ConversationIdSample.WeatherForecast) } returns flowOf(
            listOf(Action.Reply, Action.Archive, Action.MarkUnread).right()
        )
    }
    private val observePrimaryUserId: ObservePrimaryUserId = mockk {
        every { this@mockk() } returns flowOf(UserIdSample.Primary)
    }
    private val reducer: ConversationDetailReducer = mockk {
        every { newStateFrom(currentState = any(), operation = any()) } returns ConversationDetailState.Loading
    }
    private val savedStateHandle: SavedStateHandle = mockk {
        every { get<String>(ConversationDetailScreen.CONVERSATION_ID_KEY) } returns
            ConversationIdSample.WeatherForecast.id
    }

    private val viewModel by lazy {
        ConversationDetailViewModel(
            observePrimaryUserId = observePrimaryUserId,
            observeConversation = observeConversation,
            metadataUiModelMapper = conversationUiModelMapper,
            actionUiModelMapper = actionUiModelMapper,
            observeDetailActions = observeConversationDetailActions,
            reducer = reducer,
            savedStateHandle = savedStateHandle
        )
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
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
        every { savedStateHandle.get<String>(ConversationDetailScreen.CONVERSATION_ID_KEY) } returns null
        // When
        val thrown = assertFailsWith<IllegalStateException> { viewModel.state }
        // Then
        assertEquals("No Conversation id given", thrown.message)
    }

    @Test
    fun `does handle user not logged in`() = runTest {
        // given
        val expectedOperation = ConversationDetailEvent.NoPrimaryUser
        every { observePrimaryUserId() } returns flowOf(null)

        // when
        viewModel.state.test {

            // then
            advanceUntilIdle()
            verify { reducer.newStateFrom(ConversationDetailState.Loading, expectedOperation) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `does handle conversation data`() = runTest {
        // given
        val expectedOperation = ConversationDetailEvent.ConversationData(
            conversationUiModel = ConversationDetailMetadataUiModelSample.WeatherForecast
        )

        // when
        viewModel.state.test {

            // then
            advanceUntilIdle()
            verify { reducer.newStateFrom(ConversationDetailState.Loading, expectedOperation) }
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `does handle conversation error`() = runTest {
        // given
        val expectedOperation = ConversationDetailEvent.ErrorLoadingConversation
        every { observeConversation(UserIdSample.Primary, ConversationIdSample.WeatherForecast) } returns flowOf(
            DataError.Local.NoDataCached.left()
        )

        // when
        viewModel.state.test {

            // then
            advanceUntilIdle()
            verify { reducer.newStateFrom(ConversationDetailState.Loading, expectedOperation) }
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `bottomBar state is data when use case returns actions`() = runTest {
        // Given
        every {
            observeConversationDetailActions(UserIdSample.Primary, ConversationIdSample.WeatherForecast)
        } returns flowOf(listOf(Action.Reply, Action.Archive).right())

        // when
        viewModel.state.test {
            initialStateEmitted()
            conversationStateEmitted()
            // Then
            val actionUiModels = listOf(ActionUiModelTestData.reply, ActionUiModelTestData.archive)
            val expected = BottomBarState.Data(actionUiModels)
            assertEquals(expected, awaitItem().bottomBarState)
        }
    }

    @Test
    fun `bottomBar state is failed loading actions when use case returns error`() = runTest {
        // Given
        every {
            observeConversationDetailActions(UserIdSample.Primary, ConversationIdSample.WeatherForecast)
        } returns flowOf(DataError.Local.NoDataCached.left())

        // When
        viewModel.state.test {
            initialStateEmitted()
            conversationStateEmitted()
            // Then
            val expected = BottomBarState.Error.FailedLoadingActions
            assertEquals(expected, awaitItem().bottomBarState)
        }

    }

    private suspend fun FlowTurbine<ConversationDetailState>.initialStateEmitted() {
        assertEquals(ConversationDetailState.Loading, awaitItem())
    }

    private suspend fun FlowTurbine<ConversationDetailState>.conversationStateEmitted() {
        assertIs<ConversationDetailMetadataState.Data>(awaitItem().conversationState)
    }

    private fun givenNoLoggedInUser() {
        every { observePrimaryUserId.invoke() } returns flowOf(null)
    }
}
