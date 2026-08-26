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

package ch.protonmail.android.mailupselling.presentation.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import ch.protonmail.android.mailupselling.presentation.R

enum class UpsellContentTheme(
    @DrawableRes val headerDrawable: Int,
    @StringRes val titleRes: Int,
    @StringRes val rowTitleRes: Int,
    @StringRes val rowSubtitleRes: Int
) {
    Generic(
        R.drawable.illustration_pu_generic,
        R.string.upselling_pu_generic_title,
        R.string.upselling_pu_row_generic_title,
        R.string.upselling_pu_row_generic_subtitle
    ),
    ThreatProtection(
        R.drawable.illustration_pu_sentinel,
        R.string.upselling_pu_threat_protection_title,
        R.string.upselling_pu_row_threat_protection_title,
        R.string.upselling_pu_row_threat_protection_subtitle
    ),
    Aliases(
        R.drawable.illustration_pu_aliases,
        R.string.upselling_pu_aliases_title,
        R.string.upselling_pu_row_aliases_title,
        R.string.upselling_pu_row_aliases_subtitle
    ),
    Drive(
        R.drawable.illustration_pu_drive,
        R.string.upselling_pu_drive_title,
        R.string.upselling_pu_row_drive_title,
        R.string.upselling_pu_row_drive_subtitle
    ),
    VpnBrowse(
        R.drawable.illustration_pu_vpn,
        R.string.upselling_pu_vpn_browse_title,
        R.string.upselling_pu_row_vpn_browse_title,
        R.string.upselling_pu_row_vpn_browse_subtitle
    ),
    VpnStreaming(
        R.drawable.illustration_pu_streaming,
        R.string.upselling_pu_vpn_streaming_title,
        R.string.upselling_pu_row_vpn_streaming_title,
        R.string.upselling_pu_row_vpn_streaming_subtitle
    ),
    Pass(
        R.drawable.illustration_pu_pass,
        R.string.upselling_pu_pass_title,
        R.string.upselling_pu_row_pass_title,
        R.string.upselling_pu_row_pass_subtitle
    )
}
