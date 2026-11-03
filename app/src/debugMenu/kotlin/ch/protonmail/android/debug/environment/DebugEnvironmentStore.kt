/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.debug.environment

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import ch.protonmail.android.BuildConfig
import ch.protonmail.android.api.ApiHostOverride
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.debugEnvironmentDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "debugEnvironmentDataStore"
)

/**
 * The proton.black environment chosen in the debug menu, and the last ones used.
 *
 * It is read once, when the API client is built at app start, so a change applies at the next start and the values
 * below are those of this process.
 * It lives in the debug menu, so dev and alpha builds honour it and prod builds, which have no debug menu, never do.
 */
@Singleton
internal class DebugEnvironmentStore(
    private val dataStore: DataStore<Preferences>
) : ApiHostOverride {

    @Inject
    constructor(@ApplicationContext context: Context) : this(dataStore = context.debugEnvironmentDataStore)

    // Blocking, as the API client needs the host before anything else runs.
    private val stored: Preferences by lazy { runBlocking { dataStore.data.first() } }

    override fun host(): String? = stored[KEY_HOST]

    /** The environment the app uses: the override, or the build's default. */
    fun effectiveHost(): String = host() ?: defaultHost()

    /** The build's own environment, used without an override. */
    fun defaultHost(): String = BuildConfig.API_HOST

    /** Most recent first. */
    fun recentHosts(): List<String> = stored[KEY_RECENT].toHostList()

    /** Returns the recently used environments, most recent first, now including [host]. */
    suspend fun save(host: String): List<String> {
        val preferences = dataStore.edit { preferences ->
            val recent = listOf(host) + preferences[KEY_RECENT].toHostList().filterNot { it == host }
            preferences[KEY_HOST] = host
            preferences[KEY_RECENT] = recent.take(MAX_RECENT).joinToString(RECENT_SEPARATOR)
        }
        return preferences[KEY_RECENT].toHostList()
    }

    suspend fun clear() {
        dataStore.edit { it.remove(KEY_HOST) }
    }

    private fun String?.toHostList(): List<String> =
        this?.split(RECENT_SEPARATOR)?.filter { it.isNotEmpty() }.orEmpty()

    private companion object {

        const val RECENT_SEPARATOR = ","
        const val MAX_RECENT = 5
        val KEY_HOST = stringPreferencesKey("host")
        val KEY_RECENT = stringPreferencesKey("recent_hosts")
    }
}
