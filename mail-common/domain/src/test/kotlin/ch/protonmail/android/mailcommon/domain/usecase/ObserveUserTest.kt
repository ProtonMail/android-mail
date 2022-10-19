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

package ch.protonmail.android.mailcommon.domain.usecase

import app.cash.turbine.test
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import ch.protonmail.android.testdata.user.UserTestData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.user.domain.UserManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class ObserveUserTest {

    private val userManager: UserManager = mockk {
        every { observeUser(userId) } returns flowOf(UserTestData.Primary)
    }
    private val observeUser = ObserveUser(userManager = userManager)

    @Test
    fun `return correct user`() = runTest {
        // when
        observeUser(userId).test {

            // then
            val expected = UserTestData.Primary
            assertEquals(expected, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `returns null on error`() = runTest {
        // given
        every { userManager.observeUser(userId) } returns flowOf(null)

        // when
        observeUser(userId).test {

            // then
            assertNull(awaitItem())
            awaitComplete()
        }
    }
}
