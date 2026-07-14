/*
 * Copyright (c) 2025 Proton Technologies AG
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

/**
 * Manages device-local, developer-only feature-flag overrides (debug menu only).
 *
 * Overrides are applied by the underlying provider itself, so reading a flag already reflects
 * any override; this manager only sets, clears and lists them.
 */
interface FeatureFlagOverrideManager {

    /**
     * Currently overridden flags keyed by flag name, with the overridden boolean value
     * (`null` when only a variant is forced).
     */
    suspend fun overriddenDebugFlags(): Map<String, Boolean?>

    suspend fun setDebugOverride(key: String, enabled: Boolean)

    suspend fun clearAllDebugOverrides()
}
