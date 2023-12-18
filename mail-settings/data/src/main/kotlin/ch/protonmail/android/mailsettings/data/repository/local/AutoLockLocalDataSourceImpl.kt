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

import androidx.datastore.preferences.core.stringPreferencesKey
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import ch.protonmail.android.mailcommon.data.mapper.safeEdit
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailsettings.data.MailSettingsDataStoreProvider
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockEnabledEncryptedValue
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockEncryptedInterval
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockEncryptedPin
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockLastEncryptedForegroundMillis
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject

class AutoLockLocalDataSourceImpl @Inject constructor(
    private val dataStoreProvider: MailSettingsDataStoreProvider
) : AutoLockLocalDataSource {

    private val hasAutoLockKey = stringPreferencesKey("hasAutoLockPrefKey")
    private val autoLockIntervalKey = stringPreferencesKey("autoLockIntervalPrefKey")
    private val lastForegroundMillisKey = stringPreferencesKey("lastForegroundTimestampPrefKey")
    private val pinKey = stringPreferencesKey("pinCodePrefKey")

    override fun observeAutoLockEnabledEncryptedValue() = dataStoreProvider.autoLockDataStore.data.map {
        val encryptedValue = it[hasAutoLockKey] ?: return@map PreferencesError.left()
        AutoLockEnabledEncryptedValue(encryptedValue).right()
    }

    override fun observeAutoLockEncryptedInterval() = dataStoreProvider.autoLockDataStore.data.map {
        val encryptedValue = it[autoLockIntervalKey] ?: return@map PreferencesError.left()
        AutoLockEncryptedInterval(encryptedValue).right()
    }

    override fun observeLastEncryptedForegroundMillis() = dataStoreProvider.autoLockDataStore.data.mapLatest {
        val encryptedValue = it[lastForegroundMillisKey] ?: return@mapLatest PreferencesError.left()
        AutoLockLastEncryptedForegroundMillis(encryptedValue).right()
    }

    override fun observeAutoLockEncryptedPin() = dataStoreProvider.autoLockDataStore.data.map {
        val encryptedValue = it[pinKey] ?: return@map PreferencesError.left()
        AutoLockEncryptedPin(encryptedValue).right()
    }

    override suspend fun updateAutoLockEnabledEncryptedValue(value: AutoLockEnabledEncryptedValue) =
        either<PreferencesError, Unit> {
            dataStoreProvider.autoLockDataStore.safeEdit {
                it[hasAutoLockKey] = value.encryptedValue
            }.bind()
        }

    override suspend fun updateAutoLockEncryptedInterval(interval: AutoLockEncryptedInterval) =
        either<PreferencesError, Unit> {
            dataStoreProvider.autoLockDataStore.safeEdit {
                it[autoLockIntervalKey] = interval.encryptedValue
            }.bind()
        }

    override suspend fun updateLastEncryptedForegroundMillis(timestamp: AutoLockLastEncryptedForegroundMillis) =
        either<PreferencesError, Unit> {
            dataStoreProvider.autoLockDataStore.safeEdit {
                it[lastForegroundMillisKey] = timestamp.encryptedValue
            }.bind()
        }

    override suspend fun updateAutoLockEncryptedPin(pin: AutoLockEncryptedPin) =
        either<PreferencesError, Unit> {
            dataStoreProvider.autoLockDataStore.safeEdit {
                it[pinKey] = pin.encryptedValue
            }.bind()
        }
}
