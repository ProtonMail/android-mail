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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens.AutoLockPinScreen.PinDotsGridHeight
import ch.protonmail.android.mailsettings.presentation.R
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme

@Composable
fun AutoLockPinLockIcon(modifier: Modifier = Modifier) {
    Icon(
        modifier = modifier.wrapContentSize(),
        painter = painterResource(R.drawable.ic_proton_lock),
        contentDescription = stringResource(id = R.string.mail_settings_pin_insertion_lock_icon_description)
    )
}

@Composable
fun AutoLockPinDotItem(modifier: Modifier = Modifier) {
    Icon(
        modifier = modifier
            .wrapContentSize()
            .padding(ProtonDimens.ExtraSmallSpacing),
        painter = painterResource(id = R.drawable.ic_proton_circle_filled),
        tint = ProtonTheme.colors.iconAccent,
        contentDescription = stringResource(id = R.string.mail_settings_pin_insertion_pin_dot_description)
    )
}

@Composable
fun AutoLockPinDotsGrid(insertedPinSize: Int, modifier: Modifier = Modifier) {
    LazyHorizontalGrid(
        modifier = modifier
            .height(PinDotsGridHeight)
            .wrapContentSize(),
        rows = GridCells.Fixed(Constants.PinRowMaxLines),
        horizontalArrangement = Arrangement.Center
    ) {
        items(insertedPinSize) {
            AutoLockPinDotItem()
        }
    }
}

@Composable
fun AutoLockPinKeyboardGrid(
    showBiometricPin: Boolean,
    actions: AutoLockPinKeyboardGrid.Actions,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        contentPadding = PaddingValues(ProtonDimens.DefaultSpacing),
        modifier = modifier.wrapContentSize(),
        columns = GridCells.Fixed(Constants.KeyboardRowSize),
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.Center
    ) {
        items(Constants.KeyboardMainElementsSize) { element ->
            VirtualKeyboardDigitItem(
                value = element + 1, // Index starts at 0
                onElementClicked = actions.onDigitAdded
            )
        }


        item {
            if (showBiometricPin) {
                VirtualKeyboardButtonItem(
                    drawableRes = R.drawable.ic_proton_fingerprint,
                    onClick = actions.onBiometricPinClick,
                    contentDescription = stringResource(id = R.string.mail_settings_pin_insertion_biometric_description)
                )
            }
        }

        item {
            VirtualKeyboardDigitItem(
                value = 0,
                onElementClicked = actions.onDigitAdded
            )
        }
        item {
            VirtualKeyboardButtonItem(
                drawableRes = R.drawable.ic_proton_backspace,
                onClick = actions.onBackSpaceClick,
                contentDescription = stringResource(id = R.string.mail_settings_pin_insertion_backspace_description)
            )
        }
    }
}

object AutoLockPinKeyboardGrid {

    data class Actions(
        val onBiometricPinClick: () -> Unit,
        val onDigitAdded: (Int) -> Unit,
        val onBackSpaceClick: () -> Unit
    ) {
        companion object {
            val Empty = Actions(
                onBiometricPinClick = {},
                onDigitAdded = {},
                onBackSpaceClick = {}
            )
        }
    }
}

private object Constants {

    const val PinRowMaxLines = 1
    const val KeyboardRowSize = 3
    const val KeyboardMainElementsSize = 9 // Keyboard items are split in 9 + 3 elements
}
