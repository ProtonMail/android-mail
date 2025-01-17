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

package ch.protonmail.android.mailnotifications.data.local

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class NotificationPermissionLocalDataSourceImpl @Inject constructor(
    private val dataStoreProvider: DataStoreProvider
) : NotificationPermissionLocalDataSource {

    override suspend fun getNotificationPermissionTimestamp(): Either<DataError.Local, Long> {
        val notificationPermissionTimestamp = dataStoreProvider.notificationPermissionStore.data.map { prefs ->
            prefs[longPreferencesKey(NOTIFICATION_PERMISSION_TIMESTAMP_KEY)]
        }.firstOrNull()

        return notificationPermissionTimestamp?.right() ?: DataError.Local.NoDataCached.left()
    }

    override suspend fun saveNotificationPermissionTimestamp(timestamp: Long) {
        dataStoreProvider.notificationPermissionStore.edit { prefs ->
            prefs[longPreferencesKey(NOTIFICATION_PERMISSION_TIMESTAMP_KEY)] = timestamp
        }
    }

    override suspend fun getShouldStopShowingPermissionDialog(): Either<DataError.Local, Boolean> {
        val shouldStopShowingPermissionDialog = dataStoreProvider.notificationPermissionStore.data.map { prefs ->
            prefs[booleanPreferencesKey(SHOULD_STOP_SHOWING_PERMISSION_DIALOG)]
        }.firstOrNull()

        return shouldStopShowingPermissionDialog?.right() ?: DataError.Local.NoDataCached.left()
    }

    override suspend fun saveShouldStopShowingPermissionDialog(shouldStopShowingPermissionDialog: Boolean) {
        dataStoreProvider.notificationPermissionStore.edit { prefs ->
            prefs[booleanPreferencesKey(SHOULD_STOP_SHOWING_PERMISSION_DIALOG)] = shouldStopShowingPermissionDialog
        }
    }
}

internal const val NOTIFICATION_PERMISSION_TIMESTAMP_KEY = "NotificationPermissionTimestampKey"
internal const val SHOULD_STOP_SHOWING_PERMISSION_DIALOG = "ShouldStopShowingPermissionDialog"
