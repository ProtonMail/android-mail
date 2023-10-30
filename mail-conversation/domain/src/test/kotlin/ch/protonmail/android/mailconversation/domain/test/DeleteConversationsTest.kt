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

import arrow.core.right
import ch.protonmail.android.mailcommon.domain.sample.ConversationIdSample
import ch.protonmail.android.mailcommon.domain.sample.LabelIdSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import ch.protonmail.android.mailconversation.domain.usecase.DeleteConversations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class DeleteConversationsTest {

    private val userId = UserIdSample.Primary
    private val messageIds = listOf(ConversationIdSample.Invoices, ConversationIdSample.WeatherForecast)
    private val currentLabel = LabelIdSample.Trash

    private val conversationRepository = mockk<ConversationRepository>()

    private val deleteConversations by lazy { DeleteConversations(conversationRepository) }

    @Test
    fun `delete conversations calls repository with given parameters`() = runTest {
        // Given
        coEvery { conversationRepository.deleteConversations(userId, messageIds, currentLabel) } returns Unit.right()

        // When
        deleteConversations(userId, messageIds, currentLabel)

        // Then
        coVerify { conversationRepository.deleteConversations(userId, messageIds, currentLabel) }
    }
}
