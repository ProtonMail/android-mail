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

package ch.protonmail.android.mailupselling.presentation.usecase

import ch.protonmail.android.mailupselling.domain.model.FallPromoPhase
import ch.protonmail.android.mailupselling.domain.repository.FallPromoRepository
import ch.protonmail.android.mailupselling.domain.usecase.GetCurrentFallPromoPhase
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlin.time.times
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class FallPromoModalTrigger @Inject constructor(
    private val repository: FallPromoRepository,
    private val getCurrentFallPromoPhase: GetCurrentFallPromoPhase,
    private val clock: Clock
) {

    fun observe(): Flow<FallPromoPhase> = combine(
        repository.observePhaseEligibility(FallPromoPhase.Active.Wave1),
        repository.observePhaseEligibility(FallPromoPhase.Active.Wave2)
    ) { wave1Pref, wave2Pref ->
        val wave1Timestamp = wave1Pref.getOrNull()?.timestamp ?: 0L
        val wave2Timestamp = wave2Pref.getOrNull()?.timestamp ?: 0L

        invoke(wave1Timestamp, wave2Timestamp)
    }

    private suspend fun invoke(wave1Timestamp: Long, wave2Timestamp: Long): FallPromoPhase {
        val currentPhase = getCurrentFallPromoPhase()

        return when (currentPhase) {
            FallPromoPhase.None -> FallPromoPhase.None
            FallPromoPhase.Active.Wave1 -> {
                if (shouldShow(wave1Timestamp)) FallPromoPhase.Active.Wave1 else FallPromoPhase.None
            }

            FallPromoPhase.Active.Wave2 -> {
                if (shouldShow(wave2Timestamp)) FallPromoPhase.Active.Wave2 else FallPromoPhase.None
            }
        }
    }

    private fun shouldShow(lastSeenTimestamp: Long): Boolean {
        if (lastSeenTimestamp == 0L) return true

        val now = clock.now()
        val lastSeen = Instant.fromEpochMilliseconds(lastSeenTimestamp)
        val sixMonthsAgo = now.minus(6 * 30.days)

        return lastSeen < sixMonthsAgo
    }
}
