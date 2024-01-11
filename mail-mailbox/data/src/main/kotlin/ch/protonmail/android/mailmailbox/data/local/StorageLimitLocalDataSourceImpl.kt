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

package ch.protonmail.android.mailmailbox.data.local

import androidx.datastore.preferences.core.booleanPreferencesKey
import arrow.core.Either
import arrow.core.raise.either
import ch.protonmail.android.mailcommon.data.mapper.safeData
import ch.protonmail.android.mailcommon.data.mapper.safeEdit
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailmailbox.data.MailMailboxDataStoreProvider
import ch.protonmail.android.mailmailbox.domain.model.StorageLimitPreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class StorageLimitLocalDataSourceImpl @Inject constructor(
    private val dataStoreProvider: MailMailboxDataStoreProvider
) : StorageLimitLocalDataSource {

    private val firstLimitWarningConfirmedPrefKey =
        booleanPreferencesKey(FIRST_LIMIT_WARNING_CONFIRMED_PREF_KEY)
    private val secondLimitWarningConfirmedPrefKey =
        booleanPreferencesKey(SECOND_LIMIT_WARNING_CONFIRMED_PREF_KEY)

    override fun observe(): Flow<Either<PreferencesError, StorageLimitPreference>> =
        dataStoreProvider.storageLimitPrefDataStore.safeData.map { prefsEither ->
            prefsEither.map { prefs ->
                val firstLimitWarningConfirmed = prefs[firstLimitWarningConfirmedPrefKey] ?: DEFAULT_VALUE
                val secondLimitWarningConfirmed = prefs[secondLimitWarningConfirmedPrefKey] ?: DEFAULT_VALUE

                StorageLimitPreference(firstLimitWarningConfirmed, secondLimitWarningConfirmed)
            }
        }

    override suspend fun saveFirstLimitWarningPreference(confirmed: Boolean): Either<PreferencesError, Unit> = either {
        dataStoreProvider.storageLimitPrefDataStore.safeEdit {
            it[firstLimitWarningConfirmedPrefKey] = confirmed
        }.bind()
    }

    override suspend fun saveSecondLimitWarningPreference(confirmed: Boolean): Either<PreferencesError, Unit> = either {
        dataStoreProvider.storageLimitPrefDataStore.safeEdit {
            it[secondLimitWarningConfirmedPrefKey] = confirmed
        }.bind()
    }

    companion object {
        const val DEFAULT_VALUE = false
        const val FIRST_LIMIT_WARNING_CONFIRMED_PREF_KEY = "firstLimitWarningConfirmedPrefKey"
        const val SECOND_LIMIT_WARNING_CONFIRMED_PREF_KEY = "secondLimitWarningConfirmedPrefKey"
    }
}
