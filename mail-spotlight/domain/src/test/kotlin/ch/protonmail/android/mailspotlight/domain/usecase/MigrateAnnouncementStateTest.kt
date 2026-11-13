/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailspotlight.domain.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailbyoe.domain.usecase.SyncOnboardingFinishedToSdk
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailspotlight.domain.model.Announcement
import ch.protonmail.android.mailspotlight.domain.repository.FeatureGateRepository
import ch.protonmail.android.mailspotlight.domain.repository.FeatureSpotlightRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.AfterTest
import kotlin.test.Test

internal class MigrateAnnouncementStateTest {

    private val userId = UserId("user")
    private val syncOnboardingFinishedToSdk = mockk<SyncOnboardingFinishedToSdk> {
        coEvery { this@mockk(any()) } returns Unit.right()
    }
    private val featureSpotlightRepository = mockk<FeatureSpotlightRepository> {
        coEvery { isContentSearchWhatsNewSeen() } returns false.right()
    }
    private val featureGateRepository = mockk<FeatureGateRepository> {
        coEvery { migrateClientState(any(), any()) } returns Unit.right()
    }

    private val migrate = MigrateAnnouncementState(
        syncOnboardingFinishedToSdk,
        featureSpotlightRepository,
        featureGateRepository
    )

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given the content search What's New was seen, when migrating, then it is passed as seen`() = runTest {
        // Given
        coEvery { featureSpotlightRepository.isContentSearchWhatsNewSeen() } returns true.right()

        // When
        migrate(userId)

        // Then
        coVerify { featureGateRepository.migrateClientState(userId, listOf(Announcement.ContentSearchWhatsNew)) }
    }

    @Test
    fun `given neither, when migrating, then nothing is passed as seen`() = runTest {
        // When
        migrate(userId)

        // Then
        coVerify { featureGateRepository.migrateClientState(userId, emptyList()) }
    }

    @Test
    fun `when migrating, then the onboarding is recorded as finished before the SDK migration`() = runTest {
        // When
        migrate(userId)

        // Then
        coVerifyOrder {
            syncOnboardingFinishedToSdk(userId)
            featureGateRepository.migrateClientState(userId, any())
        }
    }

    @Test
    fun `given the account was migrated, when migrating again, then the SDK is not asked again`() = runTest {
        // When
        migrate(userId)
        migrate(userId)

        // Then
        coVerify(exactly = 1) { featureGateRepository.migrateClientState(userId, any()) }
    }

    @Test
    fun `given a failed migration, when migrating again, then it is retried`() = runTest {
        // Given
        coEvery { featureGateRepository.migrateClientState(userId, any()) } returnsMany
            listOf(DataError.Local.NoUserSession.left(), Unit.right())

        // When
        migrate(userId)
        migrate(userId)

        // Then
        coVerify(exactly = 2) { featureGateRepository.migrateClientState(userId, any()) }
    }

    @Test
    fun `given the What's New seen cannot be read, when migrating, then nothing is handed over and it is retried`() =
        runTest {
            // Given
            coEvery { featureSpotlightRepository.isContentSearchWhatsNewSeen() } returnsMany
                listOf(PreferencesError.left(), true.right())

            // When
            migrate(userId)
            migrate(userId)

            // Then
            coVerify(exactly = 1) { featureGateRepository.migrateClientState(userId, any()) }
            coVerify { featureGateRepository.migrateClientState(userId, listOf(Announcement.ContentSearchWhatsNew)) }
        }

    @Test
    fun `given the onboarding cannot be recorded, when migrating, then nothing is handed over and it is retried`() =
        runTest {
            // Given
            coEvery { syncOnboardingFinishedToSdk(userId) } returnsMany
                listOf(DataError.Local.Unknown.left(), Unit.right())

            // When
            migrate(userId)
            migrate(userId)

            // Then
            coVerify(exactly = 1) { featureGateRepository.migrateClientState(userId, any()) }
        }

    @Test
    fun `given an account added later, when migrating it, then it is migrated too`() = runTest {
        // Given
        val addedUserId = UserId("added")

        // When
        migrate(userId)
        migrate(addedUserId)

        // Then
        coVerify(exactly = 1) { featureGateRepository.migrateClientState(userId, any()) }
        coVerify(exactly = 1) { featureGateRepository.migrateClientState(addedUserId, any()) }
    }
}
