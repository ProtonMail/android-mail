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
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.usecase.MoveMessages
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class MoveMessageTest {

    private val userId = UserIdSample.Primary
    private val messageId = MessageIdSample.AugWeatherForecast
    private val toLabelId = SystemLabelId.Spam.labelId

    private val moveMessages = mockk<MoveMessages>()

    private val move = MoveMessage(moveMessages)


    @Test
    fun `when move to returns error then return error`() = runTest {
        // Given
        val error = DataError.Local.NoDataCached.left()
        coEvery { moveMessages(userId, listOf(messageId), toLabelId) } returns error

        // When
        val actual = move(userId, messageId, toLabelId)

        // Then
        assertEquals(error, actual)
    }

    @Test
    fun `when move messages succeeds then return success`() = runTest {
        // Given
        coEvery { moveMessages(userId, listOf(messageId), toLabelId) } returns Unit.right()

        // When
        move(userId, messageId, toLabelId)

        // Then
        coVerify { moveMessages(userId, listOf(messageId), toLabelId) }
    }
}
