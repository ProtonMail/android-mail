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

package ch.protonmail.android.mailnotifications.data.local.fcm

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailnotifications.data.local.NotificationTokenPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class FcmTokenPreferencesImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NotificationTokenPreferences {

    private val Context.fcmTokenStore: DataStore<Preferences> by preferencesDataStore(name = STORE_NAME)

    override suspend fun storeToken(token: String) {
        context.fcmTokenStore.edit { preferences -> preferences[stringPreferencesKey(FCM_TOKEN_KEY)] = token }
    }

    override suspend fun getToken(): Either<DataError.Local, String> {
        val token = context.fcmTokenStore.data.map { preferences ->
            preferences[stringPreferencesKey(FCM_TOKEN_KEY)]
        }.firstOrNull()

        return token?.right() ?: DataError.Local.NoDataCached.left()
    }


    private companion object {

        const val STORE_NAME = "fcmTokenStore"
        const val FCM_TOKEN_KEY = "fcmTokenKey"
    }
}
