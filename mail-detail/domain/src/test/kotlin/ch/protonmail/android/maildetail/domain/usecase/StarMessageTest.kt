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

import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.testdata.message.MessageTestData
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

internal class StarMessageTest {

    private val messageRepository: MessageRepository = mockk {
        every { addLabel(any(), any(), any()) } returns flowOf(MessageTestData.starredMessage.right())
    }

    private val messageId = MessageTestData.starredMessage.messageId

    private val starMessage = StarMessage(
        messageRepository
    )

    @Test
    fun `calls message repository to add starred label`() {
        // When
        starMessage(UserIdTestData.userId, messageId)
        // Then
        verify { messageRepository.addLabel(UserIdTestData.userId, messageId, SystemLabelId.Starred.labelId) }
    }

    @Test
    fun `returns starred message when repository succeeds`() = runTest {
        // When
        starMessage(UserIdTestData.userId, messageId).test {
            // Then
            assertEquals(MessageTestData.starredMessage.right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `returns error when repository fails`() = runTest {
        // Given
        val localError = DataError.Local.NoDataCached
        every { messageRepository.addLabel(any(), any(), any()) } returns flowOf(localError.left())
        // When
        starMessage(UserIdTestData.userId, messageId).test {
            // Then
            assertEquals(localError.left(), awaitItem())
            awaitComplete()
        }
    }
}
