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

package ch.protonmail.android.mailupselling.data.repository

import androidx.datastore.preferences.core.longPreferencesKey
import arrow.core.Either
import arrow.core.right
import ch.protonmail.android.mailcommon.data.mapper.safeData
import ch.protonmail.android.mailcommon.data.mapper.safeEdit
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailupselling.data.UpsellingDataStoreProvider
import ch.protonmail.android.mailupselling.domain.model.OneClickUpsellingLastSeenPreference
import ch.protonmail.android.mailupselling.domain.repository.UpsellingVisibilityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UpsellingVisibilityRepositoryImpl @Inject constructor(
    private val dataStoreProvider: UpsellingDataStoreProvider
) : UpsellingVisibilityRepository {

    private val oneClickLastSeenPreference = longPreferencesKey("oneClickLastSeenTimestamp")

    override fun observe(): Flow<Either<PreferencesError, OneClickUpsellingLastSeenPreference>> =
        dataStoreProvider.upsellingDataStore.safeData.map { preferences ->
            preferences.map {
                val value = it[oneClickLastSeenPreference] ?: DefaultValue
                OneClickUpsellingLastSeenPreference(value)
            }
        }

    override suspend fun update(value: Long): Either<PreferencesError, Unit> {
        return dataStoreProvider.upsellingDataStore.safeEdit { preferences ->
            preferences[oneClickLastSeenPreference] = value
        }.map { Unit.right() }
    }

    private companion object {

        const val DefaultValue = 0L
    }
}
