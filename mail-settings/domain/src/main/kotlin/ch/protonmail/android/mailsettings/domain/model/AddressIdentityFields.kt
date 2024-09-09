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

@JvmInline
value class DisplayName(val value: String)

@JvmInline
value class SignatureValue(val text: String)

data class Signature(val enabled: Boolean, val value: SignatureValue)

sealed class MobileFooter(val value: String, val enabled: Boolean, val editable: Boolean, val toggleable: Boolean) {

    class PaidUserMobileFooter(value: String, enabled: Boolean) : MobileFooter(
        value,
        enabled,
        editable = true,
        toggleable = true
    )

    class FreeUserMobileFooter(value: String) : MobileFooter(
        value,
        enabled = true,
        editable = false,
        toggleable = false
    )

    class FreeUserUpsellingMobileFooter(value: String) : MobileFooter(
        value,
        enabled = true,
        editable = false,
        toggleable = true
    )
}
