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

package ch.protonmail.android.mailsettings.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import javax.inject.Inject

class MailSettingsDataStoreProvider @Inject constructor(
    context: Context
) {

    private val Context.autoLockDataStore: DataStore<Preferences> by preferencesDataStore(
        name = "autoLockPrefDataStore"
    )
    private val Context.alternativeRoutingDataStore: DataStore<Preferences> by preferencesDataStore(
        name = "alternativeRoutingPrefDataStore"
    )
    private val Context.combinedContactsDataStore: DataStore<Preferences> by preferencesDataStore(
        name = "hasCombinedContactsPrefDataStore"
    )
    private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(
        name = "themePrefDataStore"
    )
    private val Context.preventScreenshotsDataStore: DataStore<Preferences> by preferencesDataStore(
        name = "preventScreenshotsPrefDataStore"
    )
    private val Context.backgroundSyncDataStore: DataStore<Preferences> by preferencesDataStore(
        name = "backgroundSyncPrefDataStore"
    )
    private val Context.addressDisplayInfoDataStore: DataStore<Preferences> by preferencesDataStore(
        name = "addressDisplayInfoPrefDataStore"
    )
    private val Context.mobileFooterDataStore: DataStore<Preferences> by preferencesDataStore(
        name = "mobileFooterPrefDataStore"
    )
    private val Context.notificationsDataStore: DataStore<Preferences> by preferencesDataStore(
        name = "notificationsPrefDataStore"
    )
    private val Context.spotlightDataStore: DataStore<Preferences> by preferencesDataStore(
        name = "spotlightDataStore"
    )
    val autoLockDataStore = context.autoLockDataStore
    val alternativeRoutingDataStore = context.alternativeRoutingDataStore
    val combinedContactsDataStore = context.combinedContactsDataStore
    val themeDataStore = context.themeDataStore
    val preventScreenshotsDataStore = context.preventScreenshotsDataStore
    val backgroundSyncDataStore = context.backgroundSyncDataStore
    val notificationsDataStore = context.notificationsDataStore
    val addressDisplayInfoDataStore = context.addressDisplayInfoDataStore
    val mobileFooterDataStore = context.mobileFooterDataStore
    val spotlightDataStore = context.spotlightDataStore
}
