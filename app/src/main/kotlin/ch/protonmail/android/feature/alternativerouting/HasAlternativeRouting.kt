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

package ch.protonmail.android.feature.alternativerouting

import ch.protonmail.android.di.ApplicationModule.LocalDiskOpCoroutineScope
import ch.protonmail.android.mailsettings.domain.model.AlternativeRoutingPreference
import ch.protonmail.android.mailsettings.domain.repository.AlternativeRoutingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

class HasAlternativeRouting @Inject constructor(
    private val alternativeRoutingRepository: AlternativeRoutingRepository,
    @LocalDiskOpCoroutineScope
    private val coroutineScope: CoroutineScope
) {

    private val initialValue = AlternativeRoutingPreference(true)

    operator fun invoke() = alternativeRoutingRepository.observe()
        .map { alternativeRoutingPreferenceEither ->
            alternativeRoutingPreferenceEither.fold(
                ifLeft = { initialValue },
                ifRight = { it }
            )
        }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = initialValue
        )
}
