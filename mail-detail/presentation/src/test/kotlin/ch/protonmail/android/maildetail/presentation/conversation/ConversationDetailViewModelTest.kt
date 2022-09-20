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

package ch.protonmail.android.maildetail.presentation.conversation

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.FlowTurbine
import app.cash.turbine.test
import arrow.core.left
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import ch.protonmail.android.maildetail.presentation.conversation.mapper.ConversationDetailUiModelMapper
import ch.protonmail.android.maildetail.presentation.conversation.model.ConversationDetailState
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

class ConversationDetailViewModelTest {

    private val rawConversationId = "detailConversationId"
    private val conversationUiModelMapper = ConversationDetailUiModelMapper()
    private val reducer = ConversationDetailReducer()

    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk.invoke() } returns flowOf(userId)
    }

    private val conversationRepository = mockk<ConversationRepository> {
        every { this@mockk.observeConversation(userId, any()) } returns flowOf(DataError.Local.NoDataCached.left())
    }
    private val savedStateHandle = mockk<SavedStateHandle> {
        every { this@mockk.get<String>(ConversationDetailScreen.CONVERSATION_ID_KEY) } returns rawConversationId
    }

    private val viewModel by lazy {
        ConversationDetailViewModel(
            observePrimaryUserId = observePrimaryUserId,
            conversationDetailReducer = reducer,
            conversationRepository = conversationRepository,
            uiModelMapper = conversationUiModelMapper,
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
            awaitInitialState()
            // Then
            assertEquals(ConversationDetailState.Error.NotLoggedIn, awaitItem())
        }
    }

    @Test
    fun `throws exception when conversation id parameter was not provided as input`() = runTest {
        // Given
        every { savedStateHandle.get<String>(ConversationDetailScreen.CONVERSATION_ID_KEY) } returns null

        // When
        val thrown = assertThrows(IllegalStateException::class.java) { viewModel.state }
        assertEquals("No Conversation id given", thrown.message)
    }


    private suspend fun FlowTurbine<ConversationDetailState>.awaitInitialState() {
        awaitItem() as ConversationDetailState.Loading
    }

    private fun givenNoLoggedInUser() {
        every { observePrimaryUserId.invoke() } returns flowOf(null)
    }
}
