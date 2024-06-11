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

package ch.protonmail.android.mailcontact.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import ch.protonmail.android.mailcommon.data.mapper.safeData
import ch.protonmail.android.mailcommon.data.mapper.safeEdit
import ch.protonmail.android.mailcontact.domain.DeviceContactsSuggestionsPrompt
import ch.protonmail.android.mailcontact.domain.usecase.featureflags.IsDeviceContactsSuggestionsEnabled
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DeviceContactsSuggestionsPromptImpl @Inject constructor(
    private val isDeviceContactsSuggestionsEnabled: IsDeviceContactsSuggestionsEnabled,
    private val contactDataStoreProvider: ContactDataStoreProvider
) : DeviceContactsSuggestionsPrompt {

    private val isPromptEnabledPrefKey =
        booleanPreferencesKey(DEVICE_CONTACT_SUGGESTIONS_PROMPT_ENABLED_PREF_KEY)

    override suspend fun setPromptDisabled() {
        contactDataStoreProvider.contactDataStore.safeEdit {
            it[isPromptEnabledPrefKey] = false
        }
    }

    override suspend fun getPromptEnabled(): Boolean {
        val promptEnabledPreference = contactDataStoreProvider.contactDataStore.safeData.map { prefsEither ->
            prefsEither.map { prefs ->
                prefs[isPromptEnabledPrefKey] ?: DEFAULT_VALUE
            }
        }.firstOrNull()?.getOrNull() ?: DEFAULT_VALUE

        return isDeviceContactsSuggestionsEnabled() && promptEnabledPreference
    }

    companion object {

        const val DEFAULT_VALUE = true

        @Suppress("VariableMaxLength")
        const val DEVICE_CONTACT_SUGGESTIONS_PROMPT_ENABLED_PREF_KEY = "deviceContactSuggestionsPromptEnabledPrefKey"
    }

}
