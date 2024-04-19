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

package ch.protonmail.android.mailcommon.presentation

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview

@Preview(device = Devices.PHONE, uiMode = Configuration.UI_MODE_NIGHT_NO, name = "1. Theme - Light mode")
@Preview(device = Devices.PHONE, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "2. Theme - Night mode")
@Preview(device = "spec:width=360dp,height=1080dp,orientation=portrait", name = "3. Size - Narrow")
@Preview(device = "spec:width=411dp,height=891dp,orientation=landscape", name = "4. Orientation Landscape")
@Preview(
    device = "spec:width=360dp,height=1080dp,orientation=portrait",
    name = "Size - Narrow - 200% font scale",
    fontScale = 2.0f
)
@Preview(device = Devices.FOLDABLE, name = "5. Foldable")
@Preview(device = Devices.TABLET, name = "6. Tablet")
annotation class AdaptivePreviews
