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

package ch.protonmail.android.mailmessage.domain.usecase

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

internal class StarMessagesTest {

    private val userId = UserIdSample.Primary
    private val messageIds = listOf(MessageTestData.starredMessage.messageId)
    private val starredLabelId = SystemLabelId.Starred.labelId

    private val messageRepository: MessageRepository = mockk {
        coEvery {
            relabel(userId, messageIds, labelsToBeAdded = listOf(starredLabelId))
        } returns listOf(MessageTestData.starredMessage).right()
    }

    private val starMessages = StarMessages(
        messageRepository
    )

    @Test
    fun `calls message repository to add starred label`() = runTest {
        // When
        starMessages(userId, messageIds)

        // Then
        coVerify { messageRepository.relabel(userId, messageIds, emptyList(), listOf(starredLabelId)) }
    }

    @Test
    fun `returns starred message when repository succeeds`() = runTest {
        // When
        val actual = starMessages(userId, messageIds)

        // Then
        assertEquals(listOf(MessageTestData.starredMessage).right(), actual)
    }

    @Test
    fun `returns error when repository fails`() = runTest {
        // Given
        val localError = DataError.Local.NoDataCached
        coEvery {
            messageRepository.relabel(
                userId = userId,
                messageIds = messageIds,
                labelsToBeAdded = listOf(starredLabelId)
            )
        } returns localError.left()

        // When
        val actual = starMessages(userId, messageIds)

        // Then
        assertEquals(localError.left(), actual)
    }
}
