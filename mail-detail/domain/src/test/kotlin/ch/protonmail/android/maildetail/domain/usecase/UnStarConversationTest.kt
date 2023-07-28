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

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.testdata.conversation.ConversationTestData
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class UnStarConversationTest {

    private val conversationRepository: ConversationRepository = mockk {
        coEvery { removeLabel(any(), any<ConversationId>(), any()) } returns ConversationTestData.conversation.right()
    }

    private val conversationId = ConversationTestData.starredConversation.conversationId

    private val unStarConversation = UnStarConversation(conversationRepository)

    @Test
    fun `call conversation repository to remove starred label`() = runTest {
        // When
        unStarConversation(UserIdTestData.userId, conversationId)
        // Then
        coVerify {
            conversationRepository.removeLabel(
                UserIdTestData.userId,
                conversationId,
                SystemLabelId.Starred.labelId
            )
        }
    }

    @Test
    fun `return unStarred conversation when repository succeeds`() = runTest {
        // When
        val actual = unStarConversation(UserIdTestData.userId, conversationId)
        // Then
        assertEquals(ConversationTestData.conversation.right(), actual)
    }

    @Test
    fun `returns error when repository fails`() = runTest {
        // Given
        val localError = DataError.Local.NoDataCached
        coEvery { conversationRepository.removeLabel(any(), any<ConversationId>(), any()) } returns localError.left()
        // When
        val actual = unStarConversation(UserIdTestData.userId, conversationId)
        // Then
        assertEquals(DataError.Local.NoDataCached.left(), actual)
    }
}
