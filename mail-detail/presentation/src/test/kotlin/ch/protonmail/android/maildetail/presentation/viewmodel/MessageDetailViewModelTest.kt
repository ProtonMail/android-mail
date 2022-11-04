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
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.reducer.BottomBarReducer
import ch.protonmail.android.maildetail.domain.usecase.ObserveMessageDetailActions
import ch.protonmail.android.maildetail.presentation.mapper.ActionUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.MessageDetailUiModelMapper
import ch.protonmail.android.maildetail.presentation.model.MessageDetailMetadataState
import ch.protonmail.android.maildetail.presentation.model.MessageDetailMetadataUiModel
import ch.protonmail.android.maildetail.presentation.model.MessageDetailState
import ch.protonmail.android.maildetail.presentation.reducer.MessageDetailMetadataReducer
import ch.protonmail.android.maildetail.presentation.ui.MessageDetailScreen
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.usecase.ObserveMessage
import ch.protonmail.android.testdata.action.ActionUiModelTestData
import ch.protonmail.android.testdata.message.MessageTestData
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertThrows
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MessageDetailViewModelTest {

    private val rawMessageId = "detailMessageId"
    private val actionUiModelMapper = ActionUiModelMapper()
    private val messageUiModelMapper = MessageDetailUiModelMapper()
    private val messageStateReducer = MessageDetailMetadataReducer()
    private val bottomBarReducer = BottomBarReducer()

    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk.invoke() } returns flowOf(userId)
    }

    private val observeMessage = mockk<ObserveMessage> {
        every { this@mockk.invoke(userId, any()) } returns flowOf(MessageTestData.message.right())
    }
    private val savedStateHandle = mockk<SavedStateHandle> {
        every { this@mockk.get<String>(MessageDetailScreen.MESSAGE_ID_KEY) } returns rawMessageId
    }
    private val observeDetailActions = mockk<ObserveMessageDetailActions> {
        every { this@mockk.invoke(userId, MessageId(rawMessageId)) } returns flowOf(
            nonEmptyListOf(Action.Reply, Action.Archive, Action.MarkUnread).right()
        )
    }

    private val viewModel by lazy {
        MessageDetailViewModel(
            observePrimaryUserId = observePrimaryUserId,
            messageStateReducer = messageStateReducer,
            bottomBarReducer = bottomBarReducer,
            observeMessage = observeMessage,
            uiModelMapper = messageUiModelMapper,
            actionUiModelMapper = actionUiModelMapper,
            observeDetailActions = observeDetailActions,
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
            assertEquals(MessageDetailState.Loading, awaitItem())
        }
    }

    @Test
    fun `message state is not logged in when there is no primary user`() = runTest {
        // Given
        givenNoLoggedInUser()

        // When
        viewModel.state.test {
            initialStateEmitted()
            // Then
            assertEquals(MessageDetailMetadataState.Error.NotLoggedIn, awaitItem().messageState)
        }
    }

    @Test
    fun `throws exception when message id parameter was not provided as input`() = runTest {
        // Given
        every { savedStateHandle.get<String>(MessageDetailScreen.MESSAGE_ID_KEY) } returns null

        // Then
        val thrown = assertThrows(IllegalStateException::class.java) { viewModel.state }
        // Then
        assertEquals("No Message id given", thrown.message)
    }

    @Test
    fun `message state is data when use case returns message metadata`() = runTest {
        // Given
        val messageId = MessageId(rawMessageId)
        val subject = "message subject"
        val isStarred = true
        val cachedMessage = MessageTestData.buildMessage(
            userId = userId,
            id = messageId.id,
            subject = subject,
            labelIds = listOf(SystemLabelId.Starred.labelId.id)
        )
        every { observeMessage.invoke(userId, messageId) } returns flowOf(cachedMessage.right())

        // When
        viewModel.state.test {
            initialStateEmitted()
            // Then
            val expected = MessageDetailMetadataState.Data(MessageDetailMetadataUiModel(messageId, subject, isStarred))
            assertEquals(expected, awaitItem().messageState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `bottomBar state is data when use case returns actions`() = runTest {
        // Given
        every { observeDetailActions.invoke(userId, MessageId(rawMessageId)) } returns flowOf(
            nonEmptyListOf(Action.Reply, Action.Archive).right()
        )

        // When
        viewModel.state.test {
            initialStateEmitted()
            messageStateEmitted()
            // Then
            val actionUiModels = listOf(ActionUiModelTestData.reply, ActionUiModelTestData.archive)
            val expected = BottomBarState.Data(actionUiModels)
            assertEquals(expected, awaitItem().bottomBarState)
        }
    }

    @Test
    fun `bottomBar state is failed loading actions when use case returns no actions`() = runTest {
        // Given
        every { observeDetailActions.invoke(userId, MessageId(rawMessageId)) } returns
            flowOf(DataError.Local.NoDataCached.left())

        // When
        viewModel.state.test {
            initialStateEmitted()
            messageStateEmitted()
            // Then
            val expected = BottomBarState.Error.FailedLoadingActions
            assertEquals(expected, awaitItem().bottomBarState)
        }
    }

    private suspend fun FlowTurbine<MessageDetailState>.messageStateEmitted() {
        assertIs<MessageDetailMetadataState.Data>(awaitItem().messageState)
    }

    private suspend fun FlowTurbine<MessageDetailState>.initialStateEmitted() {
        assertEquals(MessageDetailState.Loading, awaitItem())
    }

    private fun givenNoLoggedInUser() {
        every { observePrimaryUserId.invoke() } returns flowOf(null)
    }

}
