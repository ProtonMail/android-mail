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

package ch.protonmail.android.mailcommon.presentation.compose

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.R
import ch.protonmail.android.mailcommon.presentation.extension.tintColor
import me.proton.core.compose.theme.ProtonDimens

@Composable
fun SmallNonClickableIcon(
    @DrawableRes iconId: Int,
    modifier: Modifier = Modifier,
    tintId: Int = R.color.icon_weak
) {
    SmallNonClickableIcon(
        modifier = modifier,
        iconId = iconId,
        iconColor = colorResource(id = tintId)
    )
}

@Composable
fun SmallNonClickableIcon(
    @DrawableRes iconId: Int,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Icon(
        modifier = modifier
            .semantics { tintColor = iconColor }
            .size(ProtonDimens.SmallIconSize),
        painter = painterResource(id = iconId),
        contentDescription = NO_CONTENT_DESCRIPTION,
        tint = iconColor
    )
}
