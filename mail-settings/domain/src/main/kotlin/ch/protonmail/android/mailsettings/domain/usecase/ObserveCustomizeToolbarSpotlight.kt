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

package ch.protonmail.android.mailsettings.domain.usecase

import arrow.core.getOrElse
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailsettings.domain.model.SpotlightLastSeenPreference
import ch.protonmail.android.mailsettings.domain.repository.LocalSpotlightEventsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject

class ObserveCustomizeToolbarSpotlight @Inject constructor(
    private val repo: LocalSpotlightEventsRepository,
    private val observePrimaryUserId: ObservePrimaryUserId
) {

    operator fun invoke(): Flow<Unit> {
        return observePrimaryUserId().flatMapLatest {
            delay(DefaultDelayMs)
            repo.observeCustomizeToolbar().mapNotNull {
                it.getOrElse { Default }
            }.filter {
                it.seen.not()
            }.map { }
        }
    }

    private companion object {
        val Default = SpotlightLastSeenPreference(seen = false)
        const val DefaultDelayMs = 2000L
    }
}
