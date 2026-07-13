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

package ch.protonmail.android.mailsettings.domain.model

import ch.protonmail.android.mailpinlock.model.AutoLockInterval
import ch.protonmail.android.mailpinlock.model.Protection

data class AppSettings(
    val autolockInterval: AutoLockInterval,
    val autolockProtection: Protection,
    val hasAlternativeRouting: Boolean,
    val customAppLanguage: String?,
    val hasCombinedContactsEnabled: Boolean,
    val theme: Theme,
    val mobileSignaturePreference: MobileSignaturePreference,
    val swipeNextPreference: SwipeNextPreference,
    val useMobileDataForContentSearchIndexing: AllowMobileDataForContentSearchIndexing
) {
    val hasAutoLock = autolockProtection != Protection.None

    companion object {

        private val DEFAULT_THEME = Theme.SYSTEM_DEFAULT
        private val DEFAULT_CUSTOM_LANGUAGE: String? = null
        private const val DEFAULT_HAS_ALTERNATIVE_ROUTING = true
        private const val DEFAULT_HAS_DEVICE_CONTACTS_ENABLED = false

        fun default() = AppSettings(
            AutoLockInterval.default(),
            Protection.default(),
            DEFAULT_HAS_ALTERNATIVE_ROUTING,
            DEFAULT_CUSTOM_LANGUAGE,
            DEFAULT_HAS_DEVICE_CONTACTS_ENABLED,
            DEFAULT_THEME,
            MobileSignaturePreference.Empty,
            SwipeNextPreference.NotEnabled,
            AllowMobileDataForContentSearchIndexing.Enabled
        )
    }
}
