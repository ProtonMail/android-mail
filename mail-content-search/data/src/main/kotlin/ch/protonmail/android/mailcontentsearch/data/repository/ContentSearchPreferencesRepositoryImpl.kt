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

package ch.protonmail.android.mailcontentsearch.data.repository

import androidx.datastore.preferences.core.booleanPreferencesKey
import arrow.core.Either
import arrow.core.right
import ch.protonmail.android.mailcommon.data.mapper.safeData
import ch.protonmail.android.mailcommon.data.mapper.safeEdit
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailcontentsearch.data.local.ContentSearchDataStoreProvider
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ContentSearchPreferencesRepositoryImpl @Inject constructor(
    private val dataStoreProvider: ContentSearchDataStoreProvider
) : ContentSearchPreferencesRepository {

    private val allowMobileDataKey = booleanPreferencesKey("contentSearchAllowMobileDataPrefKey")

    override suspend fun getAllowMobileData(): Either<PreferencesError, Boolean> =
        dataStoreProvider.allowMobileDataDataStore.safeData.map { preferences ->
            preferences.map { it[allowMobileDataKey] ?: DefaultAllowMobileData }
        }.first()

    override suspend fun setAllowMobileData(value: Boolean): Either<PreferencesError, Unit> =
        dataStoreProvider.allowMobileDataDataStore.safeEdit { preferences ->
            preferences[allowMobileDataKey] = value
        }.map { Unit.right() }

    private companion object {

        const val DefaultAllowMobileData = true
    }
}
