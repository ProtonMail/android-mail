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

package ch.protonmail.android.mailcontentsearch.domain.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchRecentsRepository
import ch.protonmail.android.mailmessage.domain.model.MessageId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.Test
import kotlin.test.assertEquals

internal class TouchRecentFoundItemTest {

    private val userId = UserId("user-1")
    private val messageId = MessageId("42")
    private val repository = mockk<ContentSearchRecentsRepository>()
    private val touchRecentFoundItem = TouchRecentFoundItem(repository)

    @Test
    fun `delegates bumping the found item to the repository`() = runTest {
        // Given
        coEvery { repository.touchFoundItem(userId, messageId) } returns Unit.right()

        // When
        val result = touchRecentFoundItem(userId, messageId)

        // Then
        assertEquals(Unit.right(), result)
        coVerify { repository.touchFoundItem(userId, messageId) }
    }

    @Test
    fun `surfaces a failure from the repository`() = runTest {
        // Given
        coEvery { repository.touchFoundItem(userId, messageId) } returns DataError.Local.Unknown.left()

        // When
        val result = touchRecentFoundItem(userId, messageId)

        // Then
        assertEquals(DataError.Local.Unknown.left(), result)
    }
}
