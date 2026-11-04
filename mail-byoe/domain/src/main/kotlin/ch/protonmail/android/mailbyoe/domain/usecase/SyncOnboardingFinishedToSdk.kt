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

package ch.protonmail.android.mailbyoe.domain.usecase

import ch.protonmail.android.mailbyoe.domain.repository.ByoeGuidanceRepository
import ch.protonmail.android.mailonboarding.domain.usecase.ObserveOnboarding
import kotlinx.coroutines.flow.first
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

/**
 * The SDK only offers the BYOE spotlight once it knows the onboarding is finished, and the app keeps that in its
 * own preference. Copies it over once per user, which also covers installs that finished the onboarding before.
 */
class SyncOnboardingFinishedToSdk @Inject constructor(
    private val observeOnboarding: ObserveOnboarding,
    private val byoeGuidanceRepository: ByoeGuidanceRepository
) {

    suspend operator fun invoke(userId: UserId) {
        val isOnboardingFinished = observeOnboarding().first().getOrNull()?.display == false
        if (!isOnboardingFinished) return

        val isRecorded = byoeGuidanceRepository.isOnboardingFinishedRecorded(userId).getOrNull() ?: return
        if (isRecorded) return

        byoeGuidanceRepository.recordOnboardingFinished(userId)
            .onLeft { Timber.e("byoe: unable to record the onboarding as finished: $it") }
            .onRight { Timber.d("byoe: onboarding recorded as finished") }
    }
}
