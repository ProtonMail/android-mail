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

package ch.protonmail.android.mailsettings.domain.usecase.identity

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.mailcommon.domain.usecase.GetPrimaryAddress
import ch.protonmail.android.mailsettings.domain.model.DisplayName
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

internal class GetPrimaryAddressDisplayNameTest {

    private val getPrimaryAddress: GetPrimaryAddress = mockk<GetPrimaryAddress>()
    private val getPrimaryAddressDisplayName = GetPrimaryAddressDisplayName(getPrimaryAddress)

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should return a valid display name`() = runTest {
        // Given
        val expectedDisplayName = DisplayName(BaseDisplayName).right()
        coEvery { getPrimaryAddress(BaseUserId) } returns UserAddressSample.PrimaryAddress.right()

        // When
        val result = getPrimaryAddressDisplayName(BaseUserId)

        // Then
        assertEquals(expectedDisplayName, result)
    }

    @Test
    fun `should return an error if the address is not found`() = runTest {
        // Given
        val expectedError = DataError.AddressNotFound.left()
        coEvery { getPrimaryAddress(BaseUserId) } returns expectedError

        // When
        val result = getPrimaryAddressDisplayName(BaseUserId)

        // Then
        assertEquals(expectedError, result)
    }

    private companion object {

        val BaseUserId = UserId("123")
        const val BaseDisplayName = "name"
    }
}
