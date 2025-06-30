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

import java.time.Duration
import java.time.Instant
import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.MailFeatureId
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailcommon.domain.sample.UserSample
import ch.protonmail.android.mailcommon.domain.usecase.ObserveMailFeature
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUser
import ch.protonmail.android.mailupselling.domain.model.NPSFeedbackLastSeenPreference
import ch.protonmail.android.mailupselling.domain.repository.NPSFeedbackVisibilityRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.featureflag.domain.entity.FeatureFlag
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

internal class ObserveNPSEligibilityTest {

    private val observePrimaryUser = mockk<ObservePrimaryUser>()
    private val visibilityRepo = mockk<NPSFeedbackVisibilityRepository>()
    private val observeMailFeature = mockk<ObserveMailFeature>()

    private val sut = ObserveNPSEligibility(
        observePrimaryUser,
        visibilityRepo,
        observeMailFeature
    )

    private val user = UserSample.Primary
    private val fixedNow = Instant.ofEpochMilli(20_000_000_000L)
    private val thresholdMs = Duration.ofDays(180).toMillis()

    @Before
    fun setUp() {
        mockkStatic(Instant::class)
        every { Instant.now() } returns fixedNow
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `should emit false when primary user is null`() = runTest {
        every { observePrimaryUser() } returns flowOf(null)
        sut().test {
            assertEquals(false, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should emit false when mail feature flag is disabled`() = runTest {
        every { observePrimaryUser() } returns flowOf(user)
        isFFEnabled(false)
        // preference flow shouldn't matter—flag is off
        every { visibilityRepo.observe() } returns flowOf(NPSFeedbackLastSeenPreference(null).right())

        sut().test {
            assertEquals(false, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should emit true when feature enabled and never seen before`() = runTest {
        every { observePrimaryUser() } returns flowOf(user)
        isFFEnabled(true)
        every { visibilityRepo.observe() } returns flowOf(NPSFeedbackLastSeenPreference(null).right())

        sut().test {
            assertEquals(true, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should emit false when preferences load error`() = runTest {
        every { observePrimaryUser() } returns flowOf(user)
        isFFEnabled(true)
        every { visibilityRepo.observe() } returns flowOf(PreferencesError.left())

        sut().test {
            assertEquals(false, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should emit false when seen recently (within 180 days)`() = runTest {
        val recentTs = fixedNow.toEpochMilli() - (thresholdMs - 1)
        every { observePrimaryUser() } returns flowOf(user)
        isFFEnabled(true)
        every { visibilityRepo.observe() } returns flowOf(NPSFeedbackLastSeenPreference(recentTs).right())

        sut().test {
            assertEquals(false, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should emit true when seen long ago (180 days or more)`() = runTest {
        val oldTs = fixedNow.toEpochMilli() - (thresholdMs + 1)
        every { observePrimaryUser() } returns flowOf(user)
        isFFEnabled(true)
        every { visibilityRepo.observe() } returns flowOf(NPSFeedbackLastSeenPreference(oldTs).right())

        sut().test {
            assertEquals(true, awaitItem())
            awaitComplete()
        }
    }

    private fun isFFEnabled(enabled: Boolean) {
        every { observeMailFeature(user.userId, MailFeatureId.NPSFeedback) } returns
            flowOf(FeatureFlag.default("ff1", defaultValue = enabled))
    }
}
