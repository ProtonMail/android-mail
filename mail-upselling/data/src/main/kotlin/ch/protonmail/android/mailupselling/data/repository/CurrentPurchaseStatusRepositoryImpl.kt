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

import androidx.datastore.preferences.core.stringPreferencesKey
import arrow.core.Either
import arrow.core.right
import ch.protonmail.android.mailcommon.data.mapper.safeData
import ch.protonmail.android.mailcommon.data.mapper.safeEdit
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailupselling.data.UpsellingDataStoreProvider
import ch.protonmail.android.mailupselling.domain.model.CurrentPurchaseStatus
import ch.protonmail.android.mailupselling.domain.model.PurchaseStatusUpdate
import ch.protonmail.android.mailupselling.domain.repository.CurrentPurchaseStatusRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CurrentPurchaseStatusRepositoryImpl @Inject constructor(
    private val dataStoreProvider: UpsellingDataStoreProvider
) : CurrentPurchaseStatusRepository {

    private val prefKey = stringPreferencesKey("currentPurchaseStatus")

    override fun observe(): Flow<Either<PreferencesError, CurrentPurchaseStatus>> {
        return dataStoreProvider.upsellingDataStore.safeData.map { preferences ->
            preferences.map { prefs ->
                prefs[prefKey]?.let { CurrentPurchaseStatus.fromString(it) } ?: CurrentPurchaseStatus.Empty
            }
        }
    }

    override suspend fun onNewUpdate(value: PurchaseStatusUpdate): Either<PreferencesError, Unit> {
        return dataStoreProvider.upsellingDataStore.safeEdit { preferences ->
            val currentStatus = preferences[prefKey]?.let {
                CurrentPurchaseStatus.fromString(it)
            }?.reduce(value) ?: CurrentPurchaseStatus.initial(value)
            preferences[prefKey] = currentStatus.serializeToString()
        }.map { Unit.right() }
    }

    override suspend fun reset(): Either<PreferencesError, Unit> {
        return dataStoreProvider.upsellingDataStore.safeEdit { preferences ->
            preferences[prefKey] = CurrentPurchaseStatus.Empty.serializeToString()
        }.map { Unit.right() }
    }
}
