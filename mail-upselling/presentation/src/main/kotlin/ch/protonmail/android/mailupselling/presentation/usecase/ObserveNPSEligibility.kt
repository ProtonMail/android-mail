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
import ch.protonmail.android.mailcommon.domain.MailFeatureId
import ch.protonmail.android.mailcommon.domain.usecase.ObserveMailFeature
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUser
import ch.protonmail.android.mailupselling.domain.repository.NPSFeedbackVisibilityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class ObserveNPSEligibility @Inject constructor(
    private val observePrimaryUser: ObservePrimaryUser,
    private val visibilityRepo: NPSFeedbackVisibilityRepository,
    private val observeMailFeature: ObserveMailFeature
) {
    operator fun invoke(): Flow<Boolean> = observePrimaryUser()
        .distinctUntilChanged()
        .flatMapLatest { user ->
            if (user == null) return@flatMapLatest flowOf(false)
            combine(
                observeMailFeature(user.userId, MailFeatureId.NPSFeedback),
                visibilityRepo.observe()
            ) { npsFeatureFlag, prefEither ->

                if (npsFeatureFlag.value.not()) return@combine false

                prefEither.map { pref ->
                    val now = Instant.now().toEpochMilli()
                    val seenTimestamp = pref.seenTimestamp
                    val seenInPeriod = seenTimestamp != null && now - seenTimestamp < THRESHOLD_180_DAYS
                    !seenInPeriod
                }.getOrNull() ?: false
            }
        }
}

private val THRESHOLD_180_DAYS = Duration.ofDays(180).toMillis()
