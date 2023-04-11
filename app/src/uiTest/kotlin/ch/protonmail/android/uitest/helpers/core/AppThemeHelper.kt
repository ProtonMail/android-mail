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

package ch.protonmail.android.uitest.helpers.core

import androidx.datastore.preferences.core.stringPreferencesKey
import ch.protonmail.android.mailcommon.data.mapper.safeEdit
import ch.protonmail.android.mailsettings.data.MailSettingsDataStoreProvider
import ch.protonmail.android.mailsettings.domain.model.Theme
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * A class helper to force a certain theme in the app.
 */
internal class AppThemeHelper @Inject constructor() {

    @Inject
    lateinit var dataStoreProvider: MailSettingsDataStoreProvider

    private val themePreferenceKey = stringPreferencesKey("themeEnumNamePrefKey")

    fun applyTheme(theme: Theme) = runBlocking {
        dataStoreProvider.themeDataStore.safeEdit {
            it[themePreferenceKey] = theme.name
        }
    }
}
