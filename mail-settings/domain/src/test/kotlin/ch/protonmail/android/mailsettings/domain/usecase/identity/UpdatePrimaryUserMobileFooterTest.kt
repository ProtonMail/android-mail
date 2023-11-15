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
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailsettings.domain.model.MobileFooterPreference
import ch.protonmail.android.mailsettings.domain.repository.MobileFooterRepository
import io.mockk.called
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

internal class UpdatePrimaryUserMobileFooterTest {

    private val observePrimaryUserId = mockk<ObservePrimaryUserId>()
    private val mobileFooterRepository = mockk<MobileFooterRepository>()
    private val updatePrimaryUserMobileFooter =
        UpdatePrimaryUserMobileFooter(observePrimaryUserId, mobileFooterRepository)

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should call the repository with the correct values when updating the value`() = runTest {
        // Given
        coEvery { observePrimaryUserId() } returns flowOf(BaseUserId)
        coEvery {
            mobileFooterRepository.updateMobileFooter(
                BaseUserId,
                BaseMobileFooterPreference
            )
        } returns Unit.right()

        // When
        val result = updatePrimaryUserMobileFooter(BaseMobileFooterPreference)

        // Then
        assertTrue(result.isRight())
    }

    @Test
    fun `should return an error when the primary user id cannot be fetched`() = runTest {
        // Given
        val expectedError = UpdatePrimaryUserMobileFooter.Error.UserIdNotFound.left()
        coEvery { observePrimaryUserId() } returns flowOf(null)

        // When
        val result = updatePrimaryUserMobileFooter(BaseMobileFooterPreference)

        // Then
        assertEquals(expectedError, result)
        verify { mobileFooterRepository wasNot called }
    }

    @Test
    fun `should return an error when the update fails`() = runTest {
        // Given
        val expectedError = UpdatePrimaryUserMobileFooter.Error.UpdateFailure.left()
        coEvery { observePrimaryUserId() } returns flowOf(BaseUserId)
        coEvery {
            mobileFooterRepository.updateMobileFooter(
                BaseUserId,
                BaseMobileFooterPreference
            )
        } returns DataError.Local.Unknown.left()

        // When
        val result = updatePrimaryUserMobileFooter(BaseMobileFooterPreference)

        // Then
        assertEquals(expectedError, result)
    }

    private companion object {

        val BaseUserId = UserId("123")
        val BaseMobileFooterPreference = MobileFooterPreference(value = "123", enabled = true)
    }
}
