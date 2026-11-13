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

import arrow.core.Either
import arrow.core.raise.either
import ch.protonmail.android.mailbyoe.domain.repository.ByoeGuidanceRepository
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailonboarding.domain.usecase.ObserveOnboarding
import kotlinx.coroutines.flow.first
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

/**
 * The SDK shows no announcement until it knows the onboarding is finished, and the app keeps that in its own
 * preference. Copies it over, which also covers installs that finished the onboarding before.
 *
 * Fails when the preference or the SDK cannot be read or written, so the caller can try again. An onboarding still to
 * finish is not a failure: there is nothing to record yet.
 */
class SyncOnboardingFinishedToSdk @Inject constructor(
    private val observeOnboarding: ObserveOnboarding,
    private val byoeGuidanceRepository: ByoeGuidanceRepository
) {

    suspend operator fun invoke(userId: UserId): Either<DataError, Unit> = either {
        val onboarding = observeOnboarding().first()
            .onLeft { Timber.e("byoe: unable to read the onboarding preference: $it") }
            .mapLeft { DataError.Local.Unknown }
            .bind()
        if (onboarding.display) return@either

        if (byoeGuidanceRepository.isOnboardingFinishedRecorded(userId).bind()) return@either

        byoeGuidanceRepository.recordOnboardingFinished(userId)
            .onLeft { Timber.e("byoe: unable to record the onboarding as finished: $it") }
            .onRight { Timber.d("byoe: onboarding recorded as finished") }
            .bind()
    }
}
