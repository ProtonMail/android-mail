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

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import javax.inject.Inject

class EventsDataStoreProvider @Inject constructor(
    context: Context
) {

    private val Context.eventsDataStore: DataStore<Preferences> by preferencesDataStore(
        name = "eventsPrefDataStore"
    )

    val eventsDataStore = context.eventsDataStore

    internal companion object {

        const val ASID_KEY = "asid"
        const val INSTALL_EVENT_SENT_KEY = "installEventSent"
        const val FIRST_MESSAGE_SENT_EVENT_KEY = "firstMessageSentEvent"
        const val SIGNUP_EVENT_SENT_KEY = "signupEventSent"
        const val LAST_APP_OPEN_TIMESTAMP_KEY = "lastAppOpenTimestamp"
    }
}
