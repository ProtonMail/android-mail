/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailmessage.domain.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.SearchMatchField
import ch.protonmail.android.mailmessage.domain.model.SearchMatchPosition
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailpagination.domain.model.PageKey
import ch.protonmail.android.mailpagination.domain.model.PaginationError
import ch.protonmail.android.testdata.message.MessageTestData.buildMessage
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetSearchMatchedTermsTest {

    private val messageId = MessageId("1")
    private val searchKey = PageKey.PageKeyForSearch(keyword = "governmnt")
    private val messageRepository = mockk<MessageRepository>()

    private val getSearchMatchedTerms = GetSearchMatchedTerms(messageRepository)

    @Test
    fun `should return the text the search matched in the message`() = runTest {
        // Given
        coEvery { messageRepository.getSearchMatchPositions(userId, searchKey, listOf(messageId)) } returns mapOf(
            messageId to listOf(SearchMatchPosition(SearchMatchField.Subject, valueIndex = 0, start = 4, end = 14))
        ).right()
        coEvery { messageRepository.getMessage(userId, messageId) } returns
            buildMessage(id = "1", subject = "New government policy").right()

        // When
        val actual = getSearchMatchedTerms(userId, "governmnt", messageId)

        // Then
        assertEquals(listOf("government"), actual)
    }

    @Test
    fun `should not read the message when the search matched nothing in it`() = runTest {
        // Given
        coEvery { messageRepository.getSearchMatchPositions(userId, searchKey, listOf(messageId)) } returns
            emptyMap<MessageId, List<SearchMatchPosition>>().right()

        // When
        val actual = getSearchMatchedTerms(userId, "governmnt", messageId)

        // Then
        assertEquals(emptyList(), actual)
        coVerify(exactly = 0) { messageRepository.getMessage(any(), any()) }
    }

    @Test
    fun `should return no terms when the match positions fail`() = runTest {
        // Given
        coEvery { messageRepository.getSearchMatchPositions(userId, searchKey, listOf(messageId)) } returns
            PaginationError.Other(DataError.Local.IllegalStateError).left()

        // When
        val actual = getSearchMatchedTerms(userId, "governmnt", messageId)

        // Then
        assertEquals(emptyList(), actual)
    }

    @Test
    fun `should return no terms when the message cannot be read`() = runTest {
        // Given
        coEvery { messageRepository.getSearchMatchPositions(userId, searchKey, listOf(messageId)) } returns mapOf(
            messageId to listOf(SearchMatchPosition(SearchMatchField.Subject, valueIndex = 0, start = 0, end = 3))
        ).right()
        coEvery { messageRepository.getMessage(userId, messageId) } returns DataError.Local.NoDataCached.left()

        // When
        val actual = getSearchMatchedTerms(userId, "governmnt", messageId)

        // Then
        assertEquals(emptyList(), actual)
    }
}
