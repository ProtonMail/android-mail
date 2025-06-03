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

package ch.protonmail.upselling.domain.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.sample.UserSample
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUser
import ch.protonmail.android.mailupselling.domain.usecase.AvailableDriveStorage
import ch.protonmail.android.mailupselling.domain.usecase.GetAvailableDriveStorage
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class GetAvailableDriveStorageTest {

    private val observePrimaryUser = mockk<ObservePrimaryUser>()

    private val sut = GetAvailableDriveStorage(observePrimaryUser)

    private val oneGB = 1024 * 1024 * 1024L
    private val gb20 = 20 * oneGB
    private val gb500 = 500 * oneGB

    private val freeUser1GB = UserSample.Primary.copy(subscribed = 0, maxDriveSpace = oneGB)
    private val paidUser20GB = UserSample.Primary.copy(subscribed = 1, maxSpace = gb20, maxDriveSpace = null)
    private val paidUser500GB = UserSample.Primary.copy(subscribed = 1, maxSpace = gb500, maxDriveSpace = null)

    @Test
    fun `returns error if no primary user`() = runTest {
        // Given
        every { observePrimaryUser.invoke() } returns flowOf(null)

        // When
        val result = sut()

        // Then
        assertEquals(GetAvailableDriveStorage.GetAvailableDriveStorageError.left(), result)
    }

    @Test
    fun `returns 1 GB for 1 GB free user`() = runTest {
        // Given
        every { observePrimaryUser.invoke() } returns flowOf(freeUser1GB)

        // When
        val result = sut()

        // Then
        assertEquals(AvailableDriveStorage(1).right(), result)
    }

    @Test
    fun `returns 20 GB for 20 GB paid user`() = runTest {
        // Given
        every { observePrimaryUser.invoke() } returns flowOf(paidUser20GB)

        // When
        val result = sut()

        // Then
        assertEquals(AvailableDriveStorage(20).right(), result)
    }

    @Test
    fun `returns 500 GB for 500 GB paid user`() = runTest {
        // Given
        every { observePrimaryUser.invoke() } returns flowOf(paidUser500GB)

        // When
        val result = sut()

        // Then
        assertEquals(AvailableDriveStorage(500).right(), result)
    }
}
