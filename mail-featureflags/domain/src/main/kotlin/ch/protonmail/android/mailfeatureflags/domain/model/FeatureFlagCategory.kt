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

package ch.protonmail.android.mailfeatureflags.domain.model

sealed class FeatureFlagCategory(val name: String) {
    data object Global : FeatureFlagCategory("Global")
    data object Composer : FeatureFlagCategory("Composer")
    data object Mailbox : FeatureFlagCategory("Mailbox")
    data object Details : FeatureFlagCategory("Details")
    data object Settings : FeatureFlagCategory("Settings")
    data object Upselling : FeatureFlagCategory("Upselling")
    data object Rating : FeatureFlagCategory("Rating")
    data object Notifications : FeatureFlagCategory("Notifications")

    // Unused, only available in tests.
    data object Test : FeatureFlagCategory("Test")
}
