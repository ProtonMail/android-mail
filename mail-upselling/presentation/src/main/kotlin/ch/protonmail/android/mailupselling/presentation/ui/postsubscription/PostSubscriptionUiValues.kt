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

package ch.protonmail.android.mailupselling.presentation.ui.postsubscription

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object PostSubscriptionColors {
    val BackgroundGradientColorStops = arrayOf(0.0f to Color(0xFF0C0233), 1.0f to Color(0xFF522580))
    val CloseButtonColor = Color.White
    val CloseButtonBackground = Color(0x14FFFFFF)
    val HorizontalDividerColor = Color(0x3DFFFFFF)
    val BottomSectionBackgroundColor = Color(0x14FFFFFF)
    val BottomSectionButtonTextColor = Color(0xFF0C0C14)
    val OtherPageIndicatorColor = Color(0x33FFFFFF)
    // Welcome page
    val ContentTextColor = Color(0xFFEDC1FF)
    val EntitlementTextColor = Color(0xCCFFFFFF)
    val FadedContentColor = Color(0x1FFFFFFF)
    // Discover all apps page
    val CardBackground = Color(0x00FFFFFF)
    val CardDividerColor = Color(0x00FFFFFF)
    val AppItemBackground = Color(0x0FFFFFFF)
    val AppMessageTextColor = Color(0xCCFFFFFF)
    val InstallButtonBackgroundColor = Color(0x1AFFFFFF)
    val InstallButtonContentColor = Color(0x1AFFFFFF)
    val InstallButtonDisabledBackgroundColor = Color(0x14FFFFFF)
    val InstallButtonDisabledContentColor = Color(0x14FFFFFF)
    val InstallButtonDisabledTextColor = Color(0x80FFFFFF)
}

object PostSubscriptionDimens {
    val CloseButtonSize = 32.dp
    val WelcomePageVerticalSpacing = 136.dp
    val WelcomePageIllustrationBigWidth = 218.dp
    val WelcomePageIllustrationSmallWidth = 109.dp
    val WelcomePageIllustrationBigHeight = 162.dp
    val WelcomePageIllustrationSmallHeight = 81.dp
}
