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

package ch.protonmail.android.mailupselling.presentation.usecase

import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailcommon.domain.sample.UserSample
import ch.protonmail.android.mailupselling.domain.model.DriveSpotlightLastSeenPreference
import ch.protonmail.android.mailupselling.domain.model.telemetry.data.AccountAge
import ch.protonmail.android.mailupselling.domain.repository.DriveSpotlightVisibilityRepository
import ch.protonmail.android.mailupselling.domain.usecase.GetAccountAgeInDays
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import javax.inject.Provider
import kotlin.test.AfterTest
import kotlin.test.assertEquals

class ObserveDriveSpotlightVisibilityTest {

    private val repo = mockk<DriveSpotlightVisibilityRepository>()
    private val accountAgeInDays = mockk<GetAccountAgeInDays>()
    private val driveSpotlightEnabled = mockk<Provider<Boolean>>()
    private val sut: ObserveDriveSpotlightVisibility
        get() = ObserveDriveSpotlightVisibility(
            repo = repo,
            getAccountAgeInDays = accountAgeInDays,
            driveSpotlightEnabled = driveSpotlightEnabled.get()
        )
    private val user = UserSample.Primary

    @AfterTest
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should return false if observed FF is disabled`() = runTest {
        // Given
        expectDriveSpotlightEnabled(false)
        // When + Then
        sut(user).test {
            assertEquals(false, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return true if not already seen and account old enough`() = runTest {
        // Given
        expectDriveSpotlightEnabled(true)
        every { accountAgeInDays.invoke(user) } returns AccountAge(30)
        every { repo.observe() } returns flowOf(DriveSpotlightLastSeenPreference(null).right())
        // When + Then
        sut(user).test {
            assertEquals(true, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return true if not already seen and account old enough and no preference yet`() = runTest {
        // Given
        expectDriveSpotlightEnabled(true)
        every { accountAgeInDays.invoke(user) } returns AccountAge(30)
        every { repo.observe() } returns flowOf(PreferencesError.left())
        // When + Then
        sut(user).test {
            assertEquals(true, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return false if account not old enough`() = runTest {
        // Given
        expectDriveSpotlightEnabled(true)
        every { accountAgeInDays.invoke(user) } returns AccountAge(29)
        every { repo.observe() } returns flowOf(DriveSpotlightLastSeenPreference(null).right())
        // When + Then
        sut(user).test {
            assertEquals(false, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return false if already seen`() = runTest {
        // Given
        expectDriveSpotlightEnabled(true)
        every { accountAgeInDays.invoke(user) } returns AccountAge(30)
        every { repo.observe() } returns flowOf(DriveSpotlightLastSeenPreference(1L).right())
        // When + Then
        sut(user).test {
            assertEquals(false, awaitItem())
            awaitComplete()
        }
    }

    private fun expectDriveSpotlightEnabled(value: Boolean) {
        every { driveSpotlightEnabled.get() } returns value
    }
}

