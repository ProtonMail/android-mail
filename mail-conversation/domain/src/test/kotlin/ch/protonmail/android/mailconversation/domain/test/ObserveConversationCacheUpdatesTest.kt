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

package ch.protonmail.android.mailconversation.domain.test

import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import ch.protonmail.android.mailconversation.domain.usecase.ObserveConversationCacheUpdates
import ch.protonmail.android.testdata.conversation.ConversationTestData
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import org.junit.Test

class ObserveConversationCacheUpdatesTest {

    private val repository = mockk<ConversationRepository>()

    @Test
    fun `Should not emit when the repo emits local data`() = runTest {
        // Given
        every { repository.observeConversationCacheDataResult(UserIdTestData.userId, any()) } returns
            flowOf(DataResult.Success(ResponseSource.Local, ConversationTestData.conversation).right())

        // When
        val result = buildUseCase()(UserIdTestData.userId, ConversationTestData.conversation.conversationId)

        // Then
        result.test {
            expectNoEvents()
        }
    }

    @Test
    fun `Should not emit when the repo returns error`() = runTest {
        // Given
        every { repository.observeConversationCacheDataResult(UserIdTestData.userId, any()) } returns
            flowOf(DataError.Local.NoDataCached.left())

        // When
        val result = buildUseCase()(UserIdTestData.userId, ConversationTestData.conversation.conversationId)

        // Then
        result.test {
            expectNoEvents()
        }
    }

    @Test
    fun `Should emit when the repo emits remote data`() = runTest {
        // Given
        coEvery { repository.observeConversationCacheDataResult(UserIdTestData.userId, any()) } returns
            flowOf(DataResult.Success(ResponseSource.Remote, ConversationTestData.conversation).right())

        // When
        val result = buildUseCase()(UserIdTestData.userId, ConversationTestData.conversation.conversationId)

        // Then
        result.test {
            expectMostRecentItem()
        }
    }

    private fun buildUseCase() = ObserveConversationCacheUpdates(repository)
}
