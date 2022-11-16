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
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.testdata.message.MessageTestData
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

internal class UnStarMessageTest {

    private val messageRepository: MessageRepository = mockk {
        coEvery { removeLabel(any(), any(), any()) } returns MessageTestData.message.right()
    }

    private val messageId = MessageTestData.message.messageId

    private val unStarMessage = UnStarMessage(
        messageRepository
    )

    @Test
    fun `calls message repository to remove starred label`() = runTest {
        // When
        unStarMessage(UserIdTestData.userId, messageId)
        // Then
        coVerify { messageRepository.removeLabel(UserIdTestData.userId, messageId, SystemLabelId.Starred.labelId) }
    }

    @Test
    fun `returns unStarred message when repository succeeds`() = runTest {
        // When
        val actual = unStarMessage(UserIdTestData.userId, messageId)
        // Then
        assertEquals(MessageTestData.message.right(), actual)
    }

    @Test
    fun `returns error when repository fails`() = runTest {
        // Given
        val localError = DataError.Local.NoDataCached
        coEvery { messageRepository.removeLabel(any(), any(), any()) } returns localError.left()
        // When
        val actual = unStarMessage(UserIdTestData.userId, messageId)
        // Then
        assertEquals(localError.left(), actual)
    }
}
