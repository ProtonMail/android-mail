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

package ch.protonmail.android.mailspotlight.presentation.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf

/**
 * Set when a spotlight page is expected to show inside the feature spotlight carousel.
 *
 * A page that is shown on its own leaves this null. That is what tells the bottom bar to drop the
 * page indicator, and it is the only difference between the two presentations.
 */
internal val LocalSpotlightCarousel = compositionLocalOf<SpotlightCarousel?> { null }

@Stable
internal data class SpotlightCarousel(val currentPage: Int, val pageCount: Int)
