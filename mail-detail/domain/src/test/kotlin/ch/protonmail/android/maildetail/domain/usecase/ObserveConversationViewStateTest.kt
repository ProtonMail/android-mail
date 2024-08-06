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

package ch.protonmail.android.maildetail.domain.usecase

import java.util.Random
import app.cash.turbine.test
import ch.protonmail.android.maildetail.domain.repository.InMemoryConversationStateRepository
import ch.protonmail.android.maildetail.domain.repository.InMemoryConversationStateRepository.MessageState.Collapsed
import ch.protonmail.android.mailmessage.domain.model.MessageId
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class ObserveConversationViewStateTest {

    private val repo = mockk<InMemoryConversationStateRepository>(relaxUnitFun = true)

    @Test
    fun `Should emit the current conversation state`() = runTest {
        // Given
        val useCase = buildUseCase()
        val conversationState = InMemoryConversationStateRepository.MessagesState(
            (0 until Random().nextInt(100)).associate {
                Pair(MessageId(it.toString()), Collapsed)
            },
            shouldHideMessagesBasedOnTrashFilter = true
        )
        every { repo.conversationState } returns flowOf(conversationState)

        // When
        useCase().test {
            // Then
            assertEquals(conversationState, awaitItem())

            awaitComplete()
        }
    }

    private fun buildUseCase() = ObserveConversationViewState(repo)
}
