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

import app.cash.turbine.test
import ch.protonmail.android.mailcommon.domain.sample.LabelIdSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ObserveClearMessageOperationTest {

    private val userId = UserIdSample.Primary
    private val labelId = LabelIdSample.Trash

    private val repository = mockk<MessageRepository>()

    private val observeClearMessageOperation = ObserveClearMessageOperation(repository)


    @Test
    fun `returns false if current status is false`() = runTest {
        // Given
        coEvery { repository.observeClearLabelOperation(userId, labelId) } returns flowOf(false)

        // When
        observeClearMessageOperation(userId, labelId).test {
            // Then
            assertFalse { awaitItem() }
            awaitComplete()
        }
    }

    @Test
    fun `returns true if current status is true`() = runTest {
        // Given
        coEvery { repository.observeClearLabelOperation(userId, labelId) } returns flowOf(true)

        // When
        observeClearMessageOperation(userId, labelId).test {
            // Then
            assertTrue { awaitItem() }
            awaitComplete()
        }
    }

}
