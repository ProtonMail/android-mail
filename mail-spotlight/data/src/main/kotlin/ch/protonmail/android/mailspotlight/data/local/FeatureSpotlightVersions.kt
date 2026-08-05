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

package ch.protonmail.android.mailspotlight.data.local

@Suppress("Unused")
internal object FeatureSpotlightVersions {

    const val PRIVACY_BUNDLE = 1
    const val CATEGORY_VIEW_PRE_7110 = 2 // This was used in 7.10.x and pre-alpha of 7.11.0
    const val CATEGORY_VIEW = 3

    const val CONTENT_SEARCH = 4
}
