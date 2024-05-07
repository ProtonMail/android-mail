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

package ch.protonmail.upselling.presentation.usecase

import java.time.Instant
import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailupselling.domain.model.OneClickUpsellingLastSeenPreference
import ch.protonmail.android.mailupselling.domain.repository.UpsellingVisibilityRepository
import ch.protonmail.android.mailupselling.presentation.usecase.ObserveUpsellingOneClickOnCooldown
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class ObserveUpsellingOneClickOnCooldownTest {

    private val repository = mockk<UpsellingVisibilityRepository>()
    private val observeUpsellingOneClickOnCooldown = ObserveUpsellingOneClickOnCooldown(repository)

    @BeforeTest
    fun setup() {
        mockkStatic(Instant::class)
    }

    @AfterTest
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should return false when the repository errors`() = runTest {
        // Given
        every { repository.observe() } returns flowOf(PreferencesError.left())
        mockInstant(Threshold + 1)

        // When + Then
        observeUpsellingOneClickOnCooldown().test {
            assertFalse(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return false value when the computed value is not exceeding the threshold`() = runTest {
        // Given
        val baseInstant = 125L
        mockInstant(Threshold + baseInstant)

        every { repository.observe() } returns flowOf(
            OneClickUpsellingLastSeenPreference(baseInstant).right()
        )

        // When + Then
        observeUpsellingOneClickOnCooldown().test {
            assertFalse(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return true value when the computed value is exceeding the threshold`() = runTest {
        // Given
        val baseInstant = 125L
        mockInstant(Threshold + baseInstant - 1)

        every { repository.observe() } returns flowOf(
            OneClickUpsellingLastSeenPreference(baseInstant).right()
        )

        // When + Then
        observeUpsellingOneClickOnCooldown().test {
            assertTrue(awaitItem())
            awaitComplete()
        }
    }

    private fun mockInstant(instant: Long = 1000L) {
        every { Instant.now() } returns mockk { every { toEpochMilli() } returns instant }
    }

    private companion object {

        const val Threshold = 10 * 24 * 60 * 60 * 1000L
    }
}
