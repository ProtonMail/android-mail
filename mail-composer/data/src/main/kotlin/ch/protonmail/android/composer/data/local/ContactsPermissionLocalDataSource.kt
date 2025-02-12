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

package ch.protonmail.android.composer.data.local

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface ContactsPermissionLocalDataSource {

    fun observePermissionDenied(): Flow<Either<DataError, Boolean>>
    suspend fun trackPermissionDeniedEvent()
}

class ContactsPermissionLocalDataSourceImpl @Inject constructor(
    private val dataStoreProvider: ContactsPermissionDataStoreProvider
) : ContactsPermissionLocalDataSource {

    override fun observePermissionDenied() = dataStoreProvider.contactsPermissionsStore.data.map { prefs ->
        prefs[booleanPreferencesKey(SHOULD_STOP_SHOWING_PERMISSION_DIALOG)]?.right()
            ?: DataError.Local.NoDataCached.left()
    }

    override suspend fun trackPermissionDeniedEvent() {
        dataStoreProvider.contactsPermissionsStore.edit { prefs ->
            prefs[booleanPreferencesKey(SHOULD_STOP_SHOWING_PERMISSION_DIALOG)] = true
        }
    }
}

internal const val SHOULD_STOP_SHOWING_PERMISSION_DIALOG = "HasDeniedContactsPermission"
