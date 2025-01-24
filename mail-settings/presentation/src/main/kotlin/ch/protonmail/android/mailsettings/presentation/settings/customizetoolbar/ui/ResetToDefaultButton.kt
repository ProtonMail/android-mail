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

package ch.protonmail.android.mailsettings.presentation.settings.customizetoolbar.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ch.protonmail.android.mailsettings.presentation.R
import me.proton.core.compose.component.ProtonButton
import me.proton.core.compose.component.protonOutlinedButtonColors
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme

@Composable
internal fun ResetToDefaultButton(onClick: () -> Unit) {
    ProtonButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ButtonDefaults.MinHeight)
            .padding(
                start = ProtonDimens.DefaultSpacing,
                top = ProtonDimens.SmallSpacing,
                bottom = ProtonDimens.DefaultSpacing,
                end = ProtonDimens.DefaultSpacing
            ),
        shape = ProtonTheme.shapes.medium,
        border = BorderStroke(
            ButtonDefaults.OutlinedBorderSize,
            ProtonTheme.colors.notificationError
        ),
        elevation = null,
        colors = ButtonDefaults.protonOutlinedButtonColors(
            contentColor = ProtonTheme.colors.notificationError
        ),
        contentPadding = ButtonDefaults.ContentPadding
    ) {
        androidx.compose.material.Text(text = stringResource(R.string.customize_toolbar_reset_to_default_btn))
    }
}
