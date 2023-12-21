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
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.ConversationIdSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailconversation.domain.usecase.MoveConversations
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MoveConversationTest {

    private val userId = UserIdSample.Primary
    private val conversationId = ConversationIdSample.WeatherForecast

    private val moveConversations: MoveConversations = mockk()

    private val move = MoveConversation(moveConversations)

    @Test
    fun `when move conversations returns error then return error`() = runTest {
        // Given
        val error = DataError.Local.NoDataCached.left()
        coEvery { moveConversations(userId, listOf(conversationId), any()) } returns error

        // When
        val actual = move(userId, conversationId, SystemLabelId.Trash.labelId)

        // Then
        assertEquals(error, actual)
    }

    @Test
    fun `when moving a conversation to trash then move conversations is called with the given data`() = runTest {
        // Given
        val toLabel = SystemLabelId.Trash.labelId
        coEvery { moveConversations(userId, listOf(conversationId), toLabel) } returns Unit.right()

        // When
        move(userId, conversationId, toLabel)

        // Then
        coVerify { moveConversations(userId, listOf(conversationId), toLabel) }
    }
}
