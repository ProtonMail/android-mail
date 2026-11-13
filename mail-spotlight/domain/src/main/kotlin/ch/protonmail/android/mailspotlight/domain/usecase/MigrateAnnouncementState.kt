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

import arrow.core.getOrElse
import ch.protonmail.android.mailbyoe.domain.usecase.SyncOnboardingFinishedToSdk
import ch.protonmail.android.mailspotlight.domain.model.Announcement
import ch.protonmail.android.mailspotlight.domain.repository.FeatureGateRepository
import ch.protonmail.android.mailspotlight.domain.repository.FeatureSpotlightRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hands the SDK what the app knew before it owned the announcements. Runs once per process for each account, before
 * its first announcement, so an account added later is covered too. The SDK keeps only the first successful run, so
 * nothing is handed over when a step fails: the next ask tries again.
 *
 * A new user needs nothing here: the SDK skips the What's New for an account that went through the onboarding.
 */
@Singleton
class MigrateAnnouncementState @Inject constructor(
    private val syncOnboardingFinishedToSdk: SyncOnboardingFinishedToSdk,
    private val featureSpotlightRepository: FeatureSpotlightRepository,
    private val featureGateRepository: FeatureGateRepository
) {

    private val mutex = Mutex()
    private val migratedUserIds = mutableSetOf<UserId>()

    suspend operator fun invoke(userId: UserId) = mutex.withLock {
        if (userId in migratedUserIds) return@withLock

        // Without it, the SDK shows no announcement.
        syncOnboardingFinishedToSdk(userId).onLeft {
            Timber.e("feature-gate: unable to record the onboarding as finished, retrying on the next ask: $it")
            return@withLock
        }

        val isWhatsNewSeen = featureSpotlightRepository.isContentSearchWhatsNewSeen().getOrElse {
            Timber.e("feature-gate: unable to read the What's New seen on this device, retrying on the next ask: $it")
            return@withLock
        }
        val seen = if (isWhatsNewSeen) listOf(Announcement.ContentSearchWhatsNew) else emptyList()

        featureGateRepository.migrateClientState(userId, seen)
            .onLeft { Timber.e("feature-gate: unable to migrate the client state, retrying on the next ask: $it") }
            .onRight { migratedUserIds += userId }
    }
}
