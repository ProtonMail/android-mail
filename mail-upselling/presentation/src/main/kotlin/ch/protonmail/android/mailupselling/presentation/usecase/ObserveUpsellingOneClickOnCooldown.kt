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

import java.time.Instant
import arrow.core.getOrElse
import ch.protonmail.android.mailupselling.domain.model.OneClickUpsellingLastSeenPreference
import ch.protonmail.android.mailupselling.domain.repository.UpsellingVisibilityRepository
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject

class ObserveUpsellingOneClickOnCooldown @Inject constructor(
    private val upsellingVisibilityRepository: UpsellingVisibilityRepository
) {

    operator fun invoke() = upsellingVisibilityRepository.observe().mapLatest {
        val lastSeen = it.getOrElse { Default }.timestamp
        val currentTime = Instant.now().toEpochMilli()

        currentTime - lastSeen < Threshold
    }

    private companion object {

        // Default threshold is 10 days.
        const val Threshold = 10 * 24 * 60 * 60 * 1000L
        val Default = OneClickUpsellingLastSeenPreference(0L)
    }
}
