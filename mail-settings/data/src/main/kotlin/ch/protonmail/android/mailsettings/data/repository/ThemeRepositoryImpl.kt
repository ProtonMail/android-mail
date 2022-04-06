/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailsettings.data.repository

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import ch.protonmail.android.mailsettings.data.MailSettingsDataStoreProvider
import ch.protonmail.android.mailsettings.domain.model.Theme
import ch.protonmail.android.mailsettings.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

class ThemeRepositoryImpl @Inject constructor(
    private val dataStoreProvider: MailSettingsDataStoreProvider
) : ThemeRepository {

    private val themePreferenceKey = stringPreferencesKey("themeEnumNamePrefKey")

    override fun observe(): Flow<Theme> =
        dataStoreProvider.themeDataStore.data.map { pref ->
            val themePreference = pref[themePreferenceKey] ?: Theme.SYSTEM_DEFAULT.name
            try {
                Theme.valueOf(themePreference)
            } catch (exception: IllegalArgumentException) {
                Timber.e(
                    exception,
                    "Saved theme pref. could not be resolved as a Theme enum constant"
                )
                clearThemePreference()
                Theme.SYSTEM_DEFAULT
            }
        }

    override suspend fun update(theme: Theme) {
        dataStoreProvider.themeDataStore.edit { mutablePrefs ->
            mutablePrefs[themePreferenceKey] = theme.name
        }
    }

    private suspend fun clearThemePreference() {
        dataStoreProvider.themeDataStore.edit { mutablePrefs ->
            mutablePrefs.clear()
        }
    }
}
