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

import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ch.protonmail.android.mailsettings.presentation.R
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.AutoLockPinState
import me.proton.core.compose.component.appbar.ProtonTopAppBar

@Composable
fun AutoLockPinInsertionTopBar(
    state: AutoLockPinState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiModel = (state as? AutoLockPinState.DataLoaded)?.topBarState?.topBarStateUiModel ?: return

    val navigationIcon: (@Composable () -> Unit)? = if (uiModel.showBackButton) {
        {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = stringResource(id = R.string.mail_settings_toolbar_button_content_description)
                )
            }
        }
    } else null

    ProtonTopAppBar(
        modifier = modifier,
        title = { Text(text = stringResource(id = uiModel.textRes)) },
        navigationIcon = navigationIcon
    )
}

