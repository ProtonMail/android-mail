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

package ch.protonmail.android.mailcategory.data.local

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import arrow.core.Either
import ch.protonmail.android.mailcategory.domain.model.CategorySpotlightType
import ch.protonmail.android.mailcommon.data.mapper.safeData
import ch.protonmail.android.mailcommon.data.mapper.safeEdit
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CategorySpotlightLocalDataSourceImpl @Inject constructor(
    private val dataStoreProvider: CategorySpotlightDataStoreProvider
) : CategorySpotlightLocalDataSource {

    override fun observeHasBeenSeen(type: CategorySpotlightType): Flow<Either<PreferencesError, Boolean>> {
        val seenPrefKey = seenPrefKeyFor(type)
        return dataStoreProvider.categorySpotlightDataStore.safeData.map { prefsEither ->
            prefsEither.map { prefs -> prefs[seenPrefKey] ?: DEFAULT_VALUE }
        }
    }

    override suspend fun markSeen(type: CategorySpotlightType): Either<PreferencesError, Unit> =
        dataStoreProvider.categorySpotlightDataStore.safeEdit { mutablePreferences ->
            mutablePreferences[seenPrefKeyFor(type)] = true
        }.map { }

    private fun seenPrefKeyFor(type: CategorySpotlightType): Preferences.Key<Boolean> = when (type) {
        CategorySpotlightType.UnseenCategory ->
            booleanPreferencesKey(CategorySpotlightDataStoreProvider.CATEGORY_UNSEEN_SPOTLIGHT_SEEN_KEY)

        CategorySpotlightType.Personalise ->
            booleanPreferencesKey(CategorySpotlightDataStoreProvider.CATEGORY_PERSONALISE_SPOTLIGHT_SEEN_KEY)
    }
}

private const val DEFAULT_VALUE = false
