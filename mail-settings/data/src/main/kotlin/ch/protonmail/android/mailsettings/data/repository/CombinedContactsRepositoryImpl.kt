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

package ch.protonmail.android.mailsettings.data.repository

import androidx.datastore.preferences.core.booleanPreferencesKey
import arrow.core.Either
import ch.protonmail.android.mailcommon.data.mapper.safeData
import ch.protonmail.android.mailcommon.data.mapper.safeEdit
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailsettings.data.MailSettingsDataStoreProvider
import ch.protonmail.android.mailsettings.domain.model.CombinedContactsPreference
import ch.protonmail.android.mailsettings.domain.repository.CombinedContactsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private const val DEFAULT_VALUE = false

class CombinedContactsRepositoryImpl @Inject constructor(
    private val dataStoreProvider: MailSettingsDataStoreProvider
) : CombinedContactsRepository {

    private val hasCombinedContactsKey = booleanPreferencesKey("hasCombinedContactsPrefKey")

    override fun observe(): Flow<Either<PreferencesError, CombinedContactsPreference>> =
        dataStoreProvider.combinedContactsDataStore.safeData.map { prefsEither ->
            prefsEither.map { prefs ->
                val hasCombinedContacts = prefs[hasCombinedContactsKey] ?: DEFAULT_VALUE
                CombinedContactsPreference(hasCombinedContacts)
            }
        }

    override suspend fun save(combinedContactsPreference: CombinedContactsPreference): Either<PreferencesError, Unit> =
        dataStoreProvider.combinedContactsDataStore.safeEdit { mutablePreferences ->
            mutablePreferences[hasCombinedContactsKey] = combinedContactsPreference.isEnabled
        }.void()
}
