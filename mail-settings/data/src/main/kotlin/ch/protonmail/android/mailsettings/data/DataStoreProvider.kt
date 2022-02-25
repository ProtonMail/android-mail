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

package ch.protonmail.android.mailsettings.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import javax.inject.Inject

class DataStoreProvider @Inject constructor(
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
    val autoLockDataStore = context.autoLockDataStore
    val alternativeRoutingDataStore = context.alternativeRoutingDataStore
    val combinedContactsDataStore = context.combinedContactsDataStore
}
