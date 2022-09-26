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

package ch.protonmail.android.maildetail.presentation.message

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.FlowTurbine
import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.maildetail.presentation.message.mapper.MessageDetailUiModelMapper
import ch.protonmail.android.maildetail.presentation.message.model.MessageDetailState
import ch.protonmail.android.maildetail.presentation.message.model.MessageUiModel
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.usecase.ObserveMessage
import ch.protonmail.android.testdata.message.MessageTestData
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MessageDetailViewModelTest {

    private val rawMessageId = "detailMessageId"
    private val messageUiModelMapper = MessageDetailUiModelMapper()
    private val reducer = MessageDetailReducer()

    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk.invoke() } returns flowOf(userId)
    }

    private val observeMessage = mockk<ObserveMessage> {
        every { this@mockk.invoke(userId, any()) } returns flowOf(DataError.Local.NoDataCached.left())
    }
    private val savedStateHandle = mockk<SavedStateHandle> {
        every { this@mockk.get<String>(MessageDetailScreen.MESSAGE_ID_KEY) } returns rawMessageId
    }

    private val viewModel by lazy {
        MessageDetailViewModel(
            observePrimaryUserId = observePrimaryUserId,
            messageDetailReducer = reducer,
            observeMessage = observeMessage,
            uiModelMapper = messageUiModelMapper,
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
    fun `state is not logged in when there is no primary user`() = runTest {
        // Given
        givenNoLoggedInUser()

        // When
        viewModel.state.test {
            initialStateEmitted()
            // Then
            assertEquals(MessageDetailState.Error.NotLoggedIn, awaitItem())
        }
    }

    @Test
    fun `state is no message id param when message id parameter was not provided as input`() = runTest {
        // Given
        every { savedStateHandle.get<String>(MessageDetailScreen.MESSAGE_ID_KEY) } returns null

        // When
        viewModel.state.test {
            initialStateEmitted()
            // Then
            assertEquals(MessageDetailState.Error.NoMessageIdProvided, awaitItem())
        }
    }

    @Test
    fun `state is data when repository returns message metadata`() = runTest {
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
            val expected = MessageDetailState.Data(MessageUiModel(messageId, subject, isStarred))
            assertEquals(expected, awaitItem())
        }
    }

    private suspend fun FlowTurbine<MessageDetailState>.initialStateEmitted() {
        awaitItem() as MessageDetailState.Loading
    }

    private fun givenNoLoggedInUser() {
        every { observePrimaryUserId.invoke() } returns flowOf(null)
    }

}
