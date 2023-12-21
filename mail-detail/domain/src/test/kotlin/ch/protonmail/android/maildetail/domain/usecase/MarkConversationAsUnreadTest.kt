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
import ch.protonmail.android.mailcommon.domain.sample.ConversationIdSample
import ch.protonmail.android.mailcommon.domain.sample.DataErrorSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailconversation.domain.sample.ConversationSample
import ch.protonmail.android.mailconversation.domain.usecase.MarkConversationsAsUnread
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class MarkConversationAsUnreadTest {

    private val userId = UserIdSample.Primary
    private val conversationId = ConversationIdSample.WeatherForecast

    private val markConversationsAsUnread: MarkConversationsAsUnread = mockk()

    private val markUnread = MarkConversationAsUnread(markConversationsAsUnread)

    @Test
    fun `returns error when repository fails`() = runTest {
        // given
        val error = DataErrorSample.NoCache.left()
        coEvery { markConversationsAsUnread(userId, listOf(conversationId)) } returns error

        // when
        val result = markUnread(userId, conversationId)

        // then
        assertEquals(error, result)
    }

    @Test
    fun `returns updated conversation when repository succeeds`() = runTest {
        // given
        val conversation = ConversationSample.WeatherForecast
        coEvery { markConversationsAsUnread(userId, listOf(conversationId)) } returns listOf(conversation).right()

        // when
        val result = markUnread(userId, conversationId)

        // then
        assertEquals(conversation.right(), result)
    }
}
