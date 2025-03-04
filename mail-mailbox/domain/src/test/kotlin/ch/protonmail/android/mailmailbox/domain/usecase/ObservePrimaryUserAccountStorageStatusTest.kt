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

package ch.protonmail.android.mailmailbox.domain.usecase

import app.cash.turbine.test
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUser
import ch.protonmail.android.mailmailbox.domain.model.UserAccountStorageStatus
import ch.protonmail.android.testdata.user.UserTestData
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class ObservePrimaryUserAccountStorageStatusTest {

    private val primaryUser = UserTestData.Primary

    private val observePrimaryUser = mockk<ObservePrimaryUser>()

    private val observePrimaryUserAccountStorageStatus = ObservePrimaryUserAccountStorageStatus(observePrimaryUser)

    @Test
    fun `should get BASE storage status from primary user when account storage is split`() = runTest {
        // Given
        val storageStatus = UserAccountStorageStatus(
            usedSpace = primaryUser.usedBaseSpace!!,
            maxSpace = primaryUser.maxBaseSpace!!
        )
        every { observePrimaryUser() } returns flowOf(primaryUser)

        // When
        observePrimaryUserAccountStorageStatus.invoke().test {
            val actual = awaitItem()

            awaitComplete()

            // Then
            verify { observePrimaryUser() }
            assertEquals(storageStatus, actual)
        }
    }

    @Test
    fun `should get TOTAL storage status from primary user when account storage is unified`() = runTest {
        // Given
        val storageStatus = UserAccountStorageStatus(
            usedSpace = primaryUser.usedSpace,
            maxSpace = primaryUser.maxSpace
        )
        every { observePrimaryUser() } returns flowOf(
            primaryUser.copy(
                usedBaseSpace = null,
                maxBaseSpace = null
            )
        )

        // When
        observePrimaryUserAccountStorageStatus.invoke().test {
            val actual = awaitItem()

            awaitComplete()

            // Then
            verify { observePrimaryUser() }
            assertEquals(storageStatus, actual)
        }
    }
}
