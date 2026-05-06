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

package ch.protonmail.android.mailfeatureflags.domain

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeatureFlagResolver @Inject constructor(
    private val providers: Set<@JvmSuppressWildcards FeatureFlagValueProvider>
) {

    private val lastLogged = ConcurrentHashMap<String, Pair<Boolean, String>>()

    /**
     * Gets the value of a feature flag by its key, taking provider priorities into account.
     */
    @Suppress("TooGenericExceptionCaught")
    suspend fun getFeatureFlag(key: String, defaultValue: Boolean): Boolean {
        val match = providers
            .filter { it.isEnabled() }
            .sortedByDescending { it.priority }
            .firstNotNullOfOrNull { provider ->
                val value = try {
                    provider.getFeatureFlagValue(key)
                } catch (c: CancellationException) {
                    throw c
                } catch (t: Throwable) {
                    Timber.w(t, "FF provider '${provider.name}' failed for '$key'")
                    null
                }
                value?.let { provider.name to it }
            }

        val resolved = match?.second ?: defaultValue
        val source = match?.first ?: "default"
        val current = resolved to source
        if (lastLogged.put(key, current) != current) {
            Timber.d("Resolved FF '$key' = $resolved (via $source)")
        }
        return resolved
    }
}
