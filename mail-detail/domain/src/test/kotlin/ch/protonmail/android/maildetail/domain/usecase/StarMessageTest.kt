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
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.testdata.message.MessageTestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

internal class StarMessageTest {

    private val userId = UserIdSample.Primary
    private val messageId = MessageTestData.starredMessage.messageId
    private val starredLabelId = SystemLabelId.Starred.labelId

    private val messageRepository: MessageRepository = mockk {
        coEvery {
            relabel(userId, messageId, labelsToBeAdded = listOf(starredLabelId))
        } returns MessageTestData.starredMessage.right()
    }

    private val starMessage = StarMessage(
        messageRepository
    )

    @Test
    fun `calls message repository to add starred label`() = runTest {
        // When
        starMessage(userId, messageId)

        // Then
        coVerify { messageRepository.relabel(userId, messageId, emptyList(), listOf(starredLabelId)) }
    }

    @Test
    fun `returns starred message when repository succeeds`() = runTest {
        // When
        val actual = starMessage(userId, messageId)

        // Then
        assertEquals(MessageTestData.starredMessage.right(), actual)
    }

    @Test
    fun `returns error when repository fails`() = runTest {
        // Given
        val localError = DataError.Local.NoDataCached
        coEvery {
            messageRepository.relabel(
                userId = userId,
                messageId = messageId,
                labelsToBeAdded = listOf(starredLabelId)
            )
        } returns localError.left()

        // When
        val actual = starMessage(userId, messageId)

        // Then
        assertEquals(localError.left(), actual)
    }
}
