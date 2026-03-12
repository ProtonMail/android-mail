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

package ch.protonmail.android.mailevents.data.local

import java.util.UUID
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.data.mapper.safeData
import ch.protonmail.android.mailcommon.data.mapper.safeEdit
import ch.protonmail.android.mailcommon.domain.model.DataError
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class MailEventsDataSourceImpl @Inject constructor(
    private val dataStoreProvider: EventsDataStoreProvider
) : MailEventsDataSource {

    private val asidPreferenceKey = stringPreferencesKey(EventsDataStoreProvider.ASID_KEY)
    private val installEventSentKey = booleanPreferencesKey(EventsDataStoreProvider.INSTALL_EVENT_SENT_KEY)
    private val firstMessageSentKey = booleanPreferencesKey(EventsDataStoreProvider.FIRST_MESSAGE_SENT_EVENT_KEY)
    private val signupEventSentKey = booleanPreferencesKey(EventsDataStoreProvider.SIGNUP_EVENT_SENT_KEY)
    private val lastAppOpenTimestampKey = longPreferencesKey(EventsDataStoreProvider.LAST_APP_OPEN_TIMESTAMP_KEY)

    override suspend fun getOrCreateAsid(): Either<DataError.Local, String> {
        val prefsEither = dataStoreProvider.eventsDataStore.safeData.first()

        return prefsEither.fold(
            ifLeft = { DataError.Local.Unknown.left() },
            ifRight = { prefs ->
                val existingAsid = prefs[asidPreferenceKey]
                if (existingAsid != null) {
                    existingAsid.right()
                } else {
                    val newAsid = UUID.randomUUID().toString()
                    dataStoreProvider.eventsDataStore.safeEdit { mutablePrefs ->
                        mutablePrefs[asidPreferenceKey] = newAsid
                    }.fold(
                        ifLeft = { DataError.Local.Unknown.left() },
                        ifRight = { newAsid.right() }
                    )
                }
            }
        )
    }

    override suspend fun hasInstallEventBeenSent(): Boolean = hasEventBeenSent(installEventSentKey)

    override suspend fun hasFirstMessageSentEventBeenSent(): Boolean = hasEventBeenSent(firstMessageSentKey)

    override suspend fun hasSignupEventBeenSent(): Boolean = hasEventBeenSent(signupEventSentKey)

    override suspend fun markInstallEventSent(): Either<DataError.Local, Unit> = markEventSent(installEventSentKey)

    override suspend fun markSentMessageEventSent(): Either<DataError.Local, Unit> = markEventSent(firstMessageSentKey)

    override suspend fun markSignupEventSent(): Either<DataError.Local, Unit> = markEventSent(signupEventSentKey)

    private suspend fun hasEventBeenSent(key: Preferences.Key<Boolean>): Boolean {
        val prefsEither = dataStoreProvider.eventsDataStore.safeData.first()
        return prefsEither.fold(
            ifLeft = { false },
            ifRight = { prefs -> prefs[key] ?: false }
        )
    }

    private suspend fun markEventSent(key: Preferences.Key<Boolean>): Either<DataError.Local, Unit> {
        return dataStoreProvider.eventsDataStore.safeEdit { mutablePrefs ->
            mutablePrefs[key] = true
        }.fold(
            ifLeft = { DataError.Local.Unknown.left() },
            ifRight = { Unit.right() }
        )
    }

    override suspend fun getLastAppOpenTimestamp(): Long? {
        val prefsEither = dataStoreProvider.eventsDataStore.safeData.first()
        return prefsEither.fold(
            ifLeft = { null },
            ifRight = { prefs -> prefs[lastAppOpenTimestampKey] }
        )
    }

    override suspend fun saveLastAppOpenTimestamp(timestampMs: Long): Either<DataError.Local, Unit> {
        return dataStoreProvider.eventsDataStore.safeEdit { mutablePrefs ->
            mutablePrefs[lastAppOpenTimestampKey] = timestampMs
        }.fold(
            ifLeft = { DataError.Local.Unknown.left() },
            ifRight = { Unit.right() }
        )
    }
}
