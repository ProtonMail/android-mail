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

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import ch.protonmail.android.design.compose.theme.ProtonTheme

internal object SpotlightColors {

    /** The card outline: iris-150, against Border/Strong on dark. */
    val cardBorder: Color
        @Composable get() = if (ProtonTheme.colors.isDark) DarkBorderStrong else LightIris150

    /** The square behind a feature icon or a step number: iris-100, backgroundDeep on dark. */
    val badgeBackground: Color
        @Composable get() = if (ProtonTheme.colors.isDark) ProtonTheme.colors.backgroundDeep else LightIris100

    /** The "What's new" pill, which swaps its two iris values around between themes. */
    val pillBackground: Color
        @Composable get() = if (ProtonTheme.colors.isDark) DarkIris800 else LightIris150

    val pillText: Color
        @Composable get() = if (ProtonTheme.colors.isDark) DarkIris100 else LightIris800

    private val LightIris100 = Color(0xFFE9EAFC)
    private val LightIris150 = Color(0xFFDDDEFB)
    private val LightIris800 = Color(0xFF311E71)

    private val DarkIris100 = Color(0xFFEDEFFD)
    private val DarkIris800 = Color(0xFF370F83)
    private val DarkBorderStrong = Color(0xFF50505B)
}
