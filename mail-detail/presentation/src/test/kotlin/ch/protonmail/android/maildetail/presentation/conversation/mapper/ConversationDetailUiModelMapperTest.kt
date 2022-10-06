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

import ch.protonmail.android.maildetail.presentation.mapper.ConversationDetailUiModelMapper
import ch.protonmail.android.testdata.conversation.ConversationTestData
import ch.protonmail.android.testdata.conversation.ConversationUiModelTestData
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConversationDetailUiModelMapperTest {

    private val mapper = ConversationDetailUiModelMapper()

    @Test
    fun `map conversation to conversation ui model`() {
        // When
        val actual = mapper.toUiModel(ConversationTestData.conversationWith3Messages)
        // Then
        assertEquals(ConversationUiModelTestData.uiModelWith3Messages, actual)
    }

    @Test
    fun `map conversation with starred label to starred conversation ui model`() {
        // When
        val actual = mapper.toUiModel(ConversationTestData.starredConversation)
        // Then
        assertTrue(actual.isStarred)
    }
}
