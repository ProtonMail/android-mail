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

package ch.protonmail.android.mailsettings.data.repository.local

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.data.mapper.safeData
import ch.protonmail.android.mailcommon.data.mapper.safeEdit
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailsettings.data.MailSettingsDataStoreProvider
import ch.protonmail.android.mailsettings.domain.model.MobileFooterPreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class MobileFooterLocalDataSourceImpl @Inject constructor(
    private val dataStoreProvider: MailSettingsDataStoreProvider
) : MobileFooterLocalDataSource {

    override fun observeMobileFooterPreference(userId: UserId): Flow<Either<PreferencesError, MobileFooterPreference>> =
        dataStoreProvider.mobileFooterDataStore.safeData.map outerMap@{ preferences ->
            preferences.map innerMap@{
                val isEnabled = it[getMobileFooterEnabledPrefKey(userId)]
                val footerValue = it[getMobileFooterValuePrefKey(userId)]

                return@outerMap if (isEnabled != null && footerValue != null) {
                    MobileFooterPreference(footerValue, isEnabled).right()
                } else {
                    PreferencesError.left()
                }
            }
        }

    override suspend fun updateMobileFooter(
        userId: UserId,
        preference: MobileFooterPreference
    ): Either<PreferencesError, Unit> {
        return dataStoreProvider.mobileFooterDataStore.safeEdit { preferences ->
            preferences[getMobileFooterEnabledPrefKey(userId)] = preference.enabled
            preferences[getMobileFooterValuePrefKey(userId)] = preference.value
        }.map { Unit.right() }
    }

    private fun getMobileFooterEnabledPrefKey(userId: UserId) = booleanPreferencesKey(
        "${userId.id}-mobileFooterEnabledPrefKey"
    )

    private fun getMobileFooterValuePrefKey(userId: UserId) = stringPreferencesKey(
        "${userId.id}-mobileFooterValuePrefKey"
    )
}
