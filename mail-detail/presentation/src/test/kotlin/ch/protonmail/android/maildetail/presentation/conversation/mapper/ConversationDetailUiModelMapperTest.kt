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

package ch.protonmail.android.maildetail.presentation.conversation.mapper

import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.maildetail.presentation.conversation.model.ConversationUiModel
import ch.protonmail.android.testdata.conversation.ConversationTestData.buildConversation
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConversationDetailUiModelMapperTest {

    private val mapper = ConversationDetailUiModelMapper()

    @Test
    fun `map conversation to conversation ui model`() {
        // Given
        val conversation = buildConversation(
            userId = userId,
            id = RAW_CONVERSATION_ID,
            subject = SUBJECT,
            numMessages = 3
        )
        // When
        val actual = mapper.toUiModel(conversation)
        // Then
        val expected = ConversationUiModel(
            conversationId = ConversationId(RAW_CONVERSATION_ID),
            subject = SUBJECT,
            isStarred = false,
            messageCount = 3
        )
        assertEquals(expected, actual)
    }

    @Test
    fun `map conversation with starred label to starred conversation ui model`() {
        // Given
        val conversation = buildConversation(
            userId = userId,
            id = RAW_CONVERSATION_ID,
            labelIds = listOf("10")
        )
        // When
        val actual = mapper.toUiModel(conversation)
        // Then
        assertTrue(actual.isStarred)
    }

    companion object {

        const val RAW_CONVERSATION_ID = "rawConversationId"
        const val SUBJECT = "Here's a new email"
    }
}
