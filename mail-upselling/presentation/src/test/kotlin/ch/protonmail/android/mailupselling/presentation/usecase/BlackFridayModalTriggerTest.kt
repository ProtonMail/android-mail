/*
 * Copyright (c) 2025 Proton Technologies AG
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
import arrow.core.right
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlag
import ch.protonmail.android.mailupselling.domain.model.BlackFridayPhase
import ch.protonmail.android.mailupselling.domain.model.BlackFridaySeenPreference
import ch.protonmail.android.mailupselling.domain.repository.BlackFridayRepository
import ch.protonmail.android.mailupselling.domain.usecase.GetCurrentBlackFridayPhase
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

internal class BlackFridayModalTriggerTest {

    private val repository = mockk<BlackFridayRepository>()
    private val getCurrentBlackFridayPhase = mockk<GetCurrentBlackFridayPhase>()
    private val clock = mockk<Clock>()
    private val blackFridayOptedOut = mockk<FeatureFlag<Boolean>> {
        coEvery { get() } returns false
    }

    private lateinit var blackFridayModalTrigger: BlackFridayModalTrigger

    @BeforeTest
    fun setup() {
        blackFridayModalTrigger = BlackFridayModalTrigger(
            repository,
            getCurrentBlackFridayPhase,
            clock,
            blackFridayOptedOut
        )
    }

    @AfterTest
    fun teardown() {
        clearAllMocks()
    }

    @Test
    fun `should return no trigger when the current phase is none`() = runTest {
        // Given
        expectPreferenceAt(BlackFridayPhase.Active.Wave1, 0L)
        expectPreferenceAt(BlackFridayPhase.Active.Wave2, 0L)
        expectBlackFridayPhase(BlackFridayPhase.None)

        // When + Then
        blackFridayModalTrigger.observe().test {
            assertEquals(BlackFridayPhase.None, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should show wave1 when last phase seen was more than 6 months ago`() = runTest {
        // Given
        every { clock.now() } returns Instant.fromEpochMilliseconds(1_761_667_200_764) // 28/10/25
        expectPreferenceAt(wave = BlackFridayPhase.Active.Wave1, 1_735_686_000_000) // 01/01/25
        expectPreferenceAt(BlackFridayPhase.Active.Wave2, 0L)
        expectBlackFridayPhase(BlackFridayPhase.Active.Wave1)

        // When + Then
        blackFridayModalTrigger.observe().test {
            assertEquals(BlackFridayPhase.Active.Wave1, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should show wave2 when last phase seen was more than 6 months ago`() = runTest {
        // Given
        every { clock.now() } returns Instant.fromEpochMilliseconds(1_761_667_200_764) // 28/10/25
        expectPreferenceAt(wave = BlackFridayPhase.Active.Wave1, 1_735_686_000_000) // 01/01/25
        expectPreferenceAt(BlackFridayPhase.Active.Wave2, 0L)
        expectBlackFridayPhase(BlackFridayPhase.Active.Wave2)

        // When + Then
        blackFridayModalTrigger.observe().test {
            assertEquals(BlackFridayPhase.Active.Wave2, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should not show a trigger when last wave1 seen was less than 6 months ago`() = runTest {
        // Given
        every { clock.now() } returns Instant.fromEpochMilliseconds(1_761_667_200_764) // 28/10/25
        expectPreferenceAt(wave = BlackFridayPhase.Active.Wave1, 1_759_269_600_000) // 10/10/25
        expectPreferenceAt(BlackFridayPhase.Active.Wave2, 0L)
        expectBlackFridayPhase(BlackFridayPhase.Active.Wave1)

        // When + Then
        blackFridayModalTrigger.observe().test {
            assertEquals(BlackFridayPhase.None, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should not show a trigger when last wave2 seen was less than 6 months ago`() = runTest {
        // Given
        every { clock.now() } returns Instant.fromEpochMilliseconds(1_761_667_200_764) // 28/10/25
        expectPreferenceAt(wave = BlackFridayPhase.Active.Wave1, 0L)
        expectPreferenceAt(BlackFridayPhase.Active.Wave2, 1_759_269_600_000) // 10/10/25
        expectBlackFridayPhase(BlackFridayPhase.Active.Wave2)

        // When + Then
        blackFridayModalTrigger.observe().test {
            assertEquals(BlackFridayPhase.None, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should not show a trigger when the user opted out of black friday`() = runTest {
        // Given
        expectPreferenceAt(BlackFridayPhase.Active.Wave1, 0L)
        expectPreferenceAt(BlackFridayPhase.Active.Wave2, 0L)
        expectBlackFridayPhase(BlackFridayPhase.Active.Wave2)
        coEvery { blackFridayOptedOut.get() } returns true

        // When + Then
        blackFridayModalTrigger.observe().test {
            assertEquals(BlackFridayPhase.None, awaitItem())
            awaitComplete()
        }
    }

    private fun expectBlackFridayPhase(phase: BlackFridayPhase) {
        coEvery { getCurrentBlackFridayPhase() } returns phase
    }

    private fun expectPreferenceAt(wave: BlackFridayPhase.Active, timestamp: Long) {
        every {
            repository.observePhaseEligibility(wave)
        } returns flowOf(BlackFridaySeenPreference(wave, timestamp).right())
    }
}
