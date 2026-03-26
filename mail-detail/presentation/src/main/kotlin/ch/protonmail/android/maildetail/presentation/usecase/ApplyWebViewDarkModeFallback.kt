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

package ch.protonmail.android.maildetail.presentation.usecase

import ch.protonmail.android.mailmessage.domain.model.MessageBodyTransformations
import ch.protonmail.android.mailmessage.domain.model.MessageTheme
import ch.protonmail.android.mailmessage.domain.model.MessageThemeOptions
import ch.protonmail.android.mailmessage.domain.usecase.IsWebViewMediaQuerySupported
import javax.inject.Inject

class ApplyWebViewDarkModeFallback @Inject constructor(
    private val isWebViewMediaQuerySupported: IsWebViewMediaQuerySupported,
    private val isDarkModeEnabled: IsDarkModeEnabled
) {

    @Suppress("ReturnCount")
    operator fun invoke(transformations: MessageBodyTransformations): MessageBodyTransformations {

        val themeOptions = transformations.messageThemeOptions

        // Do nothing if the theme is explicitly overridden to Light
        if (themeOptions?.themeOverride == MessageTheme.Light) return transformations

        // Do nothing if dark mode is not enabled
        if (!isDarkModeEnabled()) return transformations

        // Do nothing if the WebView already supports media queries
        if (isWebViewMediaQuerySupported()) return transformations

        return when {
            themeOptions != null -> {
                transformations.copy(
                    messageThemeOptions = themeOptions.copy(
                        supportsDarkModeViaMediaQuery = false
                    )
                )
            }

            else -> {
                transformations.copy(
                    messageThemeOptions = MessageThemeOptions(
                        currentTheme = MessageTheme.Dark,
                        themeOverride = null,
                        supportsDarkModeViaMediaQuery = false
                    )
                )
            }
        }
    }
}
