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

package ch.protonmail.android.mailsettings.presentation.settings.autolock.ui.pin

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.ConfirmButtonUiModel
import me.proton.core.compose.component.ProtonButton
import me.proton.core.compose.component.protonButtonColors
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.ProtonTypography
import me.proton.core.compose.theme.defaultNorm

@Composable
fun VirtualKeyboardDigitItem(
    value: Int,
    onElementClicked: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .clickable(interactionSource = interactionSource, indication = null) { onElementClicked(value) }
            .wrapContentSize()
            .size(MailDimens.AutoLockPinScreen.KeyboardButtonBoxSize)
            .padding(ProtonDimens.SmallSpacing)
            .background(color = ProtonTheme.colors.shade10, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value.toString(),
            fontSize = MailDimens.AutoLockPinScreen.DigitTextSize,
            style = ProtonTypography.Default.defaultNorm,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun VirtualKeyboardButtonItem(
    @DrawableRes drawableRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        modifier = modifier
            .size(MailDimens.AutoLockPinScreen.KeyboardButtonBoxSize)
            .padding(ProtonDimens.SmallSpacing),
        onClick = onClick
    ) {
        Icon(
            painter = painterResource(drawableRes),
            contentDescription = contentDescription
        )
    }
}

@Composable
fun VirtualKeyboardConfirmButton(
    confirmButtonUiModel: ConfirmButtonUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ProtonButton(
        modifier = modifier
            .fillMaxWidth()
            .padding(ProtonDimens.DefaultSpacing),
        elevation = null,
        border = null,
        shape = ProtonTheme.shapes.medium,
        colors = ButtonDefaults.protonButtonColors(),
        enabled = confirmButtonUiModel.isEnabled,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.height(MailDimens.AutoLockPinScreen.BottomButtonSize),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(id = confirmButtonUiModel.textRes))
        }
    }
}
