/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailupselling.data.local

import androidx.datastore.preferences.core.longPreferencesKey
import arrow.core.Either
import ch.protonmail.android.mailcommon.data.mapper.safeData
import ch.protonmail.android.mailcommon.data.mapper.safeEdit
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailupselling.data.SpringPromoDataStoreProvider
import ch.protonmail.android.mailupselling.domain.model.SpringOfferSeenPreference
import ch.protonmail.android.mailupselling.domain.model.SpringPromoPhase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.time.Clock

class SpringPromoLocalDataSourceImpl @Inject constructor(
    private val dataStoreProvider: SpringPromoDataStoreProvider
) : SpringPromoLocalDataSource {

    private val wave1FirstSeenPrefKey = longPreferencesKey(
        SpringPromoDataStoreProvider.PHASE_1_SEEN_KEY
    )

    private val wave2FirstSeenPrefKey = longPreferencesKey(
        SpringPromoDataStoreProvider.PHASE_2_SEEN_KEY
    )

    override fun observePhaseEligibility(
        phase: SpringPromoPhase.Active
    ): Flow<Either<PreferencesError, SpringOfferSeenPreference>> =
        dataStoreProvider.springPromoDataSource.safeData.map { prefsEither ->
            prefsEither.map { prefs ->
                val phaseSeen = prefs[phase.toPrefKey()] ?: 0L
                SpringOfferSeenPreference(phase, phaseSeen)
            }
        }

    override suspend fun saveSeen(phase: SpringPromoPhase.Active): Either<PreferencesError, Unit> =
        dataStoreProvider.springPromoDataSource.safeEdit { mutablePreferences ->
            mutablePreferences[phase.toPrefKey()] = Clock.System.now().toEpochMilliseconds()
        }.map { }

    private fun SpringPromoPhase.Active.toPrefKey() = when (this) {
        SpringPromoPhase.Active.Wave1 -> wave1FirstSeenPrefKey
        SpringPromoPhase.Active.Wave2 -> wave2FirstSeenPrefKey
    }
}
