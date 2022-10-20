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
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.model.ActionUiModel
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.reducer.BottomBarStateReducer
import ch.protonmail.android.mailconversation.domain.usecase.ObserveConversation
import ch.protonmail.android.maildetail.domain.ObserveConversationDetailActions
import ch.protonmail.android.maildetail.presentation.mapper.ActionUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.ConversationDetailUiModelMapper
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailState
import ch.protonmail.android.maildetail.presentation.model.ConversationState
import ch.protonmail.android.maildetail.presentation.reducer.ConversationStateReducer
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailScreen
import ch.protonmail.android.testdata.conversation.ConversationTestData
import ch.protonmail.android.testdata.conversation.ConversationUiModelTestData
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.presentation.R
import org.junit.Assert.assertThrows
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ConversationDetailViewModelTest {

    private val rawConversationId = ConversationTestData.RAW_CONVERSATION_ID
    private val conversationUiModelMapper = ConversationDetailUiModelMapper()
    private val actionUiModelMapper = ActionUiModelMapper()
    private val conversationStateReducer = ConversationStateReducer()
    private val bottomBarStateReducer = BottomBarStateReducer()

    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk.invoke() } returns flowOf(userId)
    }

    private val observeConversation = mockk<ObserveConversation> {
        every { this@mockk.invoke(userId, any()) } returns flowOf(ConversationTestData.conversation.right())
    }
    private val savedStateHandle = mockk<SavedStateHandle> {
        every { this@mockk.get<String>(ConversationDetailScreen.CONVERSATION_ID_KEY) } returns rawConversationId
    }
    private val observeConversationDetailActions = mockk<ObserveConversationDetailActions> {
        every { this@mockk.invoke(userId, ConversationId(rawConversationId)) } returns flowOf(
            listOf(Action.Reply, Action.Archive, Action.MarkUnread)
        )
    }

    private val viewModel by lazy {
        ConversationDetailViewModel(
            observePrimaryUserId = observePrimaryUserId,
            conversationStateReducer = conversationStateReducer,
            bottomBarStateReducer = bottomBarStateReducer,
            observeConversation = observeConversation,
            uiModelMapper = conversationUiModelMapper,
            actionUiModelMapper = actionUiModelMapper,
            observeDetailActions = observeConversationDetailActions,
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
    fun `state is not logged in when there is no primary user`() = runTest {
        // Given
        givenNoLoggedInUser()

        // When
        viewModel.state.test {
            initialStateEmitted()
            // Then
            assertEquals(ConversationState.Error.NotLoggedIn, awaitItem().conversationState)
        }
    }

    @Test
    fun `throws exception when conversation id parameter was not provided as input`() = runTest {
        // Given
        every { savedStateHandle.get<String>(ConversationDetailScreen.CONVERSATION_ID_KEY) } returns null
        // When
        val thrown = assertThrows(IllegalStateException::class.java) { viewModel.state }
        // Then
        assertEquals("No Conversation id given", thrown.message)
    }

    @Test
    fun `conversation state is conversation data when use case returns conversation`() = runTest {
        // Given
        val conversationId = ConversationId(rawConversationId)
        every { observeConversation(userId, conversationId) } returns flowOf(ConversationTestData.conversation.right())

        // When
        viewModel.state.test {
            initialStateEmitted()
            // Then
            assertEquals(
                ConversationState.Data(ConversationUiModelTestData.conversationUiModel),
                awaitItem().conversationState
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `conversation state is failed loading data when use case returns data error`() = runTest {
        // Given
        val conversationId = ConversationId(rawConversationId)
        every { observeConversation(userId, conversationId) } returns flowOf(DataError.Local.NoDataCached.left())

        // When
        viewModel.state.test {
            initialStateEmitted()
            // Then
            assertEquals(ConversationState.Error.FailedLoadingData, awaitItem().conversationState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `bottomBar state is data when use case returns actions`() = runTest {
        // Given
        every { observeConversationDetailActions.invoke(userId, ConversationId(rawConversationId)) } returns flowOf(
            listOf(Action.Reply, Action.Archive)
        )
        // When
        viewModel.state.test {
            initialStateEmitted()
            conversationStateEmitted()
            // Then
            val actionUiModels = listOf(
                ActionUiModel(Action.Reply, R.drawable.ic_proton_arrow_up_and_left),
                ActionUiModel(Action.Archive, R.drawable.ic_proton_archive_box)
            )
            val expected = BottomBarState.Data(actionUiModels)
            assertEquals(expected, awaitItem().bottomBarState)
        }
    }

    @Test
    fun `bottomBar state is failed loading actions when use case returns no actions`() = runTest {
        // Given
        every { observeConversationDetailActions.invoke(userId, ConversationId(rawConversationId)) } returns flowOf(
            emptyList()
        )
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
        assertIs<ConversationState.Data>(awaitItem().conversationState)
    }

    private fun givenNoLoggedInUser() {
        every { observePrimaryUserId.invoke() } returns flowOf(null)
    }
}
