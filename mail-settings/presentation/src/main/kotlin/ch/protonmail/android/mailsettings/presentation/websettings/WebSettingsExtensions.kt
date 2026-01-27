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

package ch.protonmail.android.mailsettings.presentation.websettings

import ch.protonmail.android.mailsettings.domain.model.Theme
import ch.protonmail.android.mailsettings.domain.model.WebSettingsConfig
import ch.protonmail.android.mailsettings.presentation.BuildConfig
import timber.log.Timber

fun WebSettingsConfig.toPrivacyAndSecuritySettingsUrl(selector: String, theme: Theme): String =
    toSettingsUrl(selector, theme, privacySecuritySettingsAction)

fun WebSettingsConfig.toSpamFilterSettingsUrl(selector: String, theme: Theme): String =
    toSettingsUrl(selector, theme, spamFilterSettingsAction)

fun WebSettingsConfig.toFolderAndLabelSettingsUrl(selector: String, theme: Theme): String =
    toSettingsUrl(selector, theme, labelSettingsAction)

fun WebSettingsConfig.toEmailSettingsUrl(selector: String, theme: Theme): String =
    toSettingsUrl(selector, theme, emailSettingsAction)

fun WebSettingsConfig.toAccountSettingsUrl(selector: String, theme: Theme): String =
    toSettingsUrl(selector, theme, accountSettingsAction).also {
        Timber.d("web-settings: Account settings URL: $it")
    }

fun WebSettingsConfig.toEmailSignatureUrl(selector: String, theme: Theme): String =
    toSettingsUrl(selector, theme, emailSignatureAction).also {
        Timber.d("web-settings: Email signature settings URL: $it")
    }

private fun Theme.getUriParam(): String = when (this) {
    Theme.SYSTEM_DEFAULT -> SYSTEM_DEFAULT_THEME_PLACEHOLDER
    Theme.LIGHT -> LIGHT_THEME_URI_PARAM
    Theme.DARK -> DARK_THEME_URI_PARAM
}

@Suppress("MaxLineLength")
private fun WebSettingsConfig.toSettingsUrl(
    selector: String,
    theme: Theme,
    action: String
) =
    "$baseUrl?action=$action&theme=${theme.getUriParam()}&app-version=${BuildConfig.WEBVIEW_APP_VERSION}#selector=$selector"

const val SYSTEM_DEFAULT_THEME_PLACEHOLDER = "{SYSTEM_DEFAULT_THEME}"
const val LIGHT_THEME_URI_PARAM = "0"
const val DARK_THEME_URI_PARAM = "1"

